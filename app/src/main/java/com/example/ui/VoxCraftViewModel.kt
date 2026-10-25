package com.example.ui

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.engine.AudioExporter
import com.example.data.engine.AudioPlayerManager
import com.example.data.engine.DetectionResult
import com.example.data.engine.LanguageDetector
import com.example.data.model.AudioFormatOption
import com.example.data.model.AvailableGeminiVoices
import com.example.data.model.GeminiVoice
import com.example.data.model.LanguageItem
import com.example.data.model.SpeechClip
import com.example.data.model.SupportedLanguages
import com.example.data.model.UserProfile
import com.example.data.model.VoiceStyle
import com.example.data.repository.TtsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StudioUiState(
    val inputText: String = "",
    val selectedLanguage: LanguageItem = SupportedLanguages.getByCode("bn"),
    val detectionResult: DetectionResult? = null,
    val selectedStyle: VoiceStyle = VoiceStyle.NARRATIVE,
    val selectedVoice: GeminiVoice = AvailableGeminiVoices.getDefaultVoice(),
    val selectedFormat: AudioFormatOption = AudioFormatOption.WAV,
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val volume: Float = 1.0f,
    val isOfflineMode: Boolean = false,
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
    val latestClip: SpeechClip? = null
)

class VoxCraftViewModel(application: Application) : AndroidViewModel(application) {

    val repository = TtsRepository(application, viewModelScope)
    val playerManager = AudioPlayerManager(application, viewModelScope)

    // Profiles
    val allProfiles: StateFlow<List<UserProfile>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProfile: StateFlow<UserProfile?> = repository.activeProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Studio UI State
    private val _studioState = MutableStateFlow(StudioUiState())
    val studioState: StateFlow<StudioUiState> = _studioState.asStateFlow()

    // Library Filtering & Search
    val searchQuery = MutableStateFlow("")
    val filterLanguage = MutableStateFlow<String?>(null)
    val filterStyle = MutableStateFlow<String?>(null)

    val filteredClips: StateFlow<List<SpeechClip>> = combine(
        repository.allClips,
        searchQuery,
        filterLanguage,
        filterStyle
    ) { clips, query, lang, style ->
        clips.filter { clip ->
            val matchesQuery = query.isEmpty() ||
                clip.title.contains(query, ignoreCase = true) ||
                clip.originalText.contains(query, ignoreCase = true)
            val matchesLang = lang == null || clip.languageCode.equals(lang, ignoreCase = true)
            val matchesStyle = style == null || clip.voiceStyle.equals(style, ignoreCase = true)
            matchesQuery && matchesLang && matchesStyle
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalStorageUsed = repository.totalStorageUsed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalClipCount = repository.totalClipCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        // Observe active profile and populate studio preferences
        viewModelScope.launch {
            repository.activeProfile.collect { profile ->
                profile?.let { applyProfileDefaults(it) }
            }
        }

        // Preload default sample text for Bangla
        val defaultLang = SupportedLanguages.getByCode("bn")
        _studioState.value = _studioState.value.copy(
            inputText = defaultLang.sampleText,
            selectedLanguage = defaultLang
        )
        runLanguageDetection(defaultLang.sampleText)

        // Seed demo audio library if empty
        viewModelScope.launch {
            repository.seedSampleClipsIfEmpty()
        }
    }

    private fun applyProfileDefaults(profile: UserProfile) {
        val lang = SupportedLanguages.getByCode(profile.defaultLanguageCode)
        val style = VoiceStyle.fromId(profile.defaultVoiceStyle)
        val voice = AvailableGeminiVoices.voices.firstOrNull { it.name == profile.defaultVoiceName }
            ?: AvailableGeminiVoices.getDefaultVoice()
        val format = AudioFormatOption.fromExtension(profile.preferredAudioFormat)

        _studioState.value = _studioState.value.copy(
            selectedLanguage = lang,
            selectedStyle = style,
            selectedVoice = voice,
            selectedFormat = format,
            speechRate = profile.speechRate,
            pitch = profile.pitch,
            volume = profile.volume
        )
    }

    fun onInputTextChanged(text: String) {
        _studioState.value = _studioState.value.copy(inputText = text, errorMessage = null)
        runLanguageDetection(text)
    }

    private fun runLanguageDetection(text: String) {
        if (text.length >= 3) {
            val result = LanguageDetector.detect(text)
            _studioState.value = _studioState.value.copy(detectionResult = result)
        } else {
            _studioState.value = _studioState.value.copy(detectionResult = null)
        }
    }

    fun applyDetectedLanguage() {
        val detected = _studioState.value.detectionResult?.language ?: return
        selectLanguage(detected)
    }

    fun selectLanguage(language: LanguageItem) {
        val voice = AvailableGeminiVoices.voices.firstOrNull { it.name == language.recommendedVoice }
            ?: _studioState.value.selectedVoice
        _studioState.value = _studioState.value.copy(
            selectedLanguage = language,
            selectedVoice = voice
        )
    }

    fun loadSampleText(language: LanguageItem) {
        _studioState.value = _studioState.value.copy(
            inputText = language.sampleText,
            selectedLanguage = language
        )
        runLanguageDetection(language.sampleText)
    }

    fun selectVoiceStyle(style: VoiceStyle) {
        _studioState.value = _studioState.value.copy(
            selectedStyle = style,
            speechRate = (_studioState.value.speechRate * style.defaultRateModifier).coerceIn(0.5f, 2.0f),
            pitch = (_studioState.value.pitch + style.defaultPitchOffset).coerceIn(0.5f, 2.0f)
        )
    }

    fun selectVoice(voice: GeminiVoice) {
        _studioState.value = _studioState.value.copy(selectedVoice = voice)
    }

    fun selectAudioFormat(format: AudioFormatOption) {
        _studioState.value = _studioState.value.copy(selectedFormat = format)
    }

    fun updateSpeechRate(rate: Float) {
        _studioState.value = _studioState.value.copy(speechRate = rate)
    }

    fun updatePitch(pitch: Float) {
        _studioState.value = _studioState.value.copy(pitch = pitch)
    }

    fun updateVolume(volume: Float) {
        _studioState.value = _studioState.value.copy(volume = volume)
    }

    fun toggleOfflineMode() {
        _studioState.value = _studioState.value.copy(isOfflineMode = !_studioState.value.isOfflineMode)
    }

    fun generateSpeech() {
        val state = _studioState.value
        val text = state.inputText.trim()
        if (text.isEmpty()) {
            _studioState.value = state.copy(errorMessage = "Please enter some text to synthesize")
            return
        }

        _studioState.value = state.copy(isGenerating = true, errorMessage = null)

        viewModelScope.launch {
            val result = repository.synthesizeSpeech(
                text = text,
                language = state.selectedLanguage,
                voiceStyle = state.selectedStyle,
                voiceName = state.selectedVoice.name,
                speechRate = state.speechRate,
                pitch = state.pitch,
                volume = state.volume,
                format = state.selectedFormat,
                forceOffline = state.isOfflineMode
            )

            if (result.isSuccess) {
                val clip = result.getOrThrow()
                _studioState.value = _studioState.value.copy(
                    isGenerating = false,
                    latestClip = clip
                )
                // Automatically begin preview playback
                playerManager.playFile(clip.filePath, clip.id, clip.title)
                Toast.makeText(getApplication(), "Speech generated successfully!", Toast.LENGTH_SHORT).show()
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Synthesis failed"
                _studioState.value = _studioState.value.copy(
                    isGenerating = false,
                    errorMessage = errorMsg
                )
            }
        }
    }

    fun saveCurrentSettingsToProfile() {
        val profile = activeProfile.value ?: return
        val state = _studioState.value
        val updated = profile.copy(
            defaultLanguageCode = state.selectedLanguage.code,
            defaultVoiceStyle = state.selectedStyle.id,
            defaultVoiceName = state.selectedVoice.name,
            preferredAudioFormat = state.selectedFormat.extension,
            speechRate = state.speechRate,
            pitch = state.pitch,
            volume = state.volume
        )
        viewModelScope.launch {
            repository.updateProfile(updated)
            Toast.makeText(getApplication(), "Preferences saved to ${profile.name} profile", Toast.LENGTH_SHORT).show()
        }
    }

    fun switchProfile(profileId: Long) {
        viewModelScope.launch {
            repository.switchActiveProfile(profileId)
        }
    }

    fun createProfile(
        name: String,
        email: String,
        avatarColor: String,
        defaultLang: String,
        defaultStyle: String,
        defaultFormat: String
    ) {
        viewModelScope.launch {
            val newProfile = UserProfile(
                name = name,
                email = email,
                avatarColorHex = avatarColor,
                defaultLanguageCode = defaultLang,
                defaultVoiceStyle = defaultStyle,
                preferredAudioFormat = defaultFormat,
                isActiveProfile = true
            )
            val newId = repository.createProfile(newProfile)
            repository.switchActiveProfile(newId)
            Toast.makeText(getApplication(), "Profile '$name' created and activated", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.deleteProfile(profile)
            Toast.makeText(getApplication(), "Profile '${profile.name}' deleted", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteClip(clip: SpeechClip) {
        viewModelScope.launch {
            if (playerManager.playerState.value.currentClipId == clip.id) {
                playerManager.stop()
            }
            repository.deleteClip(clip)
            Toast.makeText(getApplication(), "Audio clip deleted", Toast.LENGTH_SHORT).show()
        }
    }

    fun downloadClipToDevice(clip: SpeechClip) {
        val format = AudioFormatOption.fromExtension(clip.audioFormat)
        val file = java.io.File(clip.filePath)
        if (!file.exists()) {
            Toast.makeText(getApplication(), "Audio file not found on disk", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = AudioExporter.exportToDownloads(getApplication(), file, clip.title, format)
        if (uri != null) {
            Toast.makeText(getApplication(), "Saved to Downloads/VoxCraft!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(getApplication(), "Saved to app storage", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareClip(clip: SpeechClip) {
        val format = AudioFormatOption.fromExtension(clip.audioFormat)
        val file = java.io.File(clip.filePath)
        if (file.exists()) {
            AudioExporter.shareAudio(getApplication(), file, clip.title, format)
        } else {
            Toast.makeText(getApplication(), "Audio file not found", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearAllLibrary() {
        viewModelScope.launch {
            playerManager.stop()
            repository.clearAllClips()
            Toast.makeText(getApplication(), "All audio files cleared", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.stop()
        repository.offlineEngine.shutdown()
    }
}

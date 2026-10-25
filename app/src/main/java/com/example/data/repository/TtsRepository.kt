package com.example.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.engine.OfflineTtsEngine
import com.example.data.local.VoxCraftDatabase
import com.example.data.model.AudioFormatOption
import com.example.data.model.LanguageItem
import com.example.data.model.SpeechClip
import com.example.data.model.SupportedLanguages
import com.example.data.model.UserProfile
import com.example.data.model.VoiceStyle
import com.example.data.remote.GeminiTtsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class TtsRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val database = VoxCraftDatabase.getInstance(context, scope)
    private val userProfileDao = database.userProfileDao()
    private val speechClipDao = database.speechClipDao()

    val geminiService = GeminiTtsService()
    val offlineEngine = OfflineTtsEngine(context)

    val allProfiles: Flow<List<UserProfile>> = userProfileDao.getAllProfiles()
    val activeProfile: Flow<UserProfile?> = userProfileDao.getActiveProfile()

    val allClips: Flow<List<SpeechClip>> = speechClipDao.getAllClips()
    val totalStorageUsed: Flow<Long?> = speechClipDao.getTotalStorageUsed()
    val totalClipCount: Flow<Int> = speechClipDao.getTotalClipCount()

    private val audioDir: File by lazy {
        File(context.filesDir, "audio_library").apply {
            if (!exists()) mkdirs()
        }
    }

    suspend fun getActiveProfileOnce(): UserProfile? = withContext(Dispatchers.IO) {
        userProfileDao.getActiveProfileOnce()
    }

    suspend fun createProfile(profile: UserProfile): Long = withContext(Dispatchers.IO) {
        userProfileDao.insertProfile(profile)
    }

    suspend fun updateProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        userProfileDao.updateProfile(profile)
    }

    suspend fun switchActiveProfile(profileId: Long) = withContext(Dispatchers.IO) {
        userProfileDao.clearActiveProfiles()
        userProfileDao.setActiveProfile(profileId)
    }

    suspend fun deleteProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        userProfileDao.deleteProfile(profile)
    }

    suspend fun deleteClip(clip: SpeechClip) = withContext(Dispatchers.IO) {
        val file = File(clip.filePath)
        if (file.exists()) {
            file.delete()
        }
        speechClipDao.deleteClip(clip)
    }

    suspend fun clearAllClips() = withContext(Dispatchers.IO) {
        audioDir.listFiles()?.forEach { it.delete() }
        speechClipDao.clearAll()
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun synthesizeSpeech(
        text: String,
        language: LanguageItem,
        voiceStyle: VoiceStyle,
        voiceName: String,
        speechRate: Float,
        pitch: Float,
        volume: Float,
        format: AudioFormatOption,
        forceOffline: Boolean = false
    ): Result<SpeechClip> = withContext(Dispatchers.IO) {
        val activeUser = userProfileDao.getActiveProfileOnce()
        val userId = activeUser?.id ?: 1L

        val timestamp = System.currentTimeMillis()
        val fileName = "vox_${language.code}_${voiceStyle.id}_$timestamp.${format.extension}"
        val targetFile = File(audioDir, fileName)

        var usedEngine = "gemini-3.8-flash-tts"
        var finalFileResult: Result<File>? = null

        // If online and not forcing offline, try Gemini Neural TTS first
        if (!forceOffline && isNetworkAvailable()) {
            val geminiResult = geminiService.synthesizeSpeech(
                text = text,
                language = language,
                voiceStyle = voiceStyle,
                voiceName = voiceName,
                speechRate = speechRate,
                pitch = pitch,
                format = format,
                outputFile = targetFile
            )

            if (geminiResult.isSuccess) {
                finalFileResult = geminiResult
                usedEngine = "gemini-3.8-flash-tts"
            } else {
                Log.w("TtsRepository", "Gemini synthesis error: ${geminiResult.exceptionOrNull()?.message}, falling back to Offline engine")
            }
        }

        // If Gemini failed or offline requested, use Android native Offline TTS engine
        if (finalFileResult == null || finalFileResult.isFailure) {
            usedEngine = "system-offline-tts"
            val offlineFile = if (format == AudioFormatOption.WAV) targetFile else File(audioDir, "vox_offline_$timestamp.wav")
            val offlineResult = offlineEngine.synthesizeToFile(
                text = text,
                language = language,
                style = voiceStyle,
                rate = speechRate,
                pitch = pitch,
                outputFile = offlineFile
            )
            finalFileResult = offlineResult
        }

        if (finalFileResult.isFailure) {
            return@withContext Result.failure(
                finalFileResult.exceptionOrNull()
                    ?: RuntimeException("Speech synthesis failed on both neural and offline engines")
            )
        }

        val generatedFile = finalFileResult.getOrThrow()
        val fileSizeBytes = generatedFile.length()

        // Read audio duration
        var durationMs = 0L
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(generatedFile.absolutePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationMs = durationStr?.toLongOrNull() ?: estimateDuration(text, speechRate)
            retriever.release()
        } catch (e: Exception) {
            durationMs = estimateDuration(text, speechRate)
        }

        val title = if (text.length <= 40) text else text.take(37) + "..."

        val clip = SpeechClip(
            title = title,
            originalText = text,
            languageCode = language.code,
            languageDisplayName = language.displayName,
            voiceStyle = voiceStyle.id,
            voiceName = voiceName,
            audioFormat = format.extension,
            speechRate = speechRate,
            pitch = pitch,
            volume = volume,
            filePath = generatedFile.absolutePath,
            fileSizeBytes = fileSizeBytes,
            durationMs = durationMs,
            engine = usedEngine,
            isDownloaded = true,
            isFavorite = false,
            createdAt = timestamp,
            userProfileId = userId
        )

        val insertedId = speechClipDao.insertClip(clip)
        Result.success(clip.copy(id = insertedId))
    }

    private fun estimateDuration(text: String, rate: Float): Long {
        val wordCount = text.split("\\s+".toRegex()).size.coerceAtLeast(1)
        val wordsPerMinute = 150f * rate.coerceIn(0.5f, 2.0f)
        val minutes = wordCount / wordsPerMinute
        return (minutes * 60 * 1000).toLong().coerceAtLeast(1000L)
    }

    /**
     * Seeds initial offline demo audio if library is empty
     */
    suspend fun seedSampleClipsIfEmpty() = withContext(Dispatchers.IO) {
        val count = speechClipDao.getTotalClipCount().firstOrNull() ?: 0
        if (count == 0) {
            val banglaLang = SupportedLanguages.getByCode("bn")
            val englishLang = SupportedLanguages.getByCode("en-US")
            val spanishLang = SupportedLanguages.getByCode("es")

            // Create sample clips
            val sampleItems = listOf(
                Triple("আমার সোনার বাংলা, আমি তোমায় ভালোবাসি।", banglaLang, VoiceStyle.NARRATIVE),
                Triple("Welcome to VoxCraft. Experience high-definition speech synthesis across languages.", englishLang, VoiceStyle.FORMAL),
                Triple("¡Hola amigos! Bienvenidos a nuestro estudio de locución inteligente.", spanishLang, VoiceStyle.CASUAL)
            )

            for ((text, lang, style) in sampleItems) {
                try {
                    synthesizeSpeech(
                        text = text,
                        language = lang,
                        voiceStyle = style,
                        voiceName = lang.recommendedVoice,
                        speechRate = 1.0f,
                        pitch = 1.0f,
                        volume = 1.0f,
                        format = AudioFormatOption.WAV,
                        forceOffline = true
                    )
                } catch (e: Exception) {
                    Log.w("TtsRepository", "Could not seed clip: ${e.message}")
                }
            }
        }
    }
}

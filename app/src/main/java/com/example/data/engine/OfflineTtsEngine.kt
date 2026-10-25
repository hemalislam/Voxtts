package com.example.data.engine

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.data.model.LanguageItem
import com.example.data.model.VoiceStyle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Locale
import java.util.UUID

class OfflineTtsEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized = _isInitialized.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking = _isSpeaking.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            _isInitialized.value = true
            setupProgressListener()
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isSpeaking.value = false
            }
        })
    }

    /**
     * Synthesizes audio to a file on disk for offline playback and downloading
     */
    suspend fun synthesizeToFile(
        text: String,
        language: LanguageItem,
        style: VoiceStyle,
        rate: Float,
        pitch: Float,
        outputFile: File
    ): Result<File> {
        val engine = tts ?: return Result.failure(IllegalStateException("TTS engine not ready"))

        // Set Language Locale
        val result = engine.setLanguage(language.locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Try language code alone
            engine.setLanguage(Locale(language.code))
        }

        // Apply rate and pitch modifiers
        val effectiveRate = (rate * style.defaultRateModifier).coerceIn(0.5f, 2.0f)
        val effectivePitch = (pitch + style.defaultPitchOffset).coerceIn(0.5f, 2.0f)

        engine.setSpeechRate(effectiveRate)
        engine.setPitch(effectivePitch)

        val utteranceId = "vox_offline_${UUID.randomUUID()}"
        val deferred = CompletableDeferred<Boolean>()

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {
                if (id == utteranceId) _isSpeaking.value = true
            }

            override fun onDone(id: String?) {
                if (id == utteranceId) {
                    _isSpeaking.value = false
                    deferred.complete(true)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                if (id == utteranceId) {
                    _isSpeaking.value = false
                    deferred.complete(false)
                }
            }

            override fun onError(id: String?, errorCode: Int) {
                if (id == utteranceId) {
                    _isSpeaking.value = false
                    deferred.complete(false)
                }
            }
        })

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)

        val success = engine.synthesizeToFile(text, params, outputFile, utteranceId)
        if (success == TextToSpeech.SUCCESS) {
            val completed = deferred.await()
            return if (completed && outputFile.exists() && outputFile.length() > 0) {
                Result.success(outputFile)
            } else {
                Result.failure(RuntimeException("Audio synthesis completed but output file is empty"))
            }
        } else {
            return Result.failure(RuntimeException("Failed to initiate offline speech synthesis (code: $success)"))
        }
    }

    fun speak(text: String, language: LanguageItem, rate: Float, pitch: Float) {
        val engine = tts ?: return
        engine.setLanguage(language.locale)
        engine.setSpeechRate(rate)
        engine.setPitch(pitch)
        val params = Bundle()
        val utteranceId = "vox_live_${System.currentTimeMillis()}"
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

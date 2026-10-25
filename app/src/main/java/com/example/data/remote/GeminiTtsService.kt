package com.example.data.remote

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.engine.AudioExporter
import com.example.data.model.AudioFormatOption
import com.example.data.model.LanguageItem
import com.example.data.model.VoiceStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class GeminiTtsService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "GeminiTtsService"
        // Primary requested model: gemini-3.8-flash-tts
        const val PRIMARY_TTS_MODEL = "gemini-3.8-flash-tts"
        // Alternative fallback endpoints in accordance with guidelines
        const val FALLBACK_TTS_MODEL = "gemini-3.8-flash"
        const val PREVIEW_TTS_MODEL = "gemini-2.5-flash-preview-tts"
    }

    suspend fun synthesizeSpeech(
        text: String,
        language: LanguageItem,
        voiceStyle: VoiceStyle,
        voiceName: String,
        speechRate: Float,
        pitch: Float,
        format: AudioFormatOption,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured in Secrets panel")
            )
        }

        // Try primary model first, then fallbacks
        val modelsToTry = listOf(PRIMARY_TTS_MODEL, FALLBACK_TTS_MODEL, PREVIEW_TTS_MODEL)
        var lastException: Exception? = null

        for (model in modelsToTry) {
            try {
                val result = callGeminiTtsEndpoint(
                    model = model,
                    apiKey = apiKey,
                    text = text,
                    language = language,
                    voiceStyle = voiceStyle,
                    voiceName = voiceName,
                    speechRate = speechRate,
                    pitch = pitch,
                    format = format,
                    outputFile = outputFile
                )
                if (result.isSuccess) {
                    return@withContext result
                } else {
                    lastException = result.exceptionOrNull() as? Exception
                }
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Model $model attempt failed: ${e.message}")
            }
        }

        Result.failure(lastException ?: RuntimeException("Gemini TTS synthesis failed"))
    }

    private fun callGeminiTtsEndpoint(
        model: String,
        apiKey: String,
        text: String,
        language: LanguageItem,
        voiceStyle: VoiceStyle,
        voiceName: String,
        speechRate: Float,
        pitch: Float,
        format: AudioFormatOption,
        outputFile: File
    ): Result<File> {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val promptPrompt = buildString {
            append("[Language: ${language.displayName} (${language.nativeName}) - ${language.accentDescription}]\n")
            append("[Voice Tone: ${voiceStyle.title} - ${voiceStyle.promptInstruction}]\n")
            append("[Pacing: ${String.format("%.2f", speechRate)}x speed, Pitch inflection: ${String.format("%.2f", pitch)}]\n\n")
            append(text)
        }

        val requestJson = JSONObject().apply {
            // Contents
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", promptPrompt)
                        })
                    })
                })
            })
            // GenerationConfig
            put("generationConfig", JSONObject().apply {
                put("responseModalities", JSONArray().apply {
                    put("AUDIO")
                })
                put("speechConfig", JSONObject().apply {
                    put("voiceConfig", JSONObject().apply {
                        put("prebuiltVoiceConfig", JSONObject().apply {
                            put("voiceName", voiceName)
                        })
                    })
                })
            })
        }

        val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Empty error response"
                return Result.failure(RuntimeException("Gemini TTS error ${response.code}: $errorBody"))
            }

            val responseBody = response.body?.string()
                ?: return Result.failure(RuntimeException("Empty response body from Gemini TTS"))

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return Result.failure(RuntimeException("No speech candidates returned from model"))
            }

            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts == null || parts.length() == 0) {
                return Result.failure(RuntimeException("No audio parts returned in response"))
            }

            var audioBase64: String? = null
            var mimeType = "audio/wav"

            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                val inlineData = part.optJSONObject("inlineData")
                if (inlineData != null) {
                    audioBase64 = inlineData.optString("data")
                    mimeType = inlineData.optString("mimeType", mimeType)
                    break
                }
            }

            if (audioBase64.isNullOrEmpty()) {
                return Result.failure(RuntimeException("Audio payload was empty in candidate response"))
            }

            val rawBytes = Base64.decode(audioBase64, Base64.DEFAULT)
            if (rawBytes.isEmpty()) {
                return Result.failure(RuntimeException("Decoded audio bytes were empty"))
            }

            // If audio is WAV/PCM, ensure correct header
            val finalBytes = if (format == AudioFormatOption.WAV || mimeType.contains("wav") || mimeType.contains("pcm")) {
                AudioExporter.ensureWavContainer(rawBytes, sampleRate = 24000L)
            } else {
                rawBytes
            }

            FileOutputStream(outputFile).use { fos ->
                fos.write(finalBytes)
                fos.flush()
            }

            return Result.success(outputFile)
        }
    }
}

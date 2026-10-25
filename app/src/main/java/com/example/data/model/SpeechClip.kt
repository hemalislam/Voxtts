package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speech_clips")
data class SpeechClip(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val originalText: String,
    val languageCode: String,
    val languageDisplayName: String,
    val voiceStyle: String,
    val voiceName: String,
    val audioFormat: String, // "wav", "mp3", "ogg"
    val speechRate: Float,
    val pitch: Float,
    val volume: Float,
    val filePath: String,
    val fileSizeBytes: Long,
    val durationMs: Long,
    val engine: String, // "gemini-3.8-flash-tts" or "system-offline-tts"
    val isDownloaded: Boolean = true,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val userProfileId: Long = 1L
)

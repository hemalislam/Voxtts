package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val email: String,
    val avatarColorHex: String = "#0284C7",
    val defaultLanguageCode: String = "en-US",
    val defaultVoiceStyle: String = "narrative",
    val defaultVoiceName: String = "Kore",
    val preferredAudioFormat: String = "wav",
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val volume: Float = 1.0f,
    val isActiveProfile: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

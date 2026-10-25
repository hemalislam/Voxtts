package com.example.data.model

enum class VoiceStyle(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val promptInstruction: String,
    val defaultPitchOffset: Float = 0f,
    val defaultRateModifier: Float = 1.0f
) {
    NARRATIVE(
        id = "narrative",
        title = "Narrative",
        description = "Warm, captivating cadence ideal for audiobooks, stories, and documentaries.",
        iconName = "AutoStories",
        promptInstruction = "Speak in a warm, immersive, and narrative storytelling tone with natural expressive pauses.",
        defaultPitchOffset = 0f,
        defaultRateModifier = 0.95f
    ),
    FORMAL(
        id = "formal",
        title = "Formal",
        description = "Clear, articulate, and authoritative tone suited for business, news, and official announcements.",
        iconName = "Work",
        promptInstruction = "Deliver in a formal, clear, confident, and professional tone with precise diction.",
        defaultPitchOffset = -0.05f,
        defaultRateModifier = 1.0f
    ),
    CASUAL(
        id = "casual",
        title = "Casual",
        description = "Relaxed, conversational, and energetic vibe perfect for podcasts, tutorials, and social content.",
        iconName = "ChatBubble",
        promptInstruction = "Speak in a friendly, upbeat, casual, and conversational style as if talking with a close friend.",
        defaultPitchOffset = 0.05f,
        defaultRateModifier = 1.05f
    ),
    EMPATHETIC(
        id = "empathetic",
        title = "Empathetic",
        description = "Soft, soothing, and compassionate voice for meditations, mindfulness, and gentle guidance.",
        iconName = "SelfImprovement",
        promptInstruction = "Use a gentle, soothing, empathetic, and reassuring tone with a soft and calming delivery.",
        defaultPitchOffset = -0.08f,
        defaultRateModifier = 0.9f
    );

    companion object {
        fun fromId(id: String): VoiceStyle {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: NARRATIVE
        }
    }
}

data class GeminiVoice(
    val name: String,
    val gender: String,
    val tone: String,
    val description: String
)

object AvailableGeminiVoices {
    val voices = listOf(
        GeminiVoice("Puck", "Male", "Bright & Energetic", "Modern youthful delivery with crisp clarity"),
        GeminiVoice("Charon", "Male", "Deep & Authoritative", "Resonant baritone tone with commanding presence"),
        GeminiVoice("Kore", "Female", "Calm & Professional", "Clear studio acoustic with balanced neutrality"),
        GeminiVoice("Aoede", "Female", "Melodic & Warm", "Gentle lyrical cadence full of warmth and empathy"),
        GeminiVoice("Fenrir", "Male", "Bold & Dramatic", "Cinematic richness suited for impactful narrations")
    )

    fun getDefaultVoice(): GeminiVoice = voices[2] // Kore
}

enum class AudioFormatOption(
    val extension: String,
    val mimeType: String,
    val label: String,
    val description: String
) {
    WAV("wav", "audio/wav", "WAV (Lossless)", "Highest audio fidelity, uncompressed 16-bit PCM standard"),
    MP3("mp3", "audio/mpeg", "MP3 (Universal)", "Widely compatible, lightweight compressed file"),
    OGG("ogg", "audio/ogg", "OGG (Vorbis)", "Efficient modern open audio format");

    companion object {
        fun fromExtension(ext: String): AudioFormatOption {
            return entries.firstOrNull { it.extension.equals(ext, ignoreCase = true) } ?: WAV
        }
    }
}

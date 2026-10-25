package com.example.data.model

import java.util.Locale

data class LanguageItem(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flag: String,
    val locale: Locale,
    val sampleText: String,
    val recommendedVoice: String,
    val accentDescription: String
)

object SupportedLanguages {
    val languages = listOf(
        LanguageItem(
            code = "bn",
            displayName = "Bangla (Bengali)",
            nativeName = "বাংলা",
            flag = "🇧🇩",
            locale = Locale("bn", "BD"),
            sampleText = "আমার সোনার বাংলা, আমি তোমায় ভালোবাসি। চিরদিন তোমার আকাশ, তোমার বাতাস, আমার প্রাণে বাজায় বাঁশি।",
            recommendedVoice = "Kore",
            accentDescription = "Melodic Standard Bengali"
        ),
        LanguageItem(
            code = "en-US",
            displayName = "English (US)",
            nativeName = "English (US)",
            flag = "🇺🇸",
            locale = Locale.US,
            sampleText = "Welcome to VoxCraft. Experience hyper-realistic speech synthesis powered by modern neural acoustics.",
            recommendedVoice = "Puck",
            accentDescription = "Standard American Modern"
        ),
        LanguageItem(
            code = "en-GB",
            displayName = "English (UK)",
            nativeName = "English (UK)",
            flag = "🇬🇧",
            locale = Locale.UK,
            sampleText = "Good day. Discover exceptionally articulate British narration designed for audiobooks and broadcasts.",
            recommendedVoice = "Charon",
            accentDescription = "Received Pronunciation"
        ),
        LanguageItem(
            code = "es",
            displayName = "Spanish (Spain)",
            nativeName = "Español (España)",
            flag = "🇪🇸",
            locale = Locale("es", "ES"),
            sampleText = "¡Hola! Bienvenidos a VoxCraft. Transforma tus historias escritas en locuciones llenas de vida y emoción.",
            recommendedVoice = "Aoede",
            accentDescription = "Castilian Spanish Natural"
        ),
        LanguageItem(
            code = "es-MX",
            displayName = "Spanish (Latin America)",
            nativeName = "Español (Latinoamérica)",
            flag = "🇲🇽",
            locale = Locale("es", "MX"),
            sampleText = "Buenos días a todos. Disfruta de la mejor calidad de audio para tus proyectos y contenidos digitales.",
            recommendedVoice = "Fenrir",
            accentDescription = "Latin American Neutral"
        ),
        LanguageItem(
            code = "fr",
            displayName = "French",
            nativeName = "Français",
            flag = "🇫🇷",
            locale = Locale.FRANCE,
            sampleText = "Bonjour et bienvenue dans notre studio vocal. Donnez une dimension sonore unique à vos projets.",
            recommendedVoice = "Kore",
            accentDescription = "Metropolitan French"
        ),
        LanguageItem(
            code = "hi",
            displayName = "Hindi",
            nativeName = "हिन्दी",
            flag = "🇮🇳",
            locale = Locale("hi", "IN"),
            sampleText = "नमस्ते! वोक्सक्राफ्ट में आपका स्वागत है। अपने शब्दों को एक प्रभावशाली और स्पष्ट आवाज़ दें।",
            recommendedVoice = "Aoede",
            accentDescription = "Standard Devanagari Hindi"
        ),
        LanguageItem(
            code = "de",
            displayName = "German",
            nativeName = "Deutsch",
            flag = "🇩🇪",
            locale = Locale.GERMANY,
            sampleText = "Guten Tag. Erleben Sie präzise und ausdrucksstarke Sprachausgabe für jede Anwendung.",
            recommendedVoice = "Charon",
            accentDescription = "Standard Hochdeutsch"
        ),
        LanguageItem(
            code = "ja",
            displayName = "Japanese",
            nativeName = "日本語",
            flag = "🇯🇵",
            locale = Locale.JAPAN,
            sampleText = "こんにちは。最先端の音声テクノロジーで、あなたの文章を自然で美しい声へと変換します。",
            recommendedVoice = "Kore",
            accentDescription = "Standard Tokyo Dialect"
        ),
        LanguageItem(
            code = "ar",
            displayName = "Arabic",
            nativeName = "العربية",
            flag = "🇸🇦",
            locale = Locale("ar", "SA"),
            sampleText = "مرحباً بكم في استوديو فوكس كرافت. نحول نصوصكم إلى نبرات صوتية طبيعية ومؤثرة بدقة فائقة.",
            recommendedVoice = "Fenrir",
            accentDescription = "Modern Standard Arabic"
        )
    )

    fun getByCode(code: String): LanguageItem {
        return languages.firstOrNull { it.code.equals(code, ignoreCase = true) }
            ?: languages.firstOrNull { it.code.startsWith(code.substringBefore("-"), ignoreCase = true) }
            ?: languages[1] // Default to English US
    }
}

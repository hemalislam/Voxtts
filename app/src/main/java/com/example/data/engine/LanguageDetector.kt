package com.example.data.engine

import com.example.data.model.LanguageItem
import com.example.data.model.SupportedLanguages

data class DetectionResult(
    val language: LanguageItem,
    val confidence: Float,
    val suggestion: String
)

object LanguageDetector {

    fun detect(text: String): DetectionResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return DetectionResult(
                language = SupportedLanguages.getByCode("en-US"),
                confidence = 0f,
                suggestion = "Type or paste text to detect language"
            )
        }

        // 1. Script-based Unicode detection (extremely accurate)
        var bengaliChars = 0
        var hindiChars = 0
        var arabicChars = 0
        var japaneseChars = 0
        var latinChars = 0

        for (ch in trimmed) {
            val code = ch.code
            when {
                code in 0x0980..0x09FF -> bengaliChars++
                code in 0x0900..0x097F -> hindiChars++
                code in 0x0600..0x06FF -> arabicChars++
                (code in 0x3040..0x309F) || (code in 0x30A0..0x30FF) -> japaneseChars++
                (code in 0x0041..0x005A) || (code in 0x0061..0x007A) ||
                (code in 0x00C0..0x024F) -> latinChars++
            }
        }

        val totalChars = trimmed.length.toFloat()

        if (bengaliChars > 0 && (bengaliChars.toFloat() / totalChars > 0.15f || bengaliChars >= 3)) {
            val conf = (bengaliChars.toFloat() / totalChars).coerceIn(0.75f, 0.99f)
            return DetectionResult(
                language = SupportedLanguages.getByCode("bn"),
                confidence = conf,
                suggestion = "Bangla script identified. Recommended: Melodic Bengali Voice with expressive pacing."
            )
        }

        if (hindiChars > 0 && (hindiChars.toFloat() / totalChars > 0.15f || hindiChars >= 3)) {
            val conf = (hindiChars.toFloat() / totalChars).coerceIn(0.75f, 0.99f)
            return DetectionResult(
                language = SupportedLanguages.getByCode("hi"),
                confidence = conf,
                suggestion = "Devanagari script detected. Recommended: Clear Hindi Articulation."
            )
        }

        if (arabicChars > 0 && (arabicChars.toFloat() / totalChars > 0.15f || arabicChars >= 3)) {
            val conf = (arabicChars.toFloat() / totalChars).coerceIn(0.75f, 0.99f)
            return DetectionResult(
                language = SupportedLanguages.getByCode("ar"),
                confidence = conf,
                suggestion = "Arabic script detected. Recommended: Resonant Standard Arabic."
            )
        }

        if (japaneseChars > 0 && (japaneseChars.toFloat() / totalChars > 0.15f || japaneseChars >= 2)) {
            val conf = (japaneseChars.toFloat() / totalChars).coerceIn(0.80f, 0.99f)
            return DetectionResult(
                language = SupportedLanguages.getByCode("ja"),
                confidence = conf,
                suggestion = "Japanese text detected. Recommended: Tokyo Dialect with natural pitch inflection."
            )
        }

        // 2. Latin token & accent analysis for Spanish, French, German, and English
        val lower = trimmed.lowercase()
        val tokens = lower.split(Regex("[\\s,.;:!?\"'()]+")).filter { it.isNotEmpty() }
        val tokenCount = tokens.size.coerceAtLeast(1)

        val spanishTokens = setOf(
            "el", "la", "los", "las", "un", "una", "unos", "unas", "de", "del", "que", "y", "en", "es",
            "por", "con", "para", "como", "su", "al", "hola", "gracias", "buenos", "dias", "está", "estás",
            "bienvenidos", "bienvenido", "amigo", "amiga", "mucho", "pero", "más", "este", "esta", "todos"
        )
        val spanishSpecial = lower.count { it in "áéíóúüñ¿¡" }
        val spanishMatches = tokens.count { it in spanishTokens }

        val frenchTokens = setOf(
            "le", "la", "les", "un", "une", "des", "du", "de", "et", "est", "dans", "pour", "avec", "sur",
            "bonjour", "merci", "oui", "non", "vous", "nous", "bienvenue", "cette", "plus", "tout", "faire"
        )
        val frenchSpecial = lower.count { it in "éèêàçœùîô" }
        val frenchMatches = tokens.count { it in frenchTokens }

        val germanTokens = setOf(
            "der", "die", "das", "und", "in", "zu", "den", "nicht", "von", "sie", "ist", "mit", "dem",
            "dass", "er", "es", "ein", "eine", "ich", "auf", "hallo", "danke", "guten", "tag", "schön"
        )
        val germanSpecial = lower.count { it in "äöüß" }
        val germanMatches = tokens.count { it in germanTokens }

        val englishTokens = setOf(
            "the", "be", "to", "of", "and", "a", "in", "that", "have", "i", "it", "for", "not", "on",
            "with", "he", "as", "you", "do", "at", "this", "but", "his", "by", "from", "they", "we",
            "say", "her", "she", "or", "an", "will", "my", "one", "all", "would", "there", "their",
            "what", "so", "up", "out", "if", "about", "who", "get", "which", "go", "me", "welcome"
        )
        val englishMatches = tokens.count { it in englishTokens }

        // Evaluate Spanish
        val spanishScore = spanishMatches * 2 + spanishSpecial * 3
        val frenchScore = frenchMatches * 2 + frenchSpecial * 3
        val germanScore = germanMatches * 2 + germanSpecial * 3
        val englishScore = englishMatches * 2

        val maxScore = maxOf(spanishScore, frenchScore, germanScore, englishScore)

        if (spanishScore > 0 && spanishScore == maxScore) {
            val conf = (spanishScore.toFloat() / (tokenCount * 2)).coerceIn(0.65f, 0.98f)
            return DetectionResult(
                language = SupportedLanguages.getByCode("es"),
                confidence = conf,
                suggestion = "Spanish detected. Suggested: Castilian or Latin American accent with expressive tone."
            )
        }

        if (frenchScore > 0 && frenchScore == maxScore) {
            val conf = (frenchScore.toFloat() / (tokenCount * 2)).coerceIn(0.65f, 0.98f)
            return DetectionResult(
                language = SupportedLanguages.getByCode("fr"),
                confidence = conf,
                suggestion = "French detected. Suggested: Metropolitan French accent with smooth liaison."
            )
        }

        if (germanScore > 0 && germanScore == maxScore) {
            val conf = (germanScore.toFloat() / (tokenCount * 2)).coerceIn(0.65f, 0.98f)
            return DetectionResult(
                language = SupportedLanguages.getByCode("de"),
                confidence = conf,
                suggestion = "German detected. Suggested: Crisp Hochdeutsch articulation."
            )
        }

        val conf = (englishScore.toFloat() / (tokenCount * 2)).coerceIn(0.55f, 0.95f)
        return DetectionResult(
            language = SupportedLanguages.getByCode("en-US"),
            confidence = conf,
            suggestion = "English detected. Suggested: Standard American or British RP voice."
        )
    }
}

package com.example

import com.example.data.engine.AudioExporter
import com.example.data.engine.LanguageDetector
import com.example.data.model.AudioFormatOption
import com.example.data.model.SupportedLanguages
import com.example.data.model.VoiceStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testLanguageDetector_detectsBangla() {
        val bengaliText = "আমার সোনার বাংলা, আমি তোমায় ভালোবাসি।"
        val result = LanguageDetector.detect(bengaliText)
        assertEquals("bn", result.language.code)
        assertTrue(result.confidence > 0.7f)
    }

    @Test
    fun testLanguageDetector_detectsSpanish() {
        val spanishText = "¡Hola! Bienvenidos a nuestro estudio de voz, ¿cómo estás hoy amigo?"
        val result = LanguageDetector.detect(spanishText)
        assertEquals("es", result.language.code)
        assertTrue(result.confidence > 0.5f)
    }

    @Test
    fun testLanguageDetector_detectsEnglish() {
        val englishText = "Welcome to the studio. High quality natural speech synthesis for everyone."
        val result = LanguageDetector.detect(englishText)
        assertEquals("en-US", result.language.code)
    }

    @Test
    fun testWavHeaderCreation_is44BytesWithRiff() {
        val header = AudioExporter.createWavHeader(
            totalAudioLen = 1000L,
            totalDataLen = 1036L,
            longSampleRate = 24000L
        )
        assertEquals(44, header.size)
        assertEquals('R'.code.toByte(), header[0])
        assertEquals('I'.code.toByte(), header[1])
        assertEquals('F'.code.toByte(), header[2])
        assertEquals('F'.code.toByte(), header[3])
    }

    @Test
    fun testVoiceStyles_containRequiredStyles() {
        assertNotNull(VoiceStyle.fromId("narrative"))
        assertNotNull(VoiceStyle.fromId("formal"))
        assertNotNull(VoiceStyle.fromId("casual"))
        assertNotNull(VoiceStyle.fromId("empathetic"))
    }

    @Test
    fun testAudioFormats_supportWavMp3Ogg() {
        assertEquals(AudioFormatOption.WAV, AudioFormatOption.fromExtension("wav"))
        assertEquals(AudioFormatOption.MP3, AudioFormatOption.fromExtension("mp3"))
        assertEquals(AudioFormatOption.OGG, AudioFormatOption.fromExtension("ogg"))
    }
}

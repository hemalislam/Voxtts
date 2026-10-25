package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioFormatOption
import com.example.data.model.AvailableGeminiVoices
import com.example.data.model.SupportedLanguages
import com.example.data.model.VoiceStyle
import com.example.ui.VoxCraftViewModel

data class VoicePreset(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val style: VoiceStyle,
    val voiceName: String,
    val rate: Float,
    val pitch: Float,
    val volume: Float,
    val format: AudioFormatOption,
    val testPhrase: String
)

@Composable
fun VoiceLabScreen(
    viewModel: VoxCraftViewModel,
    modifier: Modifier = Modifier
) {
    val studioState by viewModel.studioState.collectAsState()
    val scrollState = rememberScrollState()

    val presets = remember {
        listOf(
            VoicePreset(
                title = "Audiobook Narrator",
                subtitle = "Immersive storytelling with deliberate pacing and expressive pauses",
                icon = Icons.Default.Book,
                style = VoiceStyle.NARRATIVE,
                voiceName = "Kore",
                rate = 0.95f,
                pitch = 0.98f,
                volume = 1.0f,
                format = AudioFormatOption.WAV,
                testPhrase = "In a quiet corner of the ancient library, a forgotten manuscript began to glow with sapphire light."
            ),
            VoicePreset(
                title = "Executive Briefing",
                subtitle = "Authoritative, crisp diction for boardroom reports and formal keynote delivery",
                icon = Icons.Default.BusinessCenter,
                style = VoiceStyle.FORMAL,
                voiceName = "Charon",
                rate = 1.05f,
                pitch = 0.94f,
                volume = 1.0f,
                format = AudioFormatOption.MP3,
                testPhrase = "Global operations have achieved twenty-four percent year-over-year revenue growth across all key metrics."
            ),
            VoicePreset(
                title = "Vlog & Podcast Host",
                subtitle = "Dynamic, upbeat cadence filled with conversational warmth and energy",
                icon = Icons.Default.Podcasts,
                style = VoiceStyle.CASUAL,
                voiceName = "Puck",
                rate = 1.10f,
                pitch = 1.06f,
                volume = 1.0f,
                format = AudioFormatOption.WAV,
                testPhrase = "Hey everyone! Welcome back to the channel. Today we're diving into something super exciting that you cannot miss!"
            ),
            VoicePreset(
                title = "Mindfulness & Sleep Guide",
                subtitle = "Gentle, whisper-soft intonation designed for guided meditation and relaxation",
                icon = Icons.Default.Psychology,
                style = VoiceStyle.EMPATHETIC,
                voiceName = "Aoede",
                rate = 0.85f,
                pitch = 0.90f,
                volume = 0.90f,
                format = AudioFormatOption.OGG,
                testPhrase = "Take a slow, deep breath in... and exhale gently, letting go of all tension and worry."
            ),
            VoicePreset(
                title = "Bangla & Multilingual Tutor",
                subtitle = "Articulate native phonetics crafted for language learners and clear pronounciation",
                icon = Icons.Default.School,
                style = VoiceStyle.NARRATIVE,
                voiceName = "Kore",
                rate = 0.90f,
                pitch = 1.0f,
                volume = 1.0f,
                format = AudioFormatOption.WAV,
                testPhrase = "বাংলা ভাষা অত্যন্ত সমৃদ্ধ ও মিষ্টি। প্রতিটি শব্দের উচ্চারণ স্পষ্ট ও সুন্দর হওয়া জরুরি।"
            )
        )
    }

    var selectedPreset by remember { mutableStateOf(presets[0]) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Voice Customization Lab",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Fine-tune tone parameters, audition curated vocal presets, and save custom styles to your profile.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Curated Presets
        Text(
            text = "Curated Vocal Presets",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            presets.forEach { preset ->
                val isSelected = selectedPreset.title == preset.title
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedPreset = preset
                            viewModel.selectVoiceStyle(preset.style)
                            AvailableGeminiVoices.voices.firstOrNull { it.name == preset.voiceName }?.let {
                                viewModel.selectVoice(it)
                            }
                            viewModel.updateSpeechRate(preset.rate)
                            viewModel.updatePitch(preset.pitch)
                            viewModel.updateVolume(preset.volume)
                            viewModel.selectAudioFormat(preset.format)
                        }
                        .testTag("preset_${preset.title.replace(" ", "_")}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.surfaceContainer
                    ),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = preset.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = preset.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = preset.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "${preset.rate}x speed",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "${preset.voiceName} (${preset.style.title})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Audition Sandbox Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Live Audition Sandbox",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "\"${selectedPreset.testPhrase}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Real-time Audition Button using Offline TTS Live Engine
                Button(
                    onClick = {
                        val lang = if (selectedPreset.title.contains("Bangla")) {
                            SupportedLanguages.getByCode("bn")
                        } else {
                            SupportedLanguages.getByCode("en-US")
                        }
                        viewModel.repository.offlineEngine.speak(
                            text = selectedPreset.testPhrase,
                            language = lang,
                            rate = studioState.speechRate,
                            pitch = studioState.pitch
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("audition_test_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Audition Phrase (Live Audio)")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.saveCurrentSettingsToProfile()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("apply_preset_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Set as Default in My Profile")
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

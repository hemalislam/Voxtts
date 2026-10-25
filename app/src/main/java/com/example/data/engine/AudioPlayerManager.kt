package com.example.data.engine

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class PlayerState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val isLooping: Boolean = false,
    val currentClipId: Long? = null,
    val currentFilePath: String? = null,
    val currentTitle: String = ""
)

class AudioPlayerManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState = _playerState.asStateFlow()

    // 24 simulated waveform bar heights (0.1f .. 1.0f) for the current audio file
    private val _waveformAmplitudes = MutableStateFlow(generateWaveformAmplitudes(null))
    val waveformAmplitudes = _waveformAmplitudes.asStateFlow()

    fun playFile(filePath: String, clipId: Long? = null, title: String = "") {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e("AudioPlayerManager", "Audio file not found: $filePath")
            return
        }

        try {
            stop()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                isLooping = _playerState.value.isLooping

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        playbackParams = playbackParams.setSpeed(_playerState.value.playbackSpeed)
                    } catch (e: Exception) {
                        Log.w("AudioPlayerManager", "Failed to set playback speed: ${e.message}")
                    }
                }

                setOnCompletionListener {
                    if (!_playerState.value.isLooping) {
                        _playerState.value = _playerState.value.copy(
                            isPlaying = false,
                            currentPositionMs = duration
                        )
                        stopProgressTicker()
                    }
                }

                start()
            }

            val duration = mediaPlayer?.duration ?: 0
            _playerState.value = _playerState.value.copy(
                isPlaying = true,
                currentPositionMs = 0,
                durationMs = duration,
                currentClipId = clipId,
                currentFilePath = filePath,
                currentTitle = title
            )

            _waveformAmplitudes.value = generateWaveformAmplitudes(filePath)
            startProgressTicker()
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error playing audio file: ${e.message}", e)
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _playerState.value = _playerState.value.copy(isPlaying = false)
            stopProgressTicker()
        } else {
            player.start()
            _playerState.value = _playerState.value.copy(isPlaying = true)
            startProgressTicker()
        }
    }

    fun seekTo(positionMs: Int) {
        val player = mediaPlayer ?: return
        val clamped = positionMs.coerceIn(0, player.duration)
        player.seekTo(clamped)
        _playerState.value = _playerState.value.copy(currentPositionMs = clamped)
    }

    fun skipForward(deltaMs: Int = 10000) {
        val current = _playerState.value.currentPositionMs
        seekTo(current + deltaMs)
    }

    fun skipBackward(deltaMs: Int = 10000) {
        val current = _playerState.value.currentPositionMs
        seekTo(current - deltaMs)
    }

    fun setSpeed(speed: Float) {
        _playerState.value = _playerState.value.copy(playbackSpeed = speed)
        val player = mediaPlayer ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                player.playbackParams = player.playbackParams.setSpeed(speed)
            } catch (e: Exception) {
                Log.w("AudioPlayerManager", "Failed to set speed: ${e.message}")
            }
        }
    }

    fun toggleLoop() {
        val newLoop = !_playerState.value.isLooping
        _playerState.value = _playerState.value.copy(isLooping = newLoop)
        mediaPlayer?.isLooping = newLoop
    }

    fun stop() {
        stopProgressTicker()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.w("AudioPlayerManager", "Error releasing player: ${e.message}")
        }
        mediaPlayer = null
        _playerState.value = _playerState.value.copy(isPlaying = false, currentPositionMs = 0)
    }

    private fun startProgressTicker() {
        stopProgressTicker()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive && mediaPlayer != null && mediaPlayer?.isPlaying == true) {
                val current = mediaPlayer?.currentPosition ?: 0
                val dur = mediaPlayer?.duration ?: _playerState.value.durationMs
                _playerState.value = _playerState.value.copy(
                    currentPositionMs = current,
                    durationMs = dur
                )
                delay(100)
            }
        }
    }

    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun generateWaveformAmplitudes(seedString: String?): List<Float> {
        val seed = seedString?.hashCode()?.toLong() ?: 12345L
        val random = java.util.Random(seed)
        return List(28) {
            0.15f + random.nextFloat() * 0.85f
        }
    }
}

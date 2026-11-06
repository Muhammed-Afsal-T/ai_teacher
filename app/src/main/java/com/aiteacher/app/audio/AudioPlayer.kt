package com.aiteacher.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log

class AudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun playAudio(url: String, onCompletion: () -> Unit = {}, onError: (String) -> Unit = {}) {
        stopAudio()

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            try {
                setDataSource(url)
                setOnPreparedListener { mp ->
                    Log.d("AudioPlayer", "Audio prepared. Starting playback...")
                    mp.start()
                }
                setOnCompletionListener {
                    Log.d("AudioPlayer", "Audio playback completed.")
                    releasePlayer()
                    onCompletion()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPlayer", "Audio playback error: what=$what, extra=$extra")
                    releasePlayer()
                    onError("Playback error: $what")
                    true
                }
                prepareAsync()
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Failed to setup audio playback: ${e.message}", e)
                releasePlayer()
                onError(e.message ?: "Failed to play audio")
            }
        }
    }

    fun stopAudio() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                reset()
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    private fun releasePlayer() {
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }
}

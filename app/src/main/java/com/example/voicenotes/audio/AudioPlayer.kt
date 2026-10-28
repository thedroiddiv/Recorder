package com.example.voicenotes.audio

import android.media.AudioAttributes
import android.media.MediaPlayer

class AudioPlayer {

    private var mediaPlayer: MediaPlayer? = null

    val isPlaying: Boolean
        get() = runCatching { mediaPlayer?.isPlaying == true }.getOrDefault(false)

    val positionMs: Long
        get() = runCatching { mediaPlayer?.currentPosition?.toLong() ?: 0L }.getOrDefault(0L)

    val durationMs: Long
        get() = runCatching { mediaPlayer?.duration?.toLong() ?: 0L }.getOrDefault(0L)

    fun play(filePath: String, onCompleted: () -> Unit) {
        release()
        val player = MediaPlayer()
        try {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            player.setDataSource(filePath)
            player.setOnCompletionListener { onCompleted() }
            player.prepare()
            player.start()
            mediaPlayer = player
        } catch (error: Throwable) {
            player.release()
            throw error
        }
    }

    fun pause() {
        runCatching { mediaPlayer?.pause() }
    }

    fun resume() {
        runCatching { mediaPlayer?.start() }
    }

    fun seekTo(positionMs: Long) {
        runCatching { mediaPlayer?.seekTo(positionMs.coerceAtLeast(0L).toInt()) }
    }

    fun release() {
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
    }
}

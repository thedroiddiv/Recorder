package com.example.voicenotes.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException

class RecorderController(
    private val context: Context,
    private val onStarted: () -> Unit,
    private val onAmplitude: (Float) -> Unit,
    private val onCompleted: (String, Long) -> Unit,
    private val onError: (String) -> Unit
) {
    private val recordingsDir = File(context.filesDir, "recordings").apply { mkdirs() }
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startTimeMs: Long = 0L

    @SuppressLint("MissingPermission")
    fun start(sampleRate: Int): Boolean {
        if (mediaRecorder != null) return false

        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            onError("Microphone permission is required")
            return false
        }

        val file = File(recordingsDir, "voice_note_${System.currentTimeMillis()}.m4a")
        currentFile = file

        return try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioSamplingRate(sampleRate)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
            startTimeMs = System.currentTimeMillis()
            onStarted()
            true
        } catch (error: Throwable) {
            mediaRecorder?.release()
            mediaRecorder = null
            currentFile = null
            file.delete()
            onError(error.message ?: "Unable to start recording")
            false
        }
    }

    fun stop() {
        val recorder = mediaRecorder ?: return
        val file = currentFile
        try {
            recorder.stop()
        } catch (error: Throwable) {
            onError(error.message ?: "Unable to stop recording")
            mediaRecorder?.release()
            mediaRecorder = null
            currentFile = null
            file?.delete()
            return
        }
        recorder.release()
        mediaRecorder = null
        val durationMs = if (startTimeMs > 0L) System.currentTimeMillis() - startTimeMs else 0L
        startTimeMs = 0L
        if (file != null) {
            onCompleted(file.absolutePath, durationMs)
        }
        currentFile = null
    }

    fun getMaxAmplitude(): Float {
        val recorder = mediaRecorder ?: return 0F
        return try {
            val amp = recorder.maxAmplitude
            amp.coerceAtLeast(0).toFloat()
        } catch (error: Throwable) {
            0F
        }
    }
}

package com.example.voicenotes.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.daiatech.karya.rawaudiorecorder.RawAudioRecorder
import com.daiatech.karya.rawaudiorecorder.RecorderConfig
import com.daiatech.karya.rawaudiorecorder.RecorderEventListener
import com.daiatech.karya.rawaudiorecorder.SampleRate
import java.io.File
import kotlinx.coroutines.CoroutineScope

class RecorderController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onStarted: () -> Unit,
    private val onAmplitude: (Float) -> Unit,
    private val onCompleted: (String, Long) -> Unit,
    private val onError: (String) -> Unit
) : RecorderEventListener {

    private val recordingsDir = File(context.filesDir, "recordings").apply { mkdirs() }
    private var recorder: RawAudioRecorder? = null
    private var currentFile: File? = null

    @SuppressLint("MissingPermission")
    fun start(sampleRate: Int): Boolean {
        if (recorder != null) return false

        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            onError("Microphone permission is required")
            return false
        }

        val file = File(recordingsDir, "voice_note_${System.currentTimeMillis()}.wav")
        currentFile = file

        return try {
            val config = RecorderConfig(
                sampleRate = if (sampleRate == 44_100) SampleRate._44_K else SampleRate._16_K
            )
            val newRecorder = RawAudioRecorder(this, scope)
            newRecorder.prepare(file.absolutePath, config)
            recorder = newRecorder
            newRecorder.startRecording()
            true
        } catch (error: Throwable) {
            recorder = null
            currentFile = null
            file.delete()
            onError(error.message ?: "Unable to start recording")
            false
        }
    }

    fun stop() {
        recorder?.stopRecording()
    }

    override fun onPrepared() = Unit

    override fun onStart() {
        onStarted()
    }

    override fun onPause() = Unit

    override fun onResume() = Unit

    override fun onStop(durationMs: Long) {
        val file = currentFile
        recorder = null
        currentFile = null
        if (file != null) {
            onCompleted(file.absolutePath, durationMs)
        }
    }

    override fun onProgressUpdate(maxAmplitude: Int, duration: Long) {
        onAmplitude(maxAmplitude.coerceAtLeast(0).toFloat())
    }
}

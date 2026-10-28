package com.example.voicenotes

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicenotes.audio.AudioPlayer
import com.example.voicenotes.audio.RecorderController
import com.example.voicenotes.data.Note
import com.example.voicenotes.data.NotesRepository
import com.example.voicenotes.data.SettingsStore
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RecorderUiState(
    val isRecording: Boolean = false,
    val isSaving: Boolean = false,
    val elapsedMs: Long = 0L,
    val amplitude: Float = 0F,
    val error: String? = null
)

data class PlaybackUiState(
    val noteId: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val error: String? = null
)

class VoiceNotesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NotesRepository(application)
    private val settingsStore = SettingsStore(application)
    private val player = AudioPlayer()

    private val _notes = MutableStateFlow(repository.loadNotes())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _recorderState = MutableStateFlow(RecorderUiState())
    val recorderState: StateFlow<RecorderUiState> = _recorderState.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackUiState())
    val playbackState: StateFlow<PlaybackUiState> = _playbackState.asStateFlow()

    val sampleRate: StateFlow<Int> = settingsStore.sampleRate.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SettingsStore.DEFAULT_SAMPLE_RATE
    )

    private var recordingTimer: Job? = null
    private var amplitudeTimer: Job? = null
    private var playbackTicker: Job? = null

    private val recorderController = RecorderController(
        context = application,
        onStarted = ::handleRecordingStarted,
        onAmplitude = { amplitude ->
            _recorderState.update { it.copy(amplitude = amplitude) }
        },
        onCompleted = ::handleRecordingCompleted,
        onError = { message ->
            _recorderState.update { it.copy(error = message, isRecording = false, isSaving = false) }
        }
    )

    fun startRecording(): Boolean {
        if (_recorderState.value.isRecording) return false
        _recorderState.value = RecorderUiState(isRecording = true, amplitude = 0F)
        val started = recorderController.start(sampleRate.value)
        if (started) {
            startAmplitudeUpdates()
        }
        return started
    }

    fun stopRecording() {
        if (!_recorderState.value.isRecording) return
        recordingTimer?.cancel()
        amplitudeTimer?.cancel()
        _recorderState.update { it.copy(isSaving = true) }
        recorderController.stop()
    }

    fun setSampleRate(rate: Int) {
        viewModelScope.launch {
            settingsStore.setSampleRate(rate)
        }
    }

    fun togglePlayback(note: Note) {
        val current = _playbackState.value
        if (current.noteId == note.id && player.isPlaying) {
            player.pause()
            playbackTicker?.cancel()
            _playbackState.update {
                it.copy(isPlaying = false, positionMs = player.positionMs)
            }
            return
        }

        if (current.noteId == note.id) {
            if (current.positionMs >= note.durationMs - 50L) {
                player.seekTo(0L)
                _playbackState.update { it.copy(positionMs = 0L) }
            }
            player.resume()
            _playbackState.update { it.copy(isPlaying = true, error = null) }
            startPlaybackTicker(note)
            return
        }

        startPlayback(note)
    }

    fun seekTo(note: Note, fraction: Float) {
        if (_playbackState.value.noteId != note.id) {
            startPlayback(note)
        }
        if (_playbackState.value.noteId != note.id) return

        val positionMs = (fraction.coerceIn(0F, 1F) * note.durationMs).toLong()
        player.seekTo(positionMs)
        _playbackState.update { it.copy(positionMs = positionMs) }
    }

    fun stopPlayback() {
        playbackTicker?.cancel()
        player.release()
        _playbackState.value = PlaybackUiState()
    }

    private fun startAmplitudeUpdates() {
        amplitudeTimer?.cancel()
        amplitudeTimer = viewModelScope.launch {
            while (isActive && _recorderState.value.isRecording) {
                val amp = recorderController.getMaxAmplitude()
                _recorderState.update { it.copy(amplitude = amp) }
                delay(50)
            }
        }
    }

    private fun handleRecordingStarted() {
        recordingTimer?.cancel()
        val startedAt = SystemClock.elapsedRealtime()
        _recorderState.update { it.copy(isRecording = true, isSaving = false, error = null) }
        recordingTimer = viewModelScope.launch {
            while (isActive) {
                _recorderState.update {
                    it.copy(elapsedMs = SystemClock.elapsedRealtime() - startedAt)
                }
                delay(50)
            }
        }
    }

    private fun handleRecordingCompleted(filePath: String, durationMs: Long) {
        recordingTimer?.cancel()
        amplitudeTimer?.cancel()

        if (durationMs <= 0L) {
            File(filePath).delete()
            _recorderState.value = RecorderUiState(error = "Recording was too short")
            return
        }

        viewModelScope.launch {
            val createdAt = System.currentTimeMillis()
            val note = Note(
                id = UUID.randomUUID().toString(),
                title = buildNoteTitle(createdAt),
                filePath = filePath,
                durationMs = durationMs,
                createdAt = createdAt
            )
            val updatedNotes = withContext(Dispatchers.IO) {
                repository.addNote(note)
            }
            _notes.value = updatedNotes
            _recorderState.value = RecorderUiState()
        }
    }

    private fun buildNoteTitle(createdAt: Long): String {
        val timestamp = DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT,
            Locale.getDefault()
        ).format(Date(createdAt))
        return "Voice note · $timestamp"
    }

    private fun startPlayback(note: Note) {
        playbackTicker?.cancel()

        try {
            player.play(note.filePath) {
                playbackTicker?.cancel()
                _playbackState.update {
                    it.copy(isPlaying = false, positionMs = it.durationMs)
                }
            }
            _playbackState.value = PlaybackUiState(
                noteId = note.id,
                isPlaying = true,
                positionMs = 0L,
                durationMs = note.durationMs
            )
            startPlaybackTicker(note)
        } catch (error: Throwable) {
            _playbackState.value = PlaybackUiState(
                noteId = note.id,
                durationMs = note.durationMs,
                error = "Unable to play this recording"
            )
        }
    }

    private fun startPlaybackTicker(note: Note) {
        playbackTicker?.cancel()
        playbackTicker = viewModelScope.launch {
            while (isActive && player.isPlaying) {
                delay(100)
                _playbackState.update {
                    it.copy(positionMs = player.positionMs.coerceIn(0L, note.durationMs))
                }
            }
        }
    }

    override fun onCleared() {
        recordingTimer?.cancel()
        amplitudeTimer?.cancel()
        playbackTicker?.cancel()
        recorderController.stop()
        player.release()
    }
}

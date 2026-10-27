package com.example.voicenotes.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voicenotes.VoiceNotesViewModel

private enum class Route {
    Notes,
    Record,
    Detail,
    Settings
}

@Composable
fun VoiceNotesApp(viewModel: VoiceNotesViewModel = viewModel()) {
    val notes by viewModel.notes.collectAsState()
    val recorderState by viewModel.recorderState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val sampleRate by viewModel.sampleRate.collectAsState()

    var routeName by rememberSaveable { mutableStateOf(Route.Notes.name) }
    var selectedNoteId by rememberSaveable { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun beginRecording(granted: Boolean) {
        if (!granted) {
            showMessage("Microphone permission is required to record.")
            return
        }

        viewModel.stopPlayback()
        if (viewModel.startRecording()) {
            routeName = Route.Record.name
        } else {
            showMessage(viewModel.recorderState.value.error ?: "Unable to start recording.")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        beginRecording(granted)
    }

    fun requestRecording() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            beginRecording(true)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun closeDetail() {
        viewModel.stopPlayback()
        selectedNoteId = null
        routeName = Route.Notes.name
    }

    fun leaveRecording() {
        viewModel.stopRecording()
        routeName = Route.Notes.name
    }

    BackHandler(enabled = routeName != Route.Notes.name) {
        when (routeName) {
            Route.Record.name -> leaveRecording()
            Route.Detail.name -> closeDetail()
            else -> routeName = Route.Notes.name
        }
    }

    when (routeName) {
        Route.Record.name -> {
            RecordScreen(
                state = recorderState,
                onStop = viewModel::stopRecording,
                onBack = ::leaveRecording,
                onFinished = { routeName = Route.Notes.name }
            )
        }

        Route.Detail.name -> {
            val note = notes.firstOrNull { it.id == selectedNoteId }
            if (note == null) {
                LaunchedEffect(Unit) {
                    routeName = Route.Notes.name
                }
            } else {
                DetailScreen(
                    note = note,
                    playback = playbackState,
                    onBack = ::closeDetail,
                    onTogglePlayback = { viewModel.togglePlayback(note) },
                    onSeek = { progress -> viewModel.seekTo(note, progress) },
                    onStopPlayback = viewModel::stopPlayback
                )
            }
        }

        Route.Settings.name -> {
            SettingsScreen(
                sampleRate = sampleRate,
                onSampleRateChange = viewModel::setSampleRate,
                onBack = { routeName = Route.Notes.name }
            )
        }

        else -> {
            NotesListScreen(
                notes = notes,
                onRecordClick = ::requestRecording,
                onNoteClick = { note ->
                    selectedNoteId = note.id
                    routeName = Route.Detail.name
                },
                onSettingsClick = { routeName = Route.Settings.name }
            )
        }
    }
}

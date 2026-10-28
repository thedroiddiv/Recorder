package com.example.voicenotes.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.voicenotes.VoiceNotesViewModel

private object Routes {
    const val Notes = "notes"
    const val Record = "record"
    const val Detail = "detail/{noteId}"
    const val Settings = "settings"
}

@Composable
fun VoiceNotesApp(viewModel: VoiceNotesViewModel = viewModel()) {
    val navController = rememberNavController()
    val notes by viewModel.notes.collectAsState()
    val recorderState by viewModel.recorderState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val sampleRate by viewModel.sampleRate.collectAsState()

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
            navController.navigate(Routes.Record) {
                launchSingleTop = true
            }
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

    LaunchedEffect(recorderState.isRecording, recorderState.isSaving, recorderState.lastSavedNoteId, notes) {
        if (!recorderState.isRecording && !recorderState.isSaving) {
            val savedId = recorderState.lastSavedNoteId
            if (savedId != null) {
                val note = notes.firstOrNull { it.id == savedId } ?: notes.lastOrNull()
                if (note != null) {
                    navController.navigate("detail/${note.id}") {
                        popUpTo(Routes.Notes) { inclusive = false }
                        launchSingleTop = true
                    }
                    return@LaunchedEffect
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.Notes
    ) {
        composable(Routes.Notes) {
            NotesListScreen(
                notes = notes,
                onRecordClick = ::requestRecording,
                onNoteClick = { note ->
                    viewModel.stopPlayback()
                    navController.navigate("detail/${note.id}")
                },
                onSettingsClick = { navController.navigate(Routes.Settings) }
            )
        }

        composable(Routes.Record) {
            RecordScreen(
                state = recorderState,
                onStop = viewModel::stopRecording,
                onBack = {
                    viewModel.stopRecording()
                    navController.popBackStack()
                },
                onFinished = {
                    navController.popBackStack(Routes.Notes, inclusive = false)
                }
            )
        }

        composable(
            route = Routes.Detail,
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")
            val note = notes.firstOrNull { it.id == noteId }
            if (note == null) {
                LaunchedEffect(Unit) {
                    navController.popBackStack(Routes.Notes, inclusive = false)
                }
            } else {
                DetailScreen(
                    note = note,
                    playback = playbackState,
                    onBack = {
                        viewModel.stopPlayback()
                        navController.popBackStack()
                    },
                    onTogglePlayback = { viewModel.togglePlayback(note) },
                    onSeek = { progress -> viewModel.seekTo(note, progress) },
                    onStopPlayback = viewModel::stopPlayback
                )
            }
        }

        composable(Routes.Settings) {
            SettingsScreen(
                sampleRate = sampleRate,
                onSampleRateChange = viewModel::setSampleRate,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

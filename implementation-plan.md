# Implementation Plan: Voice Notes (recorder demo app)

Purpose: a small self-created app that gives an unshrunk release APK to measure and optimize. It is not a polished product. Aim for about one day of work.

## 1. Scope

Four required features. Each has a pass check that goes into `required-features.md`.

| ID | Feature | Pass check |
|---|---|---|
| F1 | Record audio and save a file | A new note appears in the list after recording |
| F2 | Simple Material3 slider for seekbar (no waveform) | Seekbar/slider renders and works on detail screen; recording shows simple amplitude indicator |
| F3 | Notes list stored as JSON with Gson | Notes are still listed after an app restart |
| F4 | Settings (sample rate) in DataStore | The setting persists after an app restart |

Out of scope: cloud sync, transcription, editing audio, UI polish.

## 2. Stack

- Kotlin, Jetpack Compose, Material 3, single module `app`
- Recording via Android `MediaRecorder` API (no third-party recorder)
- Playback via `MediaPlayer` (Android framework)
- Gson, plain data classes, reflection (deliberate, this is the R8 risk to demonstrate)
- Preferences DataStore
- `material-icons-extended` (realistic dex and resource weight)
- minSdk 26, current stable compileSdk and targetSdk

## 3. Package structure

```
com.example.voicenotes
  MainActivity.kt
  ui/
    NotesListScreen.kt
    RecordScreen.kt
    DetailScreen.kt
    SettingsScreen.kt
    Navigation.kt
  data/
    Note.kt
    NotesRepository.kt
    SettingsStore.kt
  audio/
    RecorderController.kt  // wraps MediaRecorder
    Player.kt/AudioPlayer.kt
```

## 4. Build steps

### Step 1. Project setup
1. Empty Compose Activity project.
2. Add dependencies to version catalog/build.gradle.kts (no raw-audio-recorder, no waveform).
3. Release build with signing config as needed.
4. Create keystore if needed.

### Step 2. Data layer
1. `Note` data class.
2. `NotesRepository` with Gson + TypeToken.
3. `SettingsStore` with DataStore.

### Step 3. Audio layer
1. Request `RECORD_AUDIO` at runtime.
2. `RecorderController` using `MediaRecorder` (MPEG_4/AAC), save to `filesDir/recordings/`.
3. Simple amplitude feedback during recording.
4. `AudioPlayer` for playback with seek support.

### Step 4. Screens
1. Notes list
2. Record with timer and simple amplitude indicator
3. Detail with simple Material3 slider for seek/playback (no waveform)
4. Settings for sample rate

### Step 5. Build and verify
1. Build release and verify F1-F4.

## 5. Notes
- Removed third-party libraries `rawaudiorecorder` and `waveform-android`. Using Android framework APIs only.
- Replaced waveform displays with a simple Material3 `Slider` for playback seek and a `LinearProgressIndicator` for recording amplitude.

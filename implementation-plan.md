# Implementation Plan: Voice Notes (recorder demo app)

Purpose: a small self-created app that gives an unshrunk release APK to measure and optimize. It is not a polished product. Aim for about one day of work.

This plan assumes "recorder app" means the Voice Notes demo app used for the APK size task. Check each library's README for its real API. This plan does not assume method names.

## 1. Scope

Four required features. Each has a pass check that goes into `required-features.md`.

| ID | Feature | Pass check |
|---|---|---|
| F1 | Record audio and save a file | A new note appears in the list after recording |
| F2 | Waveform on the record and detail screens | Waveform renders while recording and during playback |
| F3 | Notes list stored as JSON with Gson | Notes are still listed after an app restart |
| F4 | Settings (sample rate or theme) in DataStore | The setting persists after an app restart |

Out of scope: cloud sync, transcription, editing audio, UI polish.

## 2. Stack

- Kotlin, Jetpack Compose, Material 3, single module `app`
- RawAudioRecorder (recording) and Waveform (waveform view), your own libraries
- Gson, plain data classes, reflection (deliberate, this is the R8 risk to demonstrate)
- Preferences DataStore
- `material-icons-extended` (realistic dex and resource weight)
- minSdk 26, current stable compileSdk and targetSdk

Before adding each dependency, check that its license allows commercial use. Note the license of each in a short list for your records.

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
    Note.kt              // data class: id, title, durationMs, createdAt, filePath
    NotesRepository.kt   // reads/writes notes.json with Gson
    SettingsStore.kt     // DataStore wrapper
  audio/
    RecorderController.kt  // wraps RawAudioRecorder
    Player.kt              // playback (MediaPlayer is enough)
```

## 4. Build steps

### Step 1. Project setup
1. Create an Empty Compose Activity project in Android Studio.
2. Add dependencies to the version catalog or `build.gradle.kts`.
3. Set the release build type to the deliberate baseline:
   ```kotlin
   release {
       isMinifyEnabled = false
       isShrinkResources = false
       signingConfig = signingConfigs.getByName("release")
   }
   ```
4. Create your own keystore (`keytool -genkeypair ...`) and add a `release` signing config. Keep the keystore out of version control. Do not use a personal name in the certificate fields. Use something neutral like `CN=Demo`.

### Step 2. Data layer
1. `Note` data class with simple fields.
2. `NotesRepository`: load and save a `List<Note>` to `filesDir/notes.json` using Gson and `TypeToken`. Keep this reflection-based. Do not add `@SerializedName` or any Gson adapters. This is what R8 will break later.
3. `SettingsStore`: DataStore preferences with one key (sample rate or dark theme).

### Step 3. Audio layer
1. Request `RECORD_AUDIO` at runtime. Add it to the manifest.
2. `RecorderController`: start, stop, return the saved file path and duration. Save under `filesDir/recordings/`.
3. Feed the live amplitude or samples to the Waveform view if the library supports it.
4. `Player`: play a saved file and report position.

### Step 4. Screens
1. **Notes list:** `LazyColumn` of notes, a record button, and a settings icon.
2. **Record:** timer, live waveform, stop and save.
3. **Detail:** playback controls, playback waveform, title and duration.
4. **Settings:** one toggle or selector bound to DataStore.
5. Simple navigation between the four screens. Use at least a few icons from `material-icons-extended`.

### Step 5. Add realistic size weight (clearly unused)
Generate the images yourself so there are no rights issues. Install Pillow (`pip install pillow`), then:

```python
import os
from PIL import Image

out = "app/src/main/res/drawable-nodpi"
os.makedirs(out, exist_ok=True)
for i in range(4):
    img = Image.frombytes("RGB", (1024, 1024), os.urandom(1024 * 1024 * 3))
    img.save(f"{out}/unused_bg_{i}.png")
```

Rules:
- Do not reference these files from code or XML. Resource shrinking can only remove what is unreferenced.
- Add a few unused strings to `strings.xml`.
- Optionally add one library that ships a native `.so` if you want to test an ABI filter. If not, drop the ABI candidate from the plan and the metadata.

### Step 6. Build and run the baseline
1. `./gradlew assembleRelease`
2. Install it: `adb install -r app/build/outputs/apk/release/app-release.apk`
3. Run F1 to F4 on this release build. They must all pass before you go further.
4. Copy the APK to `baseline.apk` outside the build folder so a later build can't overwrite it.
5. Open it in Build > Analyze APK. Confirm there is a clear difference between dex, res and (if used) lib, so the candidates will show visible changes. If the baseline is too small, add more icons or images.

## 5. Check the R8 risk is real (rehearsal only, not recorded)

1. Temporarily set `isMinifyEnabled = true` with only the default rules.
2. Build, install, and open the notes list after saving a note.
3. Confirm it fails or loses data (for example null fields or an empty list after restart).
4. Find the cause in logcat and `missing_rules.txt`, and note the keep rule that fixes it.
5. Revert to the baseline settings. On camera you should only repeat the fix, not discover it.

If nothing breaks, make the Gson usage more reflection-heavy (nested data class, generic list via `TypeToken`) until it does.

## 6. Files to prepare before recording

| File | Notes |
|---|---|
| Voice Notes project (zip) | Input 1. Exclude `build/` and the keystore |
| `required-features.md` | Input 2. Pass checks only, no analysis |
| `baseline.apk` | Input or artifact, as you decide for Deliverables.zip |
| Opening Goal doc | Word or Google Docs, Goal text only |

## 7. Pre-recording checklist

- [ ] F1 to F4 pass on the baseline release APK
- [ ] No personal name, email or git account in the project, keystore, `local.properties` or git config
- [ ] Android Studio recent projects list is clean
- [ ] Gradle sync finished, project open, Terminal open in the project folder
- [ ] Candidate builds rehearsed once, build times noted
- [ ] Unused PNGs are really unreferenced
- [ ] Licenses of dependencies checked

## 8. Estimated effort

| Part | Time |
|---|---|
| Setup and dependencies | 1 h |
| Data and settings layer | 1.5 h |
| Audio and waveform | 2 h |
| Screens and navigation | 2 h |
| Size weight, signing, baseline build | 1 h |
| R8 rehearsal | 1 h |

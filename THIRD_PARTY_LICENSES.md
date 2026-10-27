# Third-party dependencies and licenses

This demo app is intended for internal APK-size experiments. Review these notes before distributing it.

| Dependency | Version | Declared license | Notes |
|---|---|---|---|
| AndroidX Core, Activity Compose, Lifecycle, Compose UI, Material 3 | Per version catalog / Compose BOM | Apache License 2.0 | Commercial use permitted. |
| AndroidX Material Icons Extended | 1.7.8 | Apache License 2.0 | Commercial use permitted. |
| AndroidX DataStore Preferences | 1.3.0 | Apache License 2.0 | Commercial use permitted. |
| Gson | 2.13.2 | Apache License 2.0 | Commercial use permitted. |
| Kotlin standard library and kotlinx.coroutines | 2.2.x / 1.10.2 | Apache License 2.0 | Commercial use permitted. |
| RawAudioRecorder | 0.0.7 | Conflicting metadata: MIT license text in the v0.0.7 repository, Apache-2.0 in GitHub metadata, GPL-3.0 in the Maven POM | Treat the most restrictive declared metadata (GPL-3.0) as applicable until the project owner confirms otherwise. |
| Waveform | 0.0.6 | GPL-3.0 in the Maven POM; no license file found at the v0.0.6 tag | GPL-3.0 permits commercial use but carries source-disclosure and copyleft obligations. |
| Karya UI | 0.0.6 | GPL-3.0 in the Maven POM | Transitive dependency of Waveform. Same obligations. |

The app project itself is a demo. If either Karya library is redistributed, confirm the intended license with its author and comply with the applicable terms.

# SMS Alarm Trigger - Native Android App

Target SDK: 35 (Android 15)
Min SDK: 26 (Android 8.0)
Architecture: Kotlin, Jetpack Compose Material 3, Preferences DataStore, BroadcastReceiver, Foreground Service.

Open this directory directly in Android Studio Ladybug (2024.2+) or newer.

## Building

- **Android Studio**: open the folder and let it sync — the Gradle Wrapper is committed, so no extra setup is needed.
- **Command line**: run `./gradlew assembleDebug` (or `gradlew.bat assembleDebug` on Windows). The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
- **CI**: push to GitHub and the included workflow (`.github/workflows/build-apk.yml`) builds the debug APK and uploads it as a workflow artifact.

# Wrap

Job and payment tracker for freelancers who work on film sets and on-site gigs:
upcoming work, finished work, payments received and money still owed.

Android first (Kotlin + Jetpack Compose); iOS (SwiftUI) later on top of the same
Kotlin Multiplatform `shared` module.

## Project layout

| Module        | What lives there                                                                 |
|---------------|----------------------------------------------------------------------------------|
| `shared/`     | Kotlin Multiplatform (`commonMain`): models, database, money rules, repositories. All business logic. |
| `androidApp/` | Jetpack Compose UI, Material 3 with dynamic color. Never calculates money.       |

iOS targets (`iosArm64`, `iosSimulatorArm64`) are only registered when building on macOS.

## Requirements

- JDK 17 or newer (built and tested with JDK 25). `JAVA_HOME` must point at it.
- Android SDK with platform 37 installed.
- `local.properties` in the project root (not committed):

  ```properties
  sdk.dir=C:/Users/<you>/AppData/Local/Android/Sdk
  ```

- `adb` on `PATH` (`<sdk>\platform-tools`) for install / logcat.

## Commands (PowerShell)

```powershell
# Build
.\gradlew.bat assembleDebug

# Install and launch on a running device / emulator
.\gradlew.bat installDebug
adb shell am start -n com.abaan.wrap/.MainActivity

# Tests
.\gradlew.bat :shared:allTests
.\gradlew.bat test

# Watch for crashes (Compose previews don't work in VS Code; verify on device)
adb logcat AndroidRuntime:E com.abaan.wrap:V *:S

# Start the emulator
& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd Medium_Phone
```

## VS Code

`.vscode/tasks.json` provides **Build**, **Install & Run**, **Test** and **Logcat (crashes)**
(`Ctrl+Shift+P` → *Tasks: Run Task*). `Ctrl+Shift+B` runs Build.

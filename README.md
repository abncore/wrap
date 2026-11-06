# Wrap

Job and payment tracker for freelancers who work on film sets and on-site gigs:
upcoming work, finished work, payments received and money still owed.

Android first (Kotlin + Jetpack Compose); iOS (SwiftUI) later on top of the same
Kotlin Multiplatform `shared` module.

## Features

- **Home**: what you're owed, received this month, overdue jobs, today's job with
  Clock in / Clock out, and your next booking.
- **Jobs**: Upcoming / Unpaid / Paid tabs, filter by client and month, overdue in red.
- **Job detail**: full money breakdown (base, overtime, kit, expenses, paid, owed),
  time logs, payment history, Add payment, and **Chase** (opens WhatsApp with a polite
  reminder of the amount owed).
- **Money**: earned all time (payments received), spent and kept after spending, a
  6-month earned vs spent chart, spending by category (this month / year / all time) and a
  spending log for fuel, kit, food, permits and other work costs.
- **Monthly budget**: progress bar with what's left per day for the rest of the month, warning
  colours on Money and Home, and a notification the first time spending reaches 80% and again
  when it goes over (toggle in Settings).
- **Clients** with total owed each; **Settings** (gear on Home) for default rate, day length,
  overtime multiplier, payment terms, currency and reminders.
- **Reminders** at 8 PM the night before each job (WorkManager).

### Money rules (in `shared/`, unit tested)

| Rate type | Base pay |
|-----------|----------|
| Day       | rate × days |
| Half day  | rate ÷ 2 × days |
| Hourly    | logged hours × rate (planned day length until time is logged) |
| Fixed     | rate |

Overtime (day rate only) = hours beyond the day length on each day × (rate ÷ day length) ×
OT multiplier. Total = base + overtime + kit rental + expenses; owed = total − payments.
Status moves UPCOMING → DONE_UNPAID → PART_PAID → PAID automatically; OVERDUE is shown
when money is owed after the due date (end date + payment terms by default).

"Earned" means money actually received. Spending is your own cost of working and is separate
from a job's *expenses*, which are billed to the client.

### Database upgrades

Schema changes ship as SQLDelight migrations (`shared/src/commonMain/sqldelight/migrations/`),
so installing a new build over an old one keeps all data. `MigrationTest` checks each upgrade.

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

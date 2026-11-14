# Wrap

Job and payment tracker for freelancers who work on film sets and on-site gigs:
upcoming work, finished work, payments received and money still owed.

Android first (Kotlin + Jetpack Compose); iOS (SwiftUI) later on top of the same
Kotlin Multiplatform `shared` module.

## Features

- **Home**: what you're owed, received this month, overdue jobs, today's job with
  Clock in / Clock out, and your next booking.
- **Jobs**: Upcoming / Unpaid / Paid tabs, filter by client and month, overdue in red.
- **Job detail**: full money breakdown (base, overtime, kit, expenses, billed spending, VAT,
  paid, owed), time logs, payment history, Add payment, and **Chase** (opens WhatsApp with a
  polite reminder of the amount owed; each chase is logged).
- **Call sheet**: unit base and parking (address or pasted maps link) with Directions, crew
  contacts with call and WhatsApp buttons, and a reminder before call time on each job day
  (how long before is set in Settings).
- **Invoices**: an A4 PDF from any job, shared through WhatsApp, email or Drive. Your name,
  bank details, TRN and invoice numbering live in Settings > Your details. A job keeps its
  invoice number. Jobs can add 5% VAT (on by default when you're VAT registered).
- **Chase reminders**: each morning, overdue jobs (3+ days past due) that haven't been chased
  or reminded about for a week get a notification.
- **Calendar**: month view of bookings with double-booked and free days; optionally copies
  every job into the phone's calendar and keeps it up to date.
- **Money**: earned all time (payments received), spent and kept after spending, a
  6-month earned vs spent chart, spending by category (this month / year / all time) and a
  spending log for fuel, kit, food, permits and other work costs. Spending can have a receipt
  photo, be **billed to a job** (added to what the client owes) and **repeat monthly** (phone,
  software, visa; missed months are filled in).
- **Reports** (its own tab): a **year report** (received, spent, kept, VAT, income by client,
  spending by category, month by month, CSV export for an accountant); **client performance**
  (income share, days worked, rates, how many days each client takes to pay and how often on
  time, with a page per client); **who owes you**
  (owed money grouped as not due, 1-30, 31-60, 61-90 and 90+ days late, per client and job);
  and **work and rates** (days, hours, overtime, average day rate, earnings per day, by role,
  last 12 months).
- **Tutorial**: a swipe-through tour on first launch, reopened from Settings > How to use Wrap.
- **Monthly budget**: progress bar with what's left per day for the rest of the month, warning
  colours on Money and Home, and a notification the first time spending reaches 80% and again
  when it goes over (toggle in Settings).
- **Clients** with total owed each; **Settings** (gear on Home) for default rate, day length,
  overtime multiplier, payment terms, currency and reminders.
- **Reminders** at 8 PM the night before each job (WorkManager).
- **Home-screen widget**: owed total, today's or the next job, and Clock in / Clock out.
- **App lock** with fingerprint, face or screen lock (asks again after 30 s in the background).
- **Backup and restore**: a .zip with all data (`wrap.json`) and receipt photos, saved wherever
  you choose (Google Drive, Downloads...). Restoring replaces everything, in one transaction.
  Android's own app backup to Google also covers the database.

### Money rules (in `shared/`, unit tested)

| Rate type | Base pay |
|-----------|----------|
| Day       | rate × days |
| Half day  | rate ÷ 2 × days |
| Hourly    | logged hours × rate (planned day length until time is logged) |
| Fixed     | rate |

Overtime (day rate only) = hours beyond the day length on each day × (rate ÷ day length) ×
OT multiplier. Subtotal = base + overtime + kit rental + expenses + spending billed to the
job; total = subtotal + VAT at the job's rate; owed = total − payments.
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

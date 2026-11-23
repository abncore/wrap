# Changelog

All notable changes to Wrap are listed here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow
[Semantic Versioning](https://semver.org/). APKs for every version are on the
[Releases](../../releases) page.

## [Unreleased]

## [2.0.0] - 2026-11-23

Wrap 2 is a new design and the first version for iPhone.

### Added
- **iPhone app.** The same screens and money rules as Android, built from the same code with
  Compose Multiplatform. It follows iOS conventions: a tab bar, large titles, "‹ Back"
  navigation, the iOS switch, segmented controls, grouped Settings, Apple's system font and
  colours, and a light and dark look that follows the phone. Install the `.ipa` with Sideloadly
  or AltStore (see the README). Receipt photos, PDF invoices, backups, calendar sync, app lock
  and reminders are Android-only for now and come to iPhone next.

### Changed
- The screens moved into a shared `sharedUi` module so Android and iOS use the same UI code.
- New look: near-black and warm off-white with green for money paid and amber for money owed,
  plus a matching light theme that follows the phone's setting. Text uses the Inter typeface,
  and cards and status pills are rounder. Wrap no longer takes its colours from the wallpaper,
  so it looks the same on every phone (the Night shoot theme is unchanged).
- Home and Money open with a Paid / Owed card: both totals side by side, with a green and amber
  bar showing how much of your billed work has been paid. Home's stat tiles are quieter, with a
  coloured dot and only the amount turning red or amber when something needs attention.
- New app icon: an open ring with a green dot.
- Large amounts on Home's small tiles shrink to fit instead of being cut off.

## [0.5.0] - 2026-11-14

### Added
- Easter eggs in the credits line at the bottom of Settings: tap for a blooper reel of your
  own stats, long-press for an end-credits roll of your work, and tap ten times for the
  hidden Night shoot theme.

## [0.4.0] - 2026-11-14

### Added
- **Reports** tab in the bottom bar with:
  - Client performance: income share, days worked, rates, days to pay and on-time rate, with
    a page per client.
  - Who owes you: owed money grouped as not due, 1-30, 31-60, 61-90 and 90+ days late.
  - Work and rates: days, hours, overtime, average day rate and earnings per day, by role.
- Swipe-through tutorial on first launch, reopened from Settings > How to use Wrap.
- Credits at the bottom of Settings.

### Changed
- The year report moved into the Reports tab.

## [0.3.0] - 2026-11-07

### Added
- Chase log, and weekly reminders for overdue payments.
- Invoice PDFs with your details (name, bank, TRN, numbering) and VAT per job.
- Year report with CSV export for your accountant.
- Calendar view with double-booked and free days, and optional sync to the phone's calendar.
- Home-screen widget with the owed total, today's or the next job, and Clock in / Clock out.
- App lock with fingerprint, face or screen lock.
- Backup and restore to a .zip (all data and receipt photos).

## [0.2.0] - 2026-11-06

### Added
- Lifetime earnings, spending tracker and a 6-month earned vs spent chart.
- Monthly spending budget with alerts at 80% and when it goes over.
- Recurring monthly costs.
- Receipt photos, and spending billed to a job.
- Call sheet: unit base, parking, crew contacts and a reminder before call time.

## [0.1.0] - 2026-11-06

First release.

### Added
- Clients, with the total each one owes.
- Jobs with day, half-day, hourly and fixed rates, overtime and kit rental.
- Upcoming / Unpaid / Paid tabs, with job status updated automatically.
- Clock in / Clock out with time logs.
- Payments, including partial payments, with overdue tracking.
- Home dashboard with live numbers.
- Chase late payers on WhatsApp, and a reminder the night before each job.
- Settings for default rate, day length, overtime, payment terms and currency.

### Fixed
- Clocking in on finished jobs, and other issues found while testing on a device.

[Unreleased]: ../../compare/v2.0.0...HEAD
[2.0.0]: ../../compare/v0.5.0...v2.0.0
[0.5.0]: ../../compare/v0.4.0...v0.5.0
[0.4.0]: ../../compare/v0.3.0...v0.4.0
[0.3.0]: ../../compare/v0.2.0...v0.3.0
[0.2.0]: ../../compare/v0.1.0...v0.2.0
[0.1.0]: ../../releases/tag/v0.1.0

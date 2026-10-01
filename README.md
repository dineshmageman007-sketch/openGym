# openGym (forked & refined by mageman007)

Android build of [openGym](https://github.com/DuarteSantos8/openGym) (AGPL v3) — a personal gym & body weight tracker. All data stays on your phone.
Exercise data: hasaneyldrm/exercises-dataset (CC).

## Changes in this fork
- Removed "Self-host openGym" from Settings
- Removed the workout day reminder option
- Export backup now works: saves a `.json` of all data (routines, workout history, body weight, settings, custom exercises...) via the system "Save as" picker. Import backup restores it.
- New credits line and app icon

## Build the APK
Push to GitHub, open **Actions → Build APK → Run workflow**, then download `openGym-apk` from the run's Artifacts.

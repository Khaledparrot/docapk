# idwaZon — Android app

This folder turns the GutCare web app into a real Android app with **native
notifications that work when the app is closed**.

You do not need Android Studio. GitHub builds the APK for you in the cloud.

## One-time setup (about 10 minutes)

1. Create a new **public** repository on github.com, e.g. `gutcare-app`.
2. Upload **everything in this folder** to the repository root
   (GitHub web page → "uploading an existing file" → drag all files and folders).
   Keep the folder structure: `www/`, `android/`, `.github/` must stay as they are.
3. Open the **Actions** tab. If GitHub asks, click
   "I understand my workflows, go ahead and enable them".
4. Click **Build GutCare APK** in the left list → **Run workflow** → **Run workflow**.
5. Wait 6–12 minutes. The run turns green.
6. Open the finished run, scroll to **Artifacts**, download **GutCare-APK**
   (a .zip). Unzip it — inside is `app-debug.apk`.

## Install it on the phone

1. Send `app-debug.apk` to the phone (Telegram to yourself, USB, Google Drive).
2. Tap it. Android will say the file is from an unknown source —
   allow "Install unknown apps" for the app you opened it from.
3. Install → open. The GutCare icon appears in your app drawer.

## How the reminders work

The app ships a small native plugin (`GutAlarmPlugin` + `GutAlarmReceiver`) that
schedules exact alarms with Android's AlarmManager and posts an **insistent**
notification: the sound repeats until you press Stop on the notification or tap
it. Three modes, chosen in More -> Reminders -> Alarm style:

- **Until stopped** - one insistent notification, rings until stopped
- **10 beeps** - ten notifications four seconds apart
- **Silent** - quiet channel, no sound

The schedule is saved on the device and restored after a reboot.

## First run

- Allow notifications when asked. The app schedules all ten daily reminders
  with Android itself, so they ring even when GutCare is closed.
- Settings inside the app work exactly as before (AI key, language, export).

## Updating later

Replace `www/index.html` in the repository with a newer GutCare.html,
then run the workflow again and reinstall the new APK over the old one.
Your data stays: it lives in the app's storage.

## If the build fails

Open the red run, click the failed step, and copy the last ~20 lines.
Nothing is lost — you can re-run as many times as you like.

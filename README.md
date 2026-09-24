# Nightmare Reels Blocker (Android)

Detects when Instagram's **Reels** tab is open and slams a full-screen
horror overlay over it — your images, a heartbeat, a scream and a
laugh — until you tap **ESCAPE**. Toggle switch in the app turns it on
or off.

Uses an **Accessibility Service** to read Instagram's on-screen UI
(never anything else) and detect Reels-specific view IDs, then draws
the overlay directly as an accessibility overlay window — no separate
"draw over other apps" permission needed.

## Why Android, not iOS

Apple's sandboxing means no app — including ones using Screen Time's
Family Controls API — can see what's happening *inside* another app.
iOS can only block or time-limit the entire Instagram app, not a
feature within it. Detecting and blocking just Reels is only possible
on Android, via Accessibility Services. See the extension version's
notes if you want a "block all of Instagram" option for iOS instead.

## 1. Get a ready-to-install APK — no Android Studio needed

This project includes a GitHub Actions workflow that builds the APK
for you automatically, on GitHub's own servers.

1. Go to [github.com/new](https://github.com/new), create a new repo
   (public or private, doesn't matter), don't add a README.
2. On the new repo's page, click **uploading an existing file**, then
   drag in the entire contents of this `NightmareReelsBlocker` folder
   (all files and subfolders, including the hidden `.github` folder —
   most file managers hide dot-folders, so if you don't see `.github`
   in your file browser, turn on "show hidden files" first).
3. Commit the upload.
4. Click the **Actions** tab at the top of the repo → you'll see a
   "Build APK" run start automatically (takes 3–5 minutes).
5. Once it finishes (green checkmark), open that run → scroll to
   **Artifacts** at the bottom → download
   **NightmareReelsBlocker-debug-apk** (a zip containing the `.apk`).
6. Unzip it on your phone (or transfer the `.apk` to your phone), tap
   it to install. Android will warn about installing from an unknown
   source the first time — that's expected for a sideloaded app; allow it.

That's it — no Android Studio, no local build tools, just the browser.

## 2. Build it yourself in Android Studio instead (optional)

1. Install [Android Studio](https://developer.android.com/studio) (free).
2. **File → Open** → select this `NightmareReelsBlocker` folder.
3. Let Gradle sync (first sync downloads dependencies — needs internet).
4. Plug in your Android phone (with USB debugging on) or start an emulator.
5. Click **Run ▶**.

No Apple-style code signing or store review needed to run it on your
own device.

## 2. Grant permissions (required, one-time, manual)

Android does not allow an app to silently enable Accessibility Service
access — you must do it by hand:

1. Open the app → tap **Grant Accessibility Access**.
2. In Android's Accessibility settings, find **Nightmare Reels
   Blocker** → turn it on → confirm the warning dialog.
3. Return to the app; "Accessibility access" should now read GRANTED.

## 3. Important: this will need maintenance

Instagram doesn't publish a stable "you're in Reels" flag, and it
changes its internal view structure periodically as it ships updates.
Detection works by matching resource-ID fragments and content
descriptions that have been observed in Instagram's Reels UI — a
best-effort approach, the same one used by other on-device app
blockers (Opal, AppBlock, etc.).

If detection stops working after an Instagram update:

1. Open Instagram, go to Reels.
2. Run `adb shell uiautomator dump` then `adb pull /sdcard/window_dump.xml`
   (with your phone connected and USB debugging on), or use Android
   Studio's **Layout Inspector** while Instagram is running.
3. Look at the dumped view hierarchy for `resource-id` values that
   only appear on the Reels screen.
4. Add them to `REEL_RESOURCE_ID_FRAGMENTS` in
   `ReelsAccessibilityService.kt` (top of the file).

## 4. Customize

- **Images**: replace `app/src/main/res/drawable/scare1.jpg`,
  `scare2.jpg`, `scare3.png` with your own (same filenames, or add
  more and list them in `scareImages` in `ReelsAccessibilityService.kt`).
- **Sounds**: replace `app/src/main/res/raw/scream.mp3` and
  `laugh.mp3`. `heartbeat.mp3` is a short synthesized double-thump
  that loops — swap it for your own if you'd rather.
- **Messages**: edit the `messages` list in
  `ReelsAccessibilityService.kt`.

## 5. A note on Google Play

Google Play's policy restricts Accessibility Service usage to apps
whose core function is genuinely accessibility-related; a Reels
blocker built this way is very unlikely to be approved for the Play
Store and would need to be sideloaded (installed directly via Android
Studio or an APK) rather than published. This doesn't affect running
it on your own phone.

## Project structure

```
app/src/main/java/com/nightmareblocker/reels/
  MainActivity.kt                 — home screen: toggle, permission setup, scare counter
  ReelsAccessibilityService.kt    — Reels detection + horror overlay + sounds
  Prefs.kt                        — stores toggle state and scare count
app/src/main/res/
  layout/activity_main.xml        — home screen UI
  layout/overlay_horror.xml       — the full-screen jumpscare overlay
  drawable/scare1.jpg, scare2.jpg, scare3.png  — your horror images
  raw/scream.mp3, laugh.mp3, heartbeat.mp3     — sound effects
  xml/accessibility_service_config.xml         — service config (Instagram only)
```

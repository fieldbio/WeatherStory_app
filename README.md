# Weather Stories — landscape Android prototype

Source project, not a compiled APK. Created for Paolo Segre.

## Features
- Landscape locked (`sensorLandscape`): either landscape direction, never portrait on devices that honor the orientation request.
- ZIP saved locally; no GPS permission or login.
- ZIP coordinates from Zippopotam.us, responsible forecast office from NWS /points.
- Left/right swipe or Previous/Next buttons; whole-image fit; double-tap for pinch-zoom dialog.
- Refresh button and cached graphics on network failure. Cached graphics may be old.
- No analytics. Requests reveal IP address to services; entered ZIP is sent to Zippopotam.us and coordinates to NWS. Saved data is cleared by clearing app storage or uninstalling.

## Build in Android Studio
1. Install Android Studio with Android SDK 35 and JDK 17.
2. Extract this ZIP and open the project folder.
3. Select Gradle 8.9 as a local Gradle installation when prompted. This archive has no Gradle wrapper binary. Alternatively install Gradle 8.9 and run `gradle wrapper --gradle-version 8.9` in the folder, then reopen/sync.
4. Sync the project and run `gradle assembleDebug` (or the matching Android Studio build action).
5. Install `app/build/outputs/apk/debug/app-debug.apk` on your Android phone. Allow installation from that source only if you trust it.
Requires internet for initial build dependency downloads. Minimum Android 8.

## Build without a local Android toolchain
Create a GitHub repository and upload the CONTENTS of this project, retaining the hidden `.github/workflows/build-apk.yml` directory. Open Actions and run 'Build installable APK'. Download its APK artifact after a successful build. Extract the artifact ZIP and transfer/install the APK on your phone. The debug APK is signed automatically for personal testing, not a Play Store release. This project does not create a repository or send anything to GitHub by itself.

## Important limitations — read before relying on it
This prototype has NOT been compiled, run on a phone, or tested against the live GRB/LOT HTML. No APK is included. Web extraction is heuristic, not a verified API integration: it scans server-returned image elements, including hidden tabs, for office/story paths and filters small images. JavaScript-generated images will not be discovered. Some office pages may include unrelated large images or use unsupported markup. GRB and LOT extraction needs live validation and likely selector refinement. No promise of nationwide coverage.
ZIP centroids are approximate; ZIPs crossing forecast-office boundaries may resolve imperfectly. Office mapping is saved per ZIP. An invalid or unsupported ZIP displays an error and can be changed.
Landscape requests may be overridden by Android large-screen or device policies. Target SDK 34 intentionally avoids newer large-screen orientation changes for this personal prototype; review SDK requirements before distribution.
The app caches only the last successful office's set. Full-resolution bitmaps are used (up to 20); exceptionally large sets can require memory optimization. Static bitmap decoding shows the first frame of animated graphics. Failed individual image downloads are skipped, so a partial set can be shown. Check the source website if completeness matters. The checked timestamp is a download time, NOT publication time. Not an emergency warning application.

## Test checklist
- Build successfully and inspect compiler output.
- Enter a Green Bay ZIP, verify every displayed graphic against https://www.weather.gov/grb/weatherstory.
- Change to a Chicago-area ZIP and verify every panel/tab against https://www.weather.gov/lot/weatherstory.
- Swipe in both directions, test single-panel state, double-tap zoom, and both landscape directions.
- Close/reopen and confirm saved ZIP. Test refresh in airplane mode with a populated cache.
- Test invalid ZIP, no stories, slow connection, partial download, and ZIP changes while loading.

## Sources
NWS API: https://www.weather.gov/documentation/services-web-api
ZIP lookup: https://api.zippopotam.us/us/54301
Weather Story examples: https://www.weather.gov/grb/weatherstory and https://www.weather.gov/lot/weatherstory

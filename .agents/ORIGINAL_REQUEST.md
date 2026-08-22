# Original User Request

## 2026-07-03T17:56:26Z

# Teamwork Project Prompt — Draft

> Status: Launched
> Goal: Multi-agent system is currently working on the task.

Implement advanced tracking and player features into the Animetail Android application (Kotlin/Jetpack Compose codebase), transforming it into a full-fledged tracking client with advanced media injection and custom player speed controls.

Working directory: `C:\platform-tools\Animetail`
Integrity mode: benchmark

## Requirements

### R1. Custom Speed Cycle
Modify the Animetail video player controls to use a custom playback speed cycle sequence (0.25, 0.5, 1.0, 1.25, 1.5, 1.75, 2.0) instead of the default hardcoded speeds.

### R2. Local Media Injector
Implement an Intent catcher in `MainActivity` that intercepts external video files shared to the app and injects them into Animetail's local library by symlinking them into the local anime directory structure.

### R3. Discover Tab (Multi-Tracker)
Build a new main navigation tab "Discover" that integrates API calls directly into the Animetail repository architecture for all supported trackers (MAL, AniList, Kitsu, MangaUpdates, Trakt) to pull live trending, top-rated, and seasonal anime/manga data.

### R4. Deep Library Sync (Profile View)
Create a dedicated "Profile" view that displays the user's tracking statistics, mean scores, and custom lists synced directly from their connected tracking accounts.

## Acceptance Criteria

### Player & Injection
- [ ] Agent-as-judge verification: Inspect `BottomLeftPlayerControls.kt` (or equivalent) to ensure the playback speed button logic cycles through the requested custom speed sequence.
- [ ] Agent-as-judge verification: Inspect `MainActivity.kt` and `AndroidManifest.xml` to verify an intent filter is registered for video files and the intent handling logic correctly creates a symlink in the local anime directory.

### Tracking UI
- [ ] Agent-as-judge verification: Inspect the UI codebase to confirm a "Discover" tab has been added to the main navigation.
- [ ] Agent-as-judge verification: Inspect the repository and network layers to confirm API calls for trending/top data are implemented for MAL, AniList, and Kitsu.
- [ ] Agent-as-judge verification: Confirm a "Profile" Compose view exists that binds to a ViewModel which fetches user tracking statistics.

## 2026-07-06T11:56:40Z

Status update request: What's the status on the Discover Tab (Multi-Tracker) implementation? Also, the user reported that 'all tracking, Gdrive is showing HTTPS 404 error cuz of API.' Did you encounter this 404 error or know what it refers to?

## 2026-07-06T12:11:51Z

The user requested: "change 'discover' to 'tracking '& its icon, with animiation in icon. B. in discover suppport all tracking sites which are suported by app."
Ensure the orchestrator renames the tab to "Tracking" instead of "Discover" and gives it an animated icon, in addition to supporting the tracking APIs. Also, the user confirmed that the tracking login gets stuck in a loop when authenticating via the browser, which is related to the 404 issue being diagnosed.

## 2026-07-06T12:22:22Z

Additional User Request for the Video Player (R1):
The user also requested: "option to change defult hold for 2x & hold + swipe pattern like in @Aniyomi-2X-".
This means that when holding on the screen (long press to speed up), if they drag/swipe left or right, it should cycle through the speed sequence being implemented in R1.
Reference implementation for this "hold + swipe" logic exists in the user's mod file: `C:\platform-tools\Aniyomi-2X-\app\src\main\java\com\dark\animetailv2\module\ModuleMain.kt` (ACTION_DOWN and ACTION_MOVE in the `dispatchTouchEvent` hook where they calculate `indexShift = (mainDelta / cachedSensitivity).toInt()`).
Ensure the player controls modifications incorporate this "hold + swipe" gesture.

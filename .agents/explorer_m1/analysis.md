# Codebase Analysis Report

## 1. Custom Playback Speed Cycle

### Target Code Location
- **File**: `app/src/main/java/eu/kanade/tachiyomi/ui/player/controls/BottomLeftPlayerControls.kt`
- **Lines**: 66–77

### Analysis
The playback speed button is rendered as a `ControlsButton` showing the current speed:
```kotlin
ControlsButton(
    text = stringResource(AYMR.strings.player_speed, playbackSpeed),
    onClick = {
        val speedList = listOf(0.25f, 0.5f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
        var idx = speedList.indices.minByOrNull { kotlin.math.abs(speedList[it] - playbackSpeed) } ?: 0
        idx = (idx + 1) % speedList.size
        val newSpeed = speedList[idx]
        onPlaybackSpeedChange(newSpeed)
        playerPreferences.playerSpeed().set(newSpeed)
    },
    onLongClick = { onOpenSheet(Sheets.PlaybackSpeed) },
)
```

### Cycling & Custom Sequence Replacement
- **How it cycles**: When clicked, the code defines a hardcoded list of speeds (`speedList`). It finds the index of the element closest to the current `playbackSpeed` using `minByOrNull`, increments the index modulo the list size (`(idx + 1) % speedList.size`), changes the playback speed, and saves the new speed to preferences.
- **Replacement**: To change the custom sequence, replace the hardcoded floats in `listOf(0.25f, 0.5f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)` at line 69.

---

## 2. Local Media Injector

### Target Code Locations
- **AndroidManifest**: `app/src/main/AndroidManifest.xml` (lines 127–135)
- **MainActivity**: `app/src/main/java/eu/kanade/tachiyomi/ui/main/MainActivity.kt`

### Analysis
The media injector receives shared external video files and links them into the app's local storage folder.

#### 1. Intent Filter (AndroidManifest.xml)
Declared under `MainActivity`:
```xml
<!-- Handle external video files -->
<intent-filter android:label="Add to Animetail Library">
    <action android:name="android.intent.action.VIEW" />
    <action android:name="android.intent.action.SEND" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:mimeType="video/*" />
    <data android:scheme="file" />
    <data android:scheme="content" />
</intent-filter>
```

#### 2. MainActivity.kt Injector Logic
- **Intent Interception**: Inside `handleIntentAction` (lines 669–685), the incoming intent is examined. If its type starts with `video/`, it extracts the `Uri` (using `EXTRA_STREAM` if action is `ACTION_SEND`, or `intent.data` otherwise), and sets `pendingVideoUri = videoUri`.
- **Dialog Confirmation**: In the Compose content block (lines 368–399), a confirmation dialog asks the user if they want to add the file. On "YES", `injectToAnimetail(context, videoUriToImport, filename)` is called.
- **Symlinking & Fallback (lines 890–934)**:
  ```kotlin
  private fun injectToAnimetail(context: Context, uri: Uri, filename: String) {
      val name = filename.substringBeforeLast(".")
      val path = uri.path ?: uri.toString()
      val localDir = "/storage/emulated/0/Animetail/local/anime/$name"
      
      try {
          Runtime.getRuntime().exec(arrayOf("su", "-c", "find /storage/emulated/0/Animetail/local/anime/ -type l -mtime +1 -delete && find /storage/emulated/0/Animetail/local/anime/ -type d -empty -delete")).waitFor()
      } catch (_: Exception) {}

      try {
          val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "mkdir -p \"$localDir\" && ln -sf \"$path\" \"$localDir/$filename\""))
          val exitCode = process.waitFor()
          if (exitCode == 0) {
              context.toast("Added to Library!")
          } else {
              copyFileNatively(context, uri, localDir, filename)
          }
      } catch (e: Exception) {
          copyFileNatively(context, uri, localDir, filename)
      }
  }
  ```
  - **Symlinking**: Tries to execute root commands (`su -c`) to create directories and create a symlink (`ln -sf`) from the video uri source to `local/anime/$name/$filename`.
  - **Cleanup**: Deletes symlinks older than 1 day and empty folders using `find ... -mtime +1 -delete`.
  - **Fallback Copy**: If executing root commands fails (e.g. non-rooted devices), it catches the exception and falls back to `copyFileNatively`, which copies the stream data byte-by-byte via `context.contentResolver.openInputStream(uri)`.

---

## 3. Tracker & GDrive API 404 / Auth Loop Issue Investigation

### Observations
1. **Official Base URLs**: The trackers use their standard, official base URLs. There are no mock servers or redirection configurations in the source code endpoints:
   - **AniList**: `https://graphql.anilist.co/`
   - **MyAnimeList**: `https://api.myanimelist.net/v2`
   - **Kitsu**: `https://kitsu.app/api/edge/`
   - **Trakt**: `https://api.trakt.tv`
   - **MangaUpdates**: `https://api.mangaupdates.com`
2. **Google Drive Endpoints**: `GoogleDriveSyncService.kt` uses Google APIs via the standard Java Client SDK (`com.google.api.services.drive`), pointing to default Google servers (`https://www.googleapis.com/`).
3. **Missing client_secrets.json**: The `client_secrets.json` file referenced in `GoogleDriveSyncService.kt` is completely missing from the assets folder. At runtime, attempting to load GDrive credentials will throw a `FileNotFoundException`.
4. **Invalid Google services Client ID**: In `app/google-services.json` line 18, the client ID has an accidental double space: `"86548812614-  ntn73vpjk2ndu8ckjr9e5mounuugfisn.apps.googleusercontent.com"`, which will invalidate OAuth requests.
5. **Deprecated Sync Host**: `SyncPreferences.kt` defaults the sync host to `https://sync.tachiyomi.org`. Since Tachiyomi shut down, this server is permanently offline and returns 404/resolution errors.
6. **OAuth Proxy Redirects**: The redirect URIs for MAL, AniList, and others originally went through `tachiyomi.org` callback pages which are offline now.

### Diagnostic of browser auth loop
When the browser launches the authorization URL, it successfully authenticates but attempts to redirect back using the registered redirect URI proxy. Since `tachiyomi.org` or other developer proxy domains are down (returning 404), the callback fails.
If the browser deep-link is somehow executed (or re-navigated) but the app fails to exchange the auth code for a token (due to the missing `client_secrets.json` or invalid client IDs), the app automatically resets the login state and throws a login failure. The user is redirected back to the settings page, which triggers a reload/re-authorization flow or prompts the user to log in again, creating a loop.

---

## 4. Discover Tab & Profile View Analysis

### Discover (Tracking) Tab (`DiscoverTab.kt`, `DiscoverScreenModel.kt`)
- **Structure**: `DiscoverTab.kt` provides a Compose view with two tabs: Anime and Manga. It triggers `DiscoverScreenModel` which calls AniList tracker's `getTrendingAnime(1)` and `getTrendingManga(1)`.
- **Identified Issue**:
  In `AnilistApi.kt`, `getTrendingAnime` and `getTrendingManga` perform requests using `authClient`. The `authClient` intercepts requests with `AnilistInterceptor`, which throws an exception if the user is not authenticated (`"Not authenticated with AniList"`).
  This causes the Discover Tab to crash or show a "Failed to load discovery feed" error for any user who is not logged in to AniList.
- **What needs to be implemented**:
  Modify `getTrendingAnime` and `getTrendingManga` in `AnilistApi.kt` to use the unauthenticated `client` rather than `authClient`, since AniList trending requests are public and do not require user authentication.

### Profile View (`ProfileStatsTab.kt`, `ProfileScreenModel.kt`)
- **Structure**: `ProfileStatsTab.kt` renders profile banner/avatar images and stats. `ProfileScreenModel` verifies if AniList is logged in (`trackerManager.aniList.isLoggedIn`) and queries `getProfileStats()`.
- **Identified Issue**:
  The Profile view is hardcoded for AniList stats only. It handles `ProfileState.SuccessAnilist(stats)` and displays statistics elements mapped strictly to AniList DTO fields (`stats.statistics.anime` / `stats.statistics.manga`).
  If a user uses other trackers (e.g. MyAnimeList, Trakt) and is not logged in to AniList, they see "Trackers Not Connected".
- **What needs to be implemented**:
  Abstract the profile stats fetching and UI to support multiple trackers (MyAnimeList, Trakt, Kitsu, etc.) depending on which services are logged in, rather than hardcoding it to AniList.

---

## 5. User Feedback Implementation Plan

### 1. Rename "Discover" to "Tracking" with Custom Animated Icon
- **Renaming**:
  - In `DiscoverTab.kt`, rename the `options.title` from `"Discover"` to `"Tracking"`.
  - In `NavStyle.kt` and `HomeScreen.kt`, update references to point to the renamed `TrackingTab` object and update localizations if needed.
- **Animated Icon**:
  - Replace `Icons.Outlined.Explore` with `ic_sync_24dp.xml` as the icon for the Tracking tab.
  - Implement a dynamic rotation animation inside `HomeScreen.kt` in `NavigationIconItem` using Compose's `rememberInfiniteTransition` when the selected tab is `TrackingTab`.
  - Example implementation for `NavigationIconItem`:
    ```kotlin
    if (TrackingTab::class.isInstance(tab)) {
        val isSelected = tabNavigator.current::class == tab::class
        val rotationAngle by if (isSelected) {
            val infiniteTransition = rememberInfiniteTransition()
            infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            remember { mutableStateOf(0f) }
        }
        Icon(
            painter = ImageVector.vectorResource(id = R.drawable.ic_sync_24dp),
            contentDescription = tab.options.title,
            modifier = Modifier.rotate(rotationAngle),
            tint = LocalContentColor.current
        )
    }
    ```

### 2. Support All Tracking Sites in "Tracking" Tab
- Modify `TrackingScreenModel` to load trending items from whichever tracking site is currently logged in (or query MAL/Kitsu/Trakt API for their public trending feeds if logged in).
- If no accounts are connected, fetch AniList public trending list using the unauthenticated OkHttpClient fallback.
- Abstract the `ProfileScreenModel` and `ProfileStatsTab` to read and render profile user stats from Kitsu, Trakt, or MyAnimeList by defining a unified `TrackerProfile` DTO interface.

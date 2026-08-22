# Project: Animetail Advanced Features & Sync Fix

## Architecture
- **App Module (`app`)**: Android application package containing UI views (Jetpack Compose), ViewModels, and main activities.
  - UI components: `eu.kanade.tachiyomi.ui`
  - Playback Speed: `eu.kanade.tachiyomi.ui.player.controls.BottomLeftPlayerControls.kt`
  - Intent catch / Local Media Injector: `MainActivity.kt` & `AndroidManifest.xml`
- **Data Module (`data`/`app/src/main/java/eu/kanade/tachiyomi/data`)**: Repository, sync, and network layers.
  - Sync Services (GDrive): `eu.kanade.tachiyomi.data.sync.service.GoogleDriveSyncService.kt`
  - Tracking Services (MAL, AniList, Kitsu, Trakt, MangaUpdates, etc.): `eu.kanade.tachiyomi.data.track`
- **Domain Module (`domain` or `eu.kanade.domain`)**: Core business rules and use cases.
- **Presentation Core Module (`presentation-core`)**: Shared Compose views.

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| 1 | M1: Exploration & 404/Login Loop Diagnosis | Investigate player controls, intent catcher, API 404, and browser login loop issues | none | DONE |
| 2 | M2: Custom Speed Cycle (R1) | Implement custom playback speed cycle sequence and hold+swipe in GestureHandler | M1 | IN_PROGRESS |
| 3 | M3: Local Media Injector (R2) | Intercept external video files shared to app and symlink them into local anime dir | M1 | IN_PROGRESS |
| 4 | M4: API 404 & Login Loop Fix | Resolve HTTPS 404 error affecting all tracking / GDrive APIs and fix OAuth browser login loop | M1 | IN_PROGRESS |
| 5 | M5: Tracking Tab UI & Multi-Tracker (R3) | Build Tracking tab main navigation with animated icon, supporting all tracking sites | M4 | IN_PROGRESS |
| 6 | M6: Profile View & Deep Sync (R4) | Create Profile view Compose screen showing user tracking stats and custom lists | M4 | IN_PROGRESS |
| 7 | M7: E2E and Audit Verification | Opaque-box E2E testing, Forensic Audit verification, and adversarial hardening | M2, M3, M5, M6 | PLANNED |

## Code Layout
- Custom Speed controls: `app/src/main/java/eu/kanade/tachiyomi/ui/player/controls/BottomLeftPlayerControls.kt`
- Intent catcher: `app/src/main/java/eu/kanade/tachiyomi/ui/main/MainActivity.kt` and `app/src/main/AndroidManifest.xml`
- Trackers: `app/src/main/java/eu/kanade/tachiyomi/data/track/` (e.g. `anilist/AnilistApi.kt`, `myanimelist/MyAnimeListApi.kt`, etc.)
- GDrive Sync: `app/src/main/java/eu/kanade/tachiyomi/data/sync/service/GoogleDriveSyncService.kt`
- Tracking Tab Screen (formerly Discover): `app/src/main/java/eu/kanade/tachiyomi/ui/discover/DiscoverTab.kt`
- Profile View Screen: `app/src/main/java/eu/kanade/tachiyomi/ui/stats/profile/ProfileStatsTab.kt`

## Interface Contracts
- **Local Media Injector**: Captures `ACTION_VIEW` or `ACTION_SEND` intents with mime-type `video/*` and creates a symlink under the local media directory structure.
- **Tracking Screen API Data Flow**: `TrackingScreenModel` -> Tracker Api Manager -> Network client -> Parser -> UI State list. Supports MAL, AniList, Kitsu, Trakt, MangaUpdates, Bangumi, Shikimori, Simkl, etc.
- **Profile View Sync Flow**: `ProfileScreenModel` -> Fetch Stats from enabled track accounts -> Aggregate statistics -> UI state.

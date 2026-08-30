# Project: Animetail Comprehensive Revamp

## Architecture
Animetail is an Android application built using Kotlin, Jetpack Compose, Voyager Navigation, Material 3 Expressive Theming, Kotlin Coroutines/Flows, SQLDelight/Room, and an embedded MPV/ExoPlayer media engine.

The comprehensive revamp is structured into modular layers:
1. **UI & Navigation Layer**:
   - `eu.kanade.tachiyomi.ui.library`: Unified `LibraryTab`, `LibraryMediaSwitcher` pill toggle, category paging, and item counters.
   - `eu.kanade.tachiyomi.ui.updates` & `eu.kanade.tachiyomi.ui.history`: `UpdatesTab`, `HistoriesTab`, `ListGroupHeader`, chronological date groups (Today, Yesterday, This Week, Last Month), resume progress indicators (`85% watched`, `Ch 42 page 15`), one-tap quick play/read button, and batch actions.
   - `eu.kanade.tachiyomi.ui.browse`: `BrowseTab`, `AnimeSourcesScreen`, `AnimeExtensionsScreen`, repository cards, and filter chips.
   - `eu.kanade.tachiyomi.ui.more`: `MoreTab`, `SettingsMainScreen`, `SettingsDataScreen` with cloud backup/sync chips.
   - `eu.kanade.presentation.theme`: `MaterialExpressiveTheme`, dynamic `CoverBasedTheme` with AndroidX Palette & MaterialKolor styles.
2. **Tracking & Discovery Layer**:
   - `eu.kanade.tachiyomi.data.track`: `TrackerManager` (15 trackers: AniList, MAL, Kitsu, Shikimori, Bangumi, Simkl, Trakt, MangaUpdates, etc.), `TrackLoginActivity` OAuth routing.
   - `eu.kanade.tachiyomi.ui.tracking`: `TrackingTab` with animated icon, `TrackingScreenModel`.
   - `eu.kanade.presentation.tracking`: `AiringScheduleView` with live countdown timers (`Ep X in Yh Zm`), `AnimiteMediaRow` / `AnimiteMediaCard` carousels, `MediaDetailsBottomSheet` with 1-tap "Watch/Read in Animetail" global search bridge, and authenticated "My Lists" with inline `+1` progress bumping.
3. **Player, Reader & Media Injection Layer**:
   - `eu.kanade.tachiyomi.ui.player`: `PlayerActivity`, `PlayerControls`, `BottomLeftPlayerControls`, `GestureHandler`, `SpeedPlayerUpdate` capsule speed pill, custom speed presets `[0.25..2.0]`, hold + swipe gestures, aspect ratio cycling, audio track selection, and subtitle styling.
   - `eu.kanade.tachiyomi.ui.reader`: `ReaderActivity`, dual-page mode, reading session timer, and background preloading.
   - `eu.kanade.tachiyomi.ui.main.MainActivity`: Local media injector with `ACTION_VIEW`/`ACTION_SEND` filters and symlink handling.
4. **Power Tools & Management Layer**:
   - `eu.kanade.tachiyomi.ui.stats`: Modernized Statistics screen with watch time vs chapters read, score distribution charts, genre radial charts, top studios/authors lists, and time range filters.
   - `eu.kanade.tachiyomi.ui.download`: Revamped Download Manager with segmented queue states (Downloading with speed & ETA, Paused, Completed, Failed/Retrying), Pause All / Resume All FABs, and drag-and-drop priority reordering.
   - `eu.kanade.tachiyomi.ui.storage`: Storage Inspector with multi-color visual breakdown bar, 1-tap smart cleaners (orphaned files, buffer cache, web cache), and per-category size ranking.
5. **Verification & Stability Layer**:
   - Gradle build compilation `:app:compileDebugKotlin` using JDK 17.
   - Reviewer, Challenger, and Forensic Auditor verification gates.

---

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | F1.1 Unified Library Tab | `LibraryMediaSwitcher` pill toggle, active item counts, category paging | M1 | ORIGINAL_REQUEST §R1 |
| 2 | F1.2 Updates & History Tab | Modernized date group cards, thumbnail badges, quick swipe actions, batch actions | M1 | ORIGINAL_REQUEST §R1 |
| 3 | F1.3 Browse & Extension Tab | Source cards with language pills, pin toggles, repository management, filter chips | M1 | ORIGINAL_REQUEST §R1 |
| 4 | F1.4 More & Settings Tab | Material 3 settings categories with iconography, dynamic switches, sync chips | M1 | ORIGINAL_REQUEST §R1 |
| 5 | F1.5 M3 Tokens & Fluid Motion | Dynamic cover-based color harmony, fade-through and shared-axis transitions | M1 | ORIGINAL_REQUEST §R1 |
| 6 | F2.1 Multi-Tracker Integration | Full multi-tracker hub (AniList, MAL, Kitsu, Shikimori, Bangumi, Simkl, Trakt) | M2 | ORIGINAL_REQUEST §R2 |
| 7 | F2.2 OAuth Robustness | Fix AniList token regex and MAL PKCE verifier persistence in `TrackLoginActivity` | M2 | ORIGINAL_REQUEST §R2 |
| 8 | F2.3 Airing Calendar & Countdown | Interactive schedule picker with live episode countdown (`Ep X in Yh Zm`) | M2 | ORIGINAL_REQUEST §R2 |
| 9 | F2.4 Discovery Carousels | Horizontal carousels (Trending, Popular, Top Rated, Upcoming) with rank badges | M2 | ORIGINAL_REQUEST §R2 |
| 10 | F2.5 Media Details Bottom Sheet | Hero backdrop, genre chips, studio badges, 1-tap global search bridge | M2 | ORIGINAL_REQUEST §R2 |
| 11 | F2.6 Authenticated My Lists | Personal Watching/Reading lists with inline `+1` progress bumping & score update | M2 | ORIGINAL_REQUEST §R2 |
| 12 | F2.7 Animated Tracking Icon | Tracking tab navigation with animated spinning sync icon | M2 | ORIGINAL_REQUEST §R2 |
| 13 | F3.1 Hold-to-2x Speed & Capsule Pill | Hold-to-speed gesture, `SpeedPlayerUpdate` capsule pill, cancellation auto-reset | M3 | ORIGINAL_REQUEST §R3 |
| 14 | F3.2 Custom Speed Cycle & Swipe | Click presets `[0.25..2.0]` and hold + swipe horizontal speed shift | M3 | ORIGINAL_REQUEST §R3 |
| 15 | F3.3 Aspect Ratio & Audio | Aspect ratio cycling (Crop, Fit, Stretch, 16:9, 18:9), audio switcher & subtitle dialogs | M3 | ORIGINAL_REQUEST §R3 |
| 16 | F3.4 Reader Enhancements | Webtoon/paged transitions, dual-page mode, reading timer, preloading | M3 | ORIGINAL_REQUEST §R3 |
| 17 | F3.5 Local Media Injector | External video intent interception and anime directory symlink/copy injection | M3 | ORIGINAL_REQUEST §R3 |
| 18 | F4.1 Statistics Tab Modernization | Watch time vs chapters read, score bar charts, genre radial charts, top studios/authors | M4 | ORIGINAL_REQUEST §Task 1 |
| 19 | F4.2 History Tab Enhancements | Chronological date groups, resume progress badges, 1-tap play/read, batch clear | M4 | ORIGINAL_REQUEST §Task 2 |
| 20 | F4.3 Download Manager Revamp | Segmented queue states (Downloading speed/ETA, Paused, Completed, Failed), FABs, drag-reorder | M4 | ORIGINAL_REQUEST §Task 3 |
| 21 | F4.4 Storage Tab & Smart Cleaners | Visual breakdown bar, 1-tap smart cleaners (orphaned, stream buffer, web cache), category inspector | M4 | ORIGINAL_REQUEST §Task 4 |
| 22 | F5.1 Clean Kotlin Compilation | Verify `./gradlew :app:compileDebugKotlin` compiles cleanly with 0 errors | M5 | ORIGINAL_REQUEST §R4 |
| 23 | F5.2 Forensic Integrity Audit | Multi-reviewer, challenger, and forensic auditor verification pass | M5 | ORIGINAL_REQUEST §R4 |

---

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| 1 | M1: Material 3 UI & Fluid Motion | Features F1.1 - F1.5 (Library, Updates/History, Browse/Extensions, Settings, Theme) | none | IN_PROGRESS |
| 2 | M2: Advanced Tracking Tab | Features F2.1 - F2.7 (OAuth fixes, Airing Calendar, Discovery, Details Sheet, My Lists) | M1 | PLANNED |
| 3 | M3: Player, Reader & Injector | Features F3.1 - F3.5 (Speed pill, hold+swipe, aspect ratios, subtitles, reader, injector) | M1 | PLANNED |
| 4 | M4: Power Tools (Stats, Downloads, Storage) | Features F4.1 - F4.4 (Stats charts, History enhancements, Download queue, Storage cleaners) | M1 | PLANNED |
| 5 | M5: Final Verification & Audit | Features F5.1 - F5.2 (Compile check, Reviewers, Challengers, Forensic Auditor) | M1, M2, M3, M4 | PLANNED |

---

## Interface Contracts

### 1. `LibraryTab` ↔ `LibraryToolbar` / `LibraryMediaSwitcher`
- `LibraryTab` state: `selectedMediaType: MutableState<LibraryMediaType>`
- `LibraryMediaSwitcher(selectedType: LibraryMediaType, onTypeSelected: (LibraryMediaType) -> Unit, animeCount: Int?, mangaCount: Int?)`
- `LibraryTabs(categories: List<Category>, selectedCategoryIndex: Int, onCategorySelected: (Int) -> Unit)`

### 2. `TrackingTab` ↔ `TrackingScreenModel` ↔ `MediaDetailsBottomSheet`
- `TrackingScreenModel`:
  - `state: StateFlow<TrackingScreenState>` (AiringSchedule, TrendingAnime, PopularAnime, TopRatedAnime, UpcomingAnime, AuthenticatedLists)
  - `fetchAiringSchedule(dayOfWeek: DayOfWeek)`
  - `updateTrackProgress(trackId: Long, newProgress: Double)`
- `MediaDetailsBottomSheet(media: TrackMediaItem, onSearchGlobal: (title: String, isAnime: Boolean) -> Unit, onDismiss: () -> Unit)`

### 3. `PlayerControls` ↔ `GestureHandler` ↔ `SpeedPlayerUpdate`
- `GestureHandler`:
  - `onLongPress`: triggers `setHoldSpeed(2.0f)` and displays `SpeedPlayerUpdate(speed = 2.0f)`
  - `detectHorizontalDragGestures`: adjusts `speed` dynamically based on drag offset
  - `onRelease` / `onCancel`: invokes `resetHoldSpeed()` immediately restoring base playback speed
- `BottomLeftPlayerControls`:
  - Speed click cycle sequence: `0.25f, 0.5f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f`

### 4. `StatsScreen` & `StorageScreen` & `DownloadManager`
- `StatsScreenModel`: Calculates watch time vs read chapters, rating spectrum distribution, genre shares, studio rankings.
- `StorageScreenModel`: Scans disk usage for anime, manga, video buffer cache, extension images, free storage, and executes smart cleaners.
- `DownloadScreenModel`: Provides segmented queue streams, speed/ETA calculations, pause/resume all commands, and priority reordering.

---

## Code Layout
- `app/src/main/java/eu/kanade/tachiyomi/ui/library/`: Library UI and media switcher
- `app/src/main/java/eu/kanade/presentation/library/`: Library Compose components
- `app/src/main/java/eu/kanade/tachiyomi/ui/updates/`: Updates UI
- `app/src/main/java/eu/kanade/tachiyomi/ui/history/`: History UI
- `app/src/main/java/eu/kanade/tachiyomi/ui/browse/`: Browse & extensions UI
- `app/src/main/java/eu/kanade/tachiyomi/ui/more/`: More & settings screens
- `app/src/main/java/eu/kanade/presentation/theme/`: Material 3 Expressive theme tokens
- `app/src/main/java/eu/kanade/tachiyomi/ui/tracking/`: Tracking tab & screen models
- `app/src/main/java/eu/kanade/presentation/tracking/`: Airing calendar, discovery rows, details sheet
- `app/src/main/java/eu/kanade/tachiyomi/data/track/`: Tracker managers, OAuth login, APIs
- `app/src/main/java/eu/kanade/tachiyomi/ui/player/`: Video player, controls, gestures, panels
- `app/src/main/java/eu/kanade/tachiyomi/ui/reader/`: Reader viewer, settings, loader
- `app/src/main/java/eu/kanade/tachiyomi/ui/main/`: MainActivity intent interceptor
- `app/src/main/java/eu/kanade/tachiyomi/ui/stats/`: Statistics charts & metrics
- `app/src/main/java/eu/kanade/tachiyomi/ui/download/`: Download manager segmented queue & priority
- `app/src/main/java/eu/kanade/presentation/more/settings/screen/data/storage/`: Storage breakdown & smart cleaners

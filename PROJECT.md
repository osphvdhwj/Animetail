# Project: DailyAL to Animetail Porting

## Architecture
- **Target Application**: Animetail (Aniyomi Plus) Android application (Kotlin Multiplatform / Jetpack Compose).
- **Subsystem**: Discover & Tracking (`eu.kanade.tachiyomi.ui.discover.*`) and AniList Tracking (`eu.kanade.tachiyomi.data.track.anilist.*`).
- **Navigation Integration**: Voyager `Tab` (`TrackingTab.kt`) registered in `HomeScreen.kt` (standalone tab) and `HomeTab.kt` (subtab 1 in merged Anime+Manga mode).
- **Data Flow**:
  - `AnilistApi.kt` communicates with `https://graphql.anilist.co/` via OkHttp with rate limiting (85 req/min ceiling).
  - `DiscoverCacheStore.kt` provides disk caching in `context.filesDir/discover_cache/` (24-hour TTL for completed media, 3-hour TTL for airing media).
  - `TrackingScreenModel.kt` (Voyager ScreenModel) exposes reactive `StateFlow` streams (`state`, `airingScheduleState`, `deepDetailsState`, `filterState`, `profileState`) to Compose UI.
  - Compose UI components in `DailyALDiscoverComponents.kt`, `DailyALMediaDetailsComponents.kt`, `AiringScheduleView.kt`, and `DailyALAdvancedFilterSheet.kt` render the UI adhering to Material 3 Expressive theming tokens, perfectly mirroring DailyAL's visual design.

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | DailyAL Data Models | `@Serializable` data models for deep media details, characters, voice actors, staff, relations, recommendations, reviews, and filter states (`DailyALMediaModels.kt`) | M1 | survey (DONE) |
| 2 | Deep Media Details GraphQL Query | Single round-trip GraphQL query in `AnilistApi.kt` retrieving characters, staff, relations, recommendations, reviews, trailer, and tags with safe-cast null handling | M1 | survey (DONE) |
| 3 | Advanced Filter GraphQL Query | Parameterized GraphQL query supporting multi-genre include/exclude, formats, statuses, seasons, years, and score ranges | M1 | survey (DONE) |
| 4 | Disk Caching Layer | Persistent JSON caching in `DiscoverCacheStore.kt` for deep media details and filter queries to respect rate limits | M1 | survey (DONE) |
| 5 | Discover Hub & Segmented Tabs | DailyAL segmented picker `[ Anime | Airing | Manga | Genres ]`, 32dp capsule switchers, and quick action chips | M2 | survey |
| 6 | Featured Hero Spotlight | 200dp hero banner with 22dp radius, 3-step vertical gradient, season tag, star score pill, and format badge | M2 | survey |
| 7 | DailyAL Media Card Composables | Media cards with 1:1.4 aspect ratio, 80dp vignette gradient overlay, dual progress bars (amber for release, blue/green for user watched), score pills (`8.75 | 1.2M`), and rank badges (Gold, Silver, Bronze) | M2 | survey |
| 8 | Discovery Rails & Headers | Horizontal discovery rails for Trending Now, All-Time Popular, Top Rated, and Seasonal charts with DailyAL section headers and "View All" actions | M2 | survey |
| 9 | Categories & Ranking Grid | 2-column adaptive flow cards (`AllRankingWidget`) with 12dp radius, 0.3 opacity background image, and 15sp title | M2 | survey |
| 10 | Interactive Airing Timetable | Day-by-day airing schedule with weekday selector row, day accordions with count e.g. `Monday (14)`, list tiles with 60x60 thumbnail + airing time pill | M3 | survey |
| 11 | Live Countdown Ticker | Algorithmic 1-second ticker calculating remaining time (`Ep X in Yd Zh Zm`) and 30-minute airing status window | M3 | survey |
| 12 | Current-Time Marker & Calendar Share | Divider card showing live current time with timezone offset, plus iCal / calendar share integration | M3 | survey |
| 13 | Advanced Filter Bottom Sheet | Modal M3 bottom sheet with multi-genre include/exclude chips, format pills, status toggles, season/year picker, and score slider | M4 | survey |
| 14 | Deep Media Details Screen & Header | Collapsible hero header with blurred backdrop banner, title language switcher (English/Romaji/Japanese), poster card, and metadata stack | M4 | survey |
| 15 | Characters & Voice Actors Dual Tiles | Grid of dual-avatar character & voice actor tiles with character name, role (Main/Supporting), and seiyuu name | M4 | survey |
| 16 | Staff & Production Listing | Horizontal rail and grid of production staff (Director, Character Design, Studio, Music) with role badges | M4 | survey |
| 17 | Relations Graph & Connections | Visual hierarchy and relation pills (Prequel, Sequel, Spin-off, Side Story, Alternative) with 1-tap navigation | M4 | survey |
| 18 | Recommendations & Community Reviews | Horizontal recommendations rail and community reviews with spoiler warning blur and score breakdown | M4 | survey |
| 19 | YouTube Trailer Launcher | 1-tap trailer play button launching external intent or in-app dialog | M4 | survey |
| 20 | AniList Profile & Streak Activity | User profile view displaying daily streak, days watched, total entries, mean score, and activity distribution | M5 | survey |
| 21 | Score Distribution & Status Charts | Donut / pie status chart (Watching, Completed, On Hold, Dropped, Plan to Watch) and 1–10 score distribution histogram | M5 | survey |
| 22 | Real-Time Score & Progress Sync | Interactive list editor sheet with `+1` episode/chapter bumping, score updates, and automatic status transition to Completed | M5 | survey |
| 23 | Bracket & Parenthesis Balance Audit | 100% verification that all `{}` and `()` are balanced across all modified and created files | M6 | survey |
| 24 | Import Hygiene & Cleanliness | Zero unresolved dependencies, unused imports, or duplicate declarations | M6 | survey |
| 25 | Comprehensive KDoc Documentation | Complete documentation on all public composables, screen models, and repository methods | M6 | survey |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| 1 | Data, Models & GraphQL Layer | `DailyALMediaModels.kt`, `AnilistApi.kt`, `DiscoverCacheStore.kt` | none | DONE |
| 2 | Discover UI & Discovery Rails (DailyAL Visual Mirror) | `DailyALDiscoverComponents.kt`, `TrackingTab.kt` | M1 | DONE |
| 3 | Airing Schedule Screen & Live Countdown Timetable | `AiringScheduleView.kt`, `TrackingScreenModel.kt` | M1 | DONE |
| 4 | Deep Media Details & Advanced Filtering | `DailyALMediaDetailsComponents.kt`, `DailyALAdvancedFilterSheet.kt`, `MediaDetailsBottomSheet.kt`, `TrackingTab.kt` | M1, M2 | DONE |
| 5 | AniList Profile & Activity Sync | `DailyALProfileComponents.kt`, `TrackingScreenModel.kt`, `TrackingTab.kt`, `AnilistApi.updateMediaProgress` | M1, M2 | DONE |
| 6 | Verification, Bracket Integrity & Documentation | All modified and created files in `eu.kanade.tachiyomi.ui.discover.*` and `eu.kanade.tachiyomi.data.track.anilist.*` | M1..M5 | DONE (static review; compile check rides the next `assembleDebug`) |

## Interface Contracts

### Data Layer ↔ ScreenModel
```kotlin
// eu.kanade.tachiyomi.data.track.anilist.AnilistApi
suspend fun getDeepMediaDetails(mediaId: Long, isAnime: Boolean = true): Result<DailyALMediaDetails>
suspend fun searchAdvanced(filter: DailyALFilterState, page: Int = 1): Result<List<ALAnime>>
suspend fun getUserProfileStats(): Result<DailyALProfileStats>
suspend fun updateMediaProgress(mediaId: Long, progress: Int, status: String? = null, score: Int? = null): Result<Unit>
```

### Cache Layer ↔ ScreenModel
```kotlin
// eu.kanade.tachiyomi.data.discover.cache.DiscoverCacheStore
suspend fun getDeepMediaDetails(mediaId: Long): DailyALMediaDetails?
suspend fun saveDeepMediaDetails(mediaId: Long, details: DailyALMediaDetails, isReleasing: Boolean)
suspend fun getAdvancedFilterCache(filterKey: String): List<ALAnime>?
suspend fun saveAdvancedFilterCache(filterKey: String, items: List<ALAnime>)
```

### ScreenModel ↔ Compose UI
```kotlin
// eu.kanade.tachiyomi.ui.discover.TrackingScreenModel
val state: StateFlow<TrackingState>                 // trending/popular anime+manga feeds
val airingSchedule: StateFlow<List<AiringEpisode>>
val deepDetailsState: StateFlow<DeepDetailsState>    // Idle | Loading | Success(DailyALMediaDetails) | Error
val advancedFilter: StateFlow<DailyALFilterState?>
val advancedFilterResults: StateFlow<List<AnimeTrackSearch>>
val profileState: StateFlow<ProfileState>            // Idle | Loading | NotLoggedIn | Success(DailyALProfileStats) | Error

fun loadDeepDetails(mediaId: Long, isAnime: Boolean)   // cache-first, falls back to GraphQL
fun clearDeepDetails()
fun applyAdvancedFilter(filter: DailyALFilterState)    // cached per filter.toCacheKey(), results as AnimeTrackSearch
fun clearAdvancedFilter()
fun loadProfileStats()                                 // requires AniList login
fun updateMediaProgress(mediaId: Long, progress: Int, status: String? = null, score: Int? = null)
```

### Discover section routing (TrackingTab)
`DiscoverSection` enum: AnimeHome(0), Airing(1), MangaHome(2), Explore(3), SwipeDeck(4), Shorts(5), SyncHub(6), Profile(7).
Deep-details relations/recommendations open via lightweight search stubs (`openDeepEntry`), which re-trigger `loadDeepDetails` for the tapped entry.

## Code Layout
- `app/src/main/java/eu/kanade/tachiyomi/ui/discover/`
  - `DailyALMediaModels.kt` (Created): Domain models for rich media details, characters, staff, relations, reviews, filters, profile stats.
  - `DailyALDiscoverComponents.kt` (Modified): DailyAL segmented controls, hero spotlight, media cards, discovery rails.
  - `DailyALMediaDetailsComponents.kt` (Created, M4): Character/VA dual tiles, staff rail, relations grid, recommendations rail, spoiler-blur reviews, genres/tags row.
  - `DailyALAdvancedFilterSheet.kt` (Created, M4): Modal sheet for genre include/exclude, format, status, season/year, min-score slider, sort order → `DailyALFilterState`.
  - `DailyALProfileComponents.kt` (Created, M5): Profile header, summary stat cards, status distribution bar, score histogram, full-state container (loading/not-logged-in/error).
  - `AiringScheduleView.kt` (Modified): Timetable, live 1-second countdown ticker, day accordions.
  - `MediaDetailsBottomSheet.kt` (Modified, M4): Hosts deep-details sections beneath metadata; optional deep-details params keep prior call sites compiling; progress stepper/status chips optionally sync to AniList via `onProgressUpdate`.
  - `TrackingTab.kt` (Modified, M4+M5): `DiscoverSection` routing incl. new `Profile(7)`; Advanced Filters card in Explore + overflow entries; filter-results grid with edit/clear; deep-details wiring (auto-load on sheet open, relation/recommendation/tag navigation).
  - `TrackingScreenModel.kt` (Modified, M4+M5): `deepDetailsState`/`advancedFilter(Results)`/`profileState` flows; `loadDeepDetails`, `applyAdvancedFilter`, `loadProfileStats`, `updateMediaProgress`; ALAnime→AnimeTrackSearch mapping.
  - `DiscoverCacheStore.kt` alias in this package removed; the store lives at `data/discover/cache/DiscoverCacheStore.kt` (Modified): JSON cache for tracker feeds, airing schedule, deep details and filter results.
- `app/src/main/java/eu/kanade/tachiyomi/data/track/anilist/`
  - `AnilistApi.kt` (Modified): GraphQL queries for deep media details, advanced search, profile stats; M5 adds `updateMediaProgress` mediaId-based upsert mutation.

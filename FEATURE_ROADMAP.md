# EverythingMoe 86 Apps Feature Synthesis & Animetail Master Roadmap

This document analyzes and synthesizes the standout features from the 86 top Anime, Manga, and Light Novel applications cataloged on [EverythingMoe](https://everythingmoe.com/section/apps) and maps them into a comprehensive feature implementation roadmap for **Animetail**.

---

## 1. Feature Synthesis by Application Category

### Category A: Manga & Webtoon Readers
*(Mihon, Kotatsu, Komikku, TachiyomiSY, TachiyomiJ2K, Neko, Yokai, Tachimanga, Paperback, Aidoku, Venera)*

- **Extension Ecosystem & Source Management**: Dynamic extension repository fetching, multi-repo support, source trust verification, and extension auto-updates.
- **Advanced Reader Engine**:
  - Webtoon mode with continuous vertical scrolling, automatic border cropping, color filters (grayscale, inverted, sepia), and page blending.
  - Paged mode with dual-page rendering (split landscape pages), reading orientation (RTL, LTR, Vertical), and page tap/swipe zones.
  - Page preloading engine with smart background memory management.
- **Smart Category & Library Management**: Custom categories, custom cover art override, reading progress badges, unread chapter counters, and automatic library updates.

---

### Category B: Anime Streaming & Video Players
*(CloudStream, Aniyomi, Hayase/Miru, Seanime, Anikku, Shiru, AnimeTV, Kaizoyu, Awery, Zenshin)*

- **Advanced Player Features**:
  - Native MPV / ExoPlayer integration with hardware acceleration.
  - **Hold-to-2x playback speed** & gesture-driven variable speed selection (`[0.25x .. 3.0x]`).
  - **Anime4K & FSR Upscaling**: Shader integration for high-definition rendering.
  - **Auto-Skip Intro / Outro & Filler Detection**: AniSkip API integration for skipping anime OP/ED automatically or via 1-tap pill.
  - Subtitle customization (font size, background opacity, stroke, position offset, custom SSA/ASS font loading).
  - Multi-audio & subtitle track selection with language preferences.
  - Picture-in-Picture (PiP) mode and background audio playback for music/drama tracks.
- **Torrent & Magnet Streaming Engine** *(from Miru / Seanime / Zenshin)*: Direct magnet link / torrent playback streaming without requiring full offline download beforehand.

---

### Category C: Light Novel & Web Novel Readers
*(LNReader, QuickNovel, Shosetsu, Tsundoku, NovelDokusha, Ranobe, IReader)*

- **E-Reader Text Engine**:
  - Custom font family, font size, line spacing, paragraph margins, and background themes (Dark, Sepia, OLED Pitch Black, Solarized).
  - Text-to-Speech (TTS) integration with voice selection, speed control, and background notification controls.
  - ePub & Web Novel parsing with table-of-contents navigation and inline image rendering.
  - Chapter bookmarking, paragraph highlighting, and reading position sync.

---

### Category D: Universal Discovery & All-In-One Hubs
*(AnymeX, Dantotsu, Saikou, Dartotsu, Kototoro, Chimahon, OtakuWorld, Azyx)*

- **Discovery Deck & Feeds**:
  - **Tinder-style Discovery Deck**: Interactive swipe cards for quick anime/manga discovery based on preferences.
  - **Shorts / Trailer Feed**: Vertical short video clips & PV trailers feed with direct 1-tap "Add to Library" or "Watch Now".
  - **Live Airing Calendar**: Interactive weekly schedule with countdown timers (`Ep X drops in Y hours Z mins`) and push notifications.
  - Seasonal charts (Trending, Popular, Top Rated, Upcoming) with rich rank badges.
- **Deep Tracking & Social Metadata**:
  - Multi-tracker sync: AniList, MyAnimeList, Kitsu, Shikimori, Bangumi, Simkl, Trakt.
  - Deep metadata bottom sheets: Voice actors (seiyuu), staff, character profiles, relations graph (prequels/sequels/spin-offs), community reviews, user score distributions, and studio portfolios.
  - **Discord Rich Presence (RPC)**: Live status broadcasting (e.g. *"Watching Episode 12 of Jujutsu Kaisen"* with elapsed timer and poster image).

---

### Category E: Cross-Platform & Server Sync
*(Suwayomi / Tachidesk, Houdoku, Teemii, Manatan, Unyo)*

- **Remote Sync & Web Server**: Host local Suwayomi/Animetail server to synchronize library state, reading progress, and watch history across Android, Desktop (Windows/Linux/macOS), and Web browsers.

---

## 2. Animetail Master Feature Matrix & Implementation Status

| Feature ID | Feature Name | Origin App Inspiration | Current Status in Animetail |
|---|---|---|---|
| **F1.1** | Unified Media Switcher (Anime / Manga / LN) | Mihon, AnymeX | **Implemented** (`LibraryMediaSwitcher.kt`) |
| **F1.2** | Material 3 Expressive Dynamic Theme | TachiyomiJ2K, Mihon | **Implemented** (`MaterialExpressiveTheme.kt`) |
| **F2.1** | Multi-Tracker Integration (AniList, MAL, etc.) | Dantotsu, Saikou | **Implemented** (`TrackerManager.kt`) |
| **F2.2** | Airing Calendar & Episode Countdown | Dantotsu, Saikou | **Implemented** (`AiringScheduleView.kt`) |
| **F2.3** | Tinder-style Discovery Deck | Dantotsu, AnymeX | **Implemented** (`TinderDiscoveryDeck.kt`) |
| **F2.4** | Shorts Video & Trailer Feed | AnymeX, Dartotsu | **Implemented** (`ShortsVideoFeed.kt`) |
| **F3.1** | Player Gesture Speed Controls (Hold 2x & Swipe) | CloudStream, Animetail | **Implemented** (`PlayerControls.kt`) |
| **F3.2** | Next Episode Autoplay & Countdown Card | CloudStream, Aniyomi | **Implemented** (`NextEpisodeAutoplayCard.kt`) |
| **F4.1** | Analytics & Score Distribution Charts | Dantotsu, AnymeX | **Implemented** (`ScoreDistributionBarChart.kt`) |
| **F4.2** | Storage Inspector & Smart Cleaners | Mihon, TachiyomiSY | **Implemented** (`StorageScreenContent.kt`) |
| **F5.1** | AniSkip Auto-Skip OP/ED API Integration | CloudStream, Seanime | **Next Up (Planned)** |
| **F5.2** | Light Novel Text Reader Engine (ePub/TTS) | LNReader, QuickNovel | **Next Up (Planned)** |
| **F5.3** | Discord Rich Presence (RPC) Integration | Dantotsu, Saikou | **Next Up (Planned)** |
| **F5.4** | AniList / MAL Detailed Character & Staff Sheet | AnymeX, Dantotsu | **Next Up (Planned)** |

---

## 3. Immediate Implementation Focus

We are proceeding with implementing **AniSkip (Auto-Skip OP/ED)** and enhancing the **Light Novel Reader Engine & Discord RPC / Metadata details** in Animetail.

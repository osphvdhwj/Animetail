<div align="center">

<a href="https://github.com/osphvdhwj/Animetail">
    <img src="./.github/assets/icon.png" alt="Aniyomi Plus logo" title="Aniyomi Plus" width="96"/>
</a>

# Aniyomi Plus 🚀

### The ultimate all-in-one Anime & Manga powerhouse for Android.
Built upon **Mihon**, **Aniyomi**, **Kuukiyomi**, and **Animetail**, supercharged with **Material You M3 Expressive Design**, **AniForge-style Bento Statistics**, **Multi-Tracker Hubs**, **Standalone Video Playback**, and **Tinder-Style Extension Discovery**.

[![GitHub Release](https://img.shields.io/badge/Release-Aniyomi%20Plus-6366f1?style=for-the-badge&logo=android)](https://github.com/osphvdhwj/Animetail/releases)
[![Architecture](https://img.shields.io/badge/ABI-arm64--v8a-emerald?style=for-the-badge)](https://github.com/osphvdhwj/Animetail)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=for-the-badge)](/LICENSE)

</div>

---

## 🌟 Key Features Overview

### 🎨 1. Material You (M3 Expressive) UI & Navigation
* **Clean 5-Tab Navigation**: Streamlined bottom bar (Library, Updates, Discover, Browse, More).
* **Smart Active Count Pills**:
  * In Anime Tab: `[ 🎬 Anime (42) ]  [ 📖 Manga ]`
  * In Manga Tab: `[ 🎬 Anime ]  [ 📖 Manga (15) ]`
* **Modern Header & Branding**: Animated `LogoHeader` and dynamic Monet color palette matching your wallpaper or cover art.
* **WhatsApp-Style Contextual FAB**: Smooth animated floating action button at the bottom right that changes dynamically per tab (Swipe Deck Toggle, Resume playback, Open local file).

---

### 📊 2. AniForge-Inspired Offline Bento Statistics
* **Animated Donut Chart**: Multi-color arc status distribution (Watching/Reading, Completed, Plan to Watch/Read, On Hold, Dropped) with center total count.
* **Bento Time Counter Widget**: Real-time calculated Days, Hours, and Minutes spent watching anime and reading manga.
* **Top Genres & Studios Leaderboard**: Ranked progress bars calculating top genres and animation studios/authors offline from your library.
* **Multi-Tracker Statistics Hub**: Per-site breakdown tabs for **AniList, MyAnimeList, Kitsu, Shikimori, Bangumi, Simkl, and MangaUpdates** with offline computation.

---

### 🃏 3. Tinder-Style Extension Discovery Deck
* **Interactive Swipe Physics**: Swipe left (Pass/Dismiss) and Swipe right (Save to Library / Explore) with spring physics and tilt rotation.
* **Universal Extension Bridge**: Tapping any card opens detailed metadata and lets you search across all installed anime and manga extensions with one click.
* **Deck Controls**: Floating action buttons for Pass ❌, Info ℹ️, and Save ⭐ with undo support.

---

### 🎬 4. Standalone Video Player & System Handler
* **Play ANY Local Video**: Open and watch external `.mp4`, `.mkv`, `.webm`, `.avi`, `.ts` files inside the high-performance MPV player engine.
* **Android System Intent Handler**: Registered as a system video player so you can open videos directly from file managers, messaging apps, and downloads.
* **Next-Gen Player Features**:
  * Quick Speed Pills (1.0x, 1.25x, 1.5x, 2.0x, 3.0x).
  * **Hold for 2X Speed Gesture**: Press and hold anywhere to temporarily boost speed with smooth haptic feedback and glass pill HUD.
  * Advanced Subtitle & Audio track selectors, aspect ratio stretching, and hardware decoding.

---

### 📅 5. Tracking & Live Airing Schedule Hub
* **Real-time Airing Calendar**: Live countdown timers for upcoming episodes airing today, tomorrow, or this week.
* **Hero Trend Carousels**: Featured trending banners with direct "Search in Sources" bridge.
* **Offline Persistent Cache**: Instant loading without API bottlenecks.
* **Force Reload All Sites**: 3-dot overflow menu and pull-to-refresh to sync all 7 trackers concurrently.
* **Custom Background Auto-Refresh**: Configurable sync intervals (6h, 12h, 24h, 48h, or manual).
* **Backup & Restore Integration**: Back up discover cache, cover art cache, and extended descriptions/tags to `.tachibk`.

---

### 📺 6. YouTube (2024–2026) Style Recent History
* **Horizontal Recent Shelf**: Embedded in the More / You profile screen with 16:9 thumbnails, watched progress bars, and one-tap resume buttons.
* **Direct "View all" Link**: Pushes the full chronological History page.
* **Decluttered Bottom Bar**: Bottom History tab hidden by default for maximum screen real estate (re-enable anytime in `Settings > Appearance > Navigation`).

---

### 💾 7. Revamped Data and Storage Hub
* **Proportional Segmented Visualizer**: Dynamic multi-color breakdown bar showing exact space taken by each series and category.
* **1-Tap Quick Cleaners**:
  * 🧹 Clear Cover & Image Cache
  * 🗑️ Clean Extension Temporary Data
  * ⚡ Clear Discover & Tracking Cache
  * 🔍 Remove Orphan Downloads
* **Search & Filter Bar**: Instantly find and sort large series by size or name with batch delete confirmation.

---

### 📱 8. Experimental Offline Shorts Feed
* **Vertical Short Video Player**: TikTok/Shorts-style feed powered by downloaded anime episodes.
* **"Watch Full Episode"**: Seamlessly jumps from clip preview to full episode playback in the player.
* **Toggle in Settings**: Hidden by default, enable anytime under `Settings > Advanced > Experimental`.

---

## 🛠️ Build & Installation

### Build Standalone `arm64-v8a` Signed APK
```powershell
$env:JAVA_HOME = "C:\platform-tools\jdk"
$env:PATH = "$env:JAVA_HOME\bin;C:\platform-tools\MinGit\cmd;$env:PATH"
$env:ANDROID_HOME = "C:\Users\HP\AppData\Local\Android\Sdk"

# Compile and package single arm64 APK signed with Android certificate:
.\gradlew.bat :app:assembleDebug
```

The output APK will be generated at:
```text
app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
```

---

## 🌐 Repository & Upstream
* **Fork Repository**: [https://github.com/osphvdhwj/Animetail](https://github.com/osphvdhwj/Animetail)
* **Branch**: `master`

---

## 📄 License
Aniyomi Plus is free and open source software licensed under the [Apache License 2.0](/LICENSE).

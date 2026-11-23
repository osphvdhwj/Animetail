<div align="center">

<a href="https://github.com/osphvdhwj/Animetail">
    <img src="./.github/assets/icon.png" alt="Aniyomi Plus logo" title="Aniyomi Plus" width="96"/>
</a>

# Aniyomi Plus 🚀

### The ultimate all-in-one Anime, Manga, Manhwa, Tracking & Standalone Video Player powerhouse for Android.
Built upon **Mihon**, **Aniyomi**, **Kuukiyomi**, and **Animetail**, supercharged with **DailyAL Discover Experience**, **Material 3 Expressive Design**, **Global Anime Power Features**, **High-Performance MPV Video Player**, and **Universal Multi-Tracker Hub**.

[![GitHub Release](https://img.shields.io/badge/Release-Aniyomi%20Plus-6366f1?style=for-the-badge&logo=android)](https://github.com/osphvdhwj/Animetail/releases)
[![Architecture](https://img.shields.io/badge/ABI-arm64--v8a-emerald?style=for-the-badge)](https://github.com/osphvdhwj/Animetail)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=for-the-badge)](/LICENSE)
[![Developer](https://img.shields.io/badge/Developer-@osphvdhwj-blueviolet?style=for-the-badge&logo=github)](https://github.com/osphvdhwj)

</div>

---

## 🌟 Key Features & Innovations

### 🌸 1. Complete DailyAL Discover Architecture
* **Segmented Mode Picker**: Pill-shaped switcher with smooth animated containers (`[ 🎬 Anime | 📅 Airing | 📖 Manga | 🏷️ Genres ]`).
* **Quick Action Controls**:
  * 🎲 **Lucky Random Roll**: Picks a surprise trending anime or manga with one tap.
  * 🌸 **Seasonal Release Chart**: Full season overview with format filters (TV, Movie, OVA, ONA, Specials).
  * 🏆 **Top 100 Leaderboards**: Quick access to all-time top-rated media worldwide.
* **DailyAL Ranking Categories Grid (`AllRankingWidget`)**:
  * 2-column adaptive category cards for Top Airing, Top Upcoming, Most Popular, Anime Movies, Light Novels, Manhwa, and Oneshots.
* **Search Tag Filter Strip**: Quick-tag chips (`#all`, `#airing`, `#upcoming`, `#bypopularity`, `#favorite`, `#movie`, `#special`, `#ova`, `#ona`) for instant filtered exploration.
* **DailyAL Media Cards (`AnimeGridCard`)**: 140dp width, 1/1.4 aspect ratio with gold/silver/bronze rank badges (`#1`, `#2`, `#3`), star score pills, format indicators, and `VisibleSection` headers with `"View All →"`.
* **7-Day Airing Calendar**: Day-by-day airing schedule with real-time broadcast countdowns (`Ep 8 airs in 2h 15m`).
* **AllGenreWidget**: 18+ vibrant genre & theme cards with colorful indicators and emojis.

---

### ⚡ 2. Double-Tap Navigation & Global Anime Power Hub
* **Double-Tap Bottom Navigation**: Double-tapping the Discover tab icon in the bottom navigation bar instantly opens the **✨ Discover Power Hub**.
* **Smart Reselect (Single Tap on Active Tab)**:
  * If scrolled down, smoothly animates back to the top of the feed.
  * If already at the top, launches the Power Features Hub.
* **Global Power Features Hub (`GlobalAnimeFeaturesSheet`)**:
  * 🎵 **Trending Anime Soundtracks & Theme Songs (OSTs)** *(from Dantotsu / Saikou)*: Preview viral opening & ending themes (Creepy Nuts, YOASOBI, King Gnu, Ado, Tatsuya Kitani) with 1-tap playback.
  * 🎙️ **Popular Seiyuu & Voice Talent Spotlight** *(from DailyAL / MAL)*: Browse celebrated voice actors (Kenjiro Tsuda, Hiroshi Kamiya, Saori Hayami, Mamoru Miyano, Rie Takahashi) and their iconic roles.
  * 🔄 **Universal Multi-Sync Dashboard**: Quick synchronization across AniList, MyAnimeList, Kitsu, Shikimori, and Simkl.

---

### 🃏 3. Refined Tinder Discovery Deck
* **Disappear & Reappear on Swipe**: During active card drag gestures, bottom buttons and control chips automatically fade away, leaving the artwork completely clean and unobstructed. Controls smoothly fade back when the card is released or transitions.
* **Zero Harsh Dividing Lines**: Borderless M3 tonal surfaces and seamless multi-stop fractional gradients—no hardcoded pixel gradient cutoff lines or thick borders.
* **⏮️ Undo / Rewind**: Accidentally passed a card? Tap the Rewind button to spring it right back onto the deck.
* **🔎 Search in Sources**: Instant bridge to search your installed extension sources (`GlobalAnimeSearchScreen` / `GlobalMangaSearchScreen`) directly from the card.
* **🏷️ Media Type Filter**: Toggle deck between `[ All | 🎬 Anime | 📖 Manga ]`.

---

### 🎬 4. Standalone Video Player Engine (mpvEx Architecture)
* **Play Any External Video**: Built-in MPV hardware-accelerated playback for local and network video streams.
* **Full Media & Storage Permissions**: Seamless runtime permission flow supporting Android 13+ (`READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`) with All Files Access fallback.
* **Automatic External Subtitle Detection**: Direct POSIX file descriptor resolution via `/proc/self/fd` so MPV automatically detects and loads same-folder subtitle files (`.srt`, `.ass`).
* **Universal System Intent Handler**: Comprehensive intent-filters for 20+ media formats (`.mp4`, `.mkv`, `.webm`, `.avi`, `.mov`, `.flv`, `.ts`, `.m2ts`, `.m3u8`, `.3gp`, `.wmv`, etc.) and `ACTION_SEND`.
* **Zero Root Dependency**: No root access or symlink scripts required.
* **Quick Access File Pickers**: Open local files anytime from More Screen, More Tab, or Network Stream Screen.

---

### 🌐 5. Universal Multi-Tracker Client Hub
* **Two-Way Sync**: Connect with **AniList**, **MyAnimeList (MAL)**, **Kitsu**, **Shikimori**, **Bangumi**, **Simkl**, and **MangaUpdates**.
* **In-App Tracking Manager**: 1-tap status chips (`Watching`, `Plan to Watch`, `Completed`, `On Hold`, `Dropped`), live progress steppers (`[-] 12 / 24 [+]`), and score distribution charts.

---

### 📖 6. Multi-Format Reading Engine
* **Universal Format Support**:
  * 🇰🇷 **Manhwa (Webtoons)** — Continuous vertical scrolling with custom cyan accents.
  * 🇨🇳 **Manhua** — High-resolution color reading optimization.
  * 📚 **Light Novels** — Text and EPUB rendering support.
  * 🇯🇵 **Manga** — Right-to-left and dual-page spread modes.
  * 📑 **One-Shots & Anthologies** — Distinct category badges.

---

### 🍏 7. Apple-Grade Product & UX Continuity
* **Next Episode Autoplay Overlay**: 20-second countdown card with next episode title, animated progress bar, and "Play Now" button as credits roll.
* **Context-Aware AniSkip**: Floating "Skip Intro" / "Skip Outro" button appears during theme songs without manual scrubbing.
* **Hold for 2X Speed**: Press and hold anywhere on screen for instant 2x speed boost with tactile feedback.
* **Scroll-Aware Floating Action Button**: Seamlessly slides and fades out during feed scrolling, returning when scrolling pauses.
* **Unique Modern Emblem**: Bespoke cosmic obsidian gradient launcher background with radiant manga-and-play vector icon.

---

## 🛠️ Build & Installation

### Local Single APK Build (`arm64-v8a`)
Run the automated build script in PowerShell:
```powershell
.\build_and_export.ps1
```
The output APK will be placed in:
```text
C:\platform-tools\APKs\AniyomiPlus-arm64-v8a-debug.apk
```

### GitHub Actions Release Build (Production APK)
1. Push a release tag to GitHub:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
2. Or go to the **Actions** tab on GitHub → select **"Build Release APK"** → click **"Run workflow"**.
3. A small, signed, production release APK will be compiled and attached directly to the GitHub Release.

---

## 🌐 Repository & Developer
* **Developer**: [@osphvdhwj](https://github.com/osphvdhwj)
* **Repository**: [https://github.com/osphvdhwj/Animetail](https://github.com/osphvdhwj/Animetail)
* **Releases**: [https://github.com/osphvdhwj/Animetail/releases](https://github.com/osphvdhwj/Animetail/releases)
* **About the Project**: See [ABOUTME.md](ABOUTME.md)

---

## 📄 License
Aniyomi Plus is free and open source software licensed under the [Apache License 2.0](/LICENSE).


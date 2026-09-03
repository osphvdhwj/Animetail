<div align="center">

<a href="https://github.com/osphvdhwj/Animetail">
    <img src="./.github/assets/icon.png" alt="Aniyomi Plus logo" title="Aniyomi Plus" width="96"/>
</a>

# Aniyomi Plus 🚀

### The ultimate all-in-one Anime, Manga, Manhwa & Tracking powerhouse for Android.
Built upon **Mihon**, **Aniyomi**, **Kuukiyomi**, and **Animetail**, supercharged with **Material 3 Expressive Design**, **Apple-Grade UX Craftsmanship**, **Universal Multi-Tracker Hub**, **Standalone Video Engine**, and **Multi-Format Reading**.

[![GitHub Release](https://img.shields.io/badge/Release-Aniyomi%20Plus-6366f1?style=for-the-badge&logo=android)](https://github.com/osphvdhwj/Animetail/releases)
[![Architecture](https://img.shields.io/badge/ABI-arm64--v8a-emerald?style=for-the-badge)](https://github.com/osphvdhwj/Animetail)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=for-the-badge)](/LICENSE)
[![Developer](https://img.shields.io/badge/Developer-@osphvdhwj-blueviolet?style=for-the-badge&logo=github)](https://github.com/osphvdhwj)

</div>

---

## 🌟 Key Features

### 🌐 1. Universal Multi-Tracker Client Hub
* **Direct Two-Way Sync**: Connect with **AniList**, **MyAnimeList (MAL)**, **Kitsu**, **Shikimori**, **Bangumi**, **Simkl**, and **MangaUpdates**.
* **Interactive In-App Tracking Manager**:
  * Tap any series to bring up the custom tracking sheet.
  * **1-Tap Status Chips**: Easily switch between `Watching / Reading`, `Plan to Watch / Plan to Read`, `Completed`, `On Hold`, and `Dropped` with instant real-time confirmation.
  * **Live Progress Stepper**: Increment episodes/chapters with `[-] 12 / 24 [+]` buttons.
  * **Zero-Friction Source Bridge**: Direct **"Watch in App"** or **"Read in App"** button that automatically searches across all installed extensions and opens the player or reader instantly.
* **Score Distribution & Stats**: Dynamic 1–10 rating distribution bar charts and offline time calculations.

---

### 📖 2. Multi-Format Reading Engine
* **Universal Format Support**:
  * 🇰🇷 **Manhwa (Webtoons)** — Continuous vertical scrolling with custom cyan accents.
  * 🇨🇳 **Manhua** — High-resolution color reading optimization.
  * 📱 **Webtoons & Web Manga** — Infinite scroll with seam hiding.
  * 📚 **Light Novels** — Text and epub rendering support.
  * 🇯🇵 **Manga** — Right-to-left and dual-page spread modes.
  * 📑 **One-Shots & Anthologies** — Distinct category badges.
* **Instant Format Filter Pills**: Switch between All, Manga, Manhwa, Manhua, and Novels on the feed with zero page reloads.

---

### 🍏 3. Apple-Grade Product & UX Philosophy + Material 3
* **“It Just Works” Continuity**:
  * **Jump Back In**: Your active anime and manga are front and center on the Home feed with accurate progress bars and instant 1-tap playback/reading.
* **Intelligent Video Automation**:
  * **Next Episode Autoplay Overlay**: 20-second countdown card with next episode title, animated progress bar, and "Play Now" button as credits roll.
  * **Context-Aware AniSkip**: Floating "Skip Intro" / "Skip Outro" button appears during theme songs without manual scrubbing.
  * **Hold for 2X Speed**: Press and hold anywhere on screen for instant 2x speed boost with tactile feedback.
* **7-Day Live Airing Calendar**:
  * Real-time countdowns (`Ep 8 airs in 2h 15m`) across Today, Tomorrow, and the entire week, synced with your local time zone.

---

### 🃏 4. Tinder Discovery Deck
* **Interactive Swipe Physics**: Card swiping with tilt physics, depth scaling, and real-time stamps (`SAVE` ⭐, `PASS` ❌, `DISMISS` 🗑️).
* **Direct Extension Bridge**: Discover new titles and immediately check available stream and scanlation sources across installed extensions.

---

### 🎬 5. Standalone Video Player Engine
* **Play Any External Video**: Open and watch external `.mp4`, `.mkv`, `.webm`, `.abi`, `.ts` files inside the hardware-accelerated MPV player.
* **Android System Intent Handler**: Set Aniyomi Plus as your default video player for downloaded media from messaging apps, browsers, and local storage.
* **Advanced Player Controls**: Custom speed presets, audio & subtitle track switcher, aspect ratio stretching, and picture-in-picture (PiP).

---

### 🔄 6. Automated GitHub Releases & In-App Updates
* **Production CI/CD**: Clean GitHub Actions workflow (`.github/workflows/release_apk.yml`) that compiles, optimizes (R8/ProGuard minified), signs, and publishes release APKs automatically.
* **Seamless OTA Updates**: The built-in updater checks [`osphvdhwj/Animetail`](https://github.com/osphvdhwj/Animetail) for releases, notifying users directly in the app when a new version is available.
* **Support Us Removed**: Clean, distraction-free experience with all donation campaigns and popups removed.

---

## 🛠️ Build & Installation

### Local Single APK Build (`arm64-v8a`)
Run the automated build script in PowerShell:
```powershell
.\build_and_export.ps1
```
The output APK will be automatically placed in:
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

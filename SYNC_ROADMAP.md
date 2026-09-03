# Awesome MyAnimeList & Universal Multi-Tracker Sync Strategy for Animetail

Inspired by [Awesome MyAnimeList](https://github.com/Katsute/awesome-myanimelist), this document details the enhanced tracking, synchronization, and cross-platform architecture integrated into **Animetail**.

---

## 1. Supported Tracker Services Matrix

Animetail features a unified multi-tracker hub supporting **15 distinct platforms**:

| Service | Type | API Architecture | Auto-Match | Multi-Sync |
|---|---|---|---|---|
| **MyAnimeList (MAL)** | Anime & Manga | REST / OAuth2 PKCE | Yes | Yes |
| **AniList** | Anime & Manga | GraphQL / OAuth2 | Yes | Yes |
| **Kitsu** | Anime & Manga | JSON:API / OAuth2 | Yes | Yes |
| **Shikimori** | Anime & Manga | REST / OAuth2 | Yes | Yes |
| **Bangumi** | Anime & Manga | REST / OAuth2 | Yes | Yes |
| **Simkl** | Anime, Manga, Movies, TV | REST / OAuth2 | Yes | Yes |
| **Trakt** | Anime, Movies, TV | REST / OAuth2 | Yes | Yes |
| **TMDB** | Anime & TV Metadata | REST API v3 | Yes | Metadata |
| **MangaUpdates** | Manga | REST API | Yes | Yes |
| **MangaBaka** | Manga | REST API | Yes | Yes |
| **Hikka** | Anime & Manga | REST API | Yes | Yes |
| **Komga** | Self-Hosted Manga | REST / Basic Auth | Server Sync | Yes |
| **Kavita** | Self-Hosted Manga & LN | REST / Token Auth | Server Sync | Yes |
| **Jellyfin** | Self-Hosted Media Server | REST / API Key | Server Sync | Yes |
| **Suwayomi** | Self-Hosted Desktop Server | REST API | Server Sync | Yes |

---

## 2. Multi-Tracker Simultaneous Syncing Engine (`TrackSyncEngine`)

When progress is made in Animetail (e.g. Episode 12 watched or Chapter 45 read):
1. **Local State Update**: Progress is updated locally in SQLDelight database.
2. **Parallel Track Dispatch**: `TrackerManager.loggedInTrackers()` filters active sessions.
3. **Cross-Platform Auto-Mapping**: Maps local entry across MAL, AniList, Kitsu, and Simkl IDs.
4. **Resilient Retry Queue**: Failed network calls are queued in offline cache and synced automatically when network connection restores.

---

## 3. MAL-Sync Cross-Database ID Mapper

To ensure smooth integration across MyAnimeList, AniList, and Kitsu:
- **AniList GraphQL Query** fetches `idMal` directly.
- **Kitsu API** checks external mapping links for `myanimelist` and `anilist` IDs.
- **Arme/Anime-Lists Cross-Reference**: Fallback mapping engine for entries with differing episode counts or split seasons.

---

## 4. Import / Export Capabilities

- **MAL XML Import / Export**: Full compatibility with MyAnimeList official backup format.
- **AniList JSON Sync**: Backup and restore user reading & watching history.
- **Mihon / Tachiyomi / Aniyomi Backup**: Import legacy `.proto.gz` and `.json` backup files seamlessly.

package eu.kanade.tachiyomi.ui.entries.manga.track

import eu.kanade.tachiyomi.data.track.Tracker
import tachiyomi.domain.track.manga.model.MangaTrack

/**
 * Presentation card/item component for MangaTrackItem.
 *
 * Handles compact data display, visual badges, and click/long-press actions.
 */
data class MangaTrackItem(val track: MangaTrack?, val tracker: Tracker)

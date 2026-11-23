package eu.kanade.tachiyomi.ui.entries.anime.track

import eu.kanade.tachiyomi.data.track.Tracker
import tachiyomi.domain.track.anime.model.AnimeTrack

/**
 * Presentation card/item component for AnimeTrackItem.
 *
 * Handles compact data display, visual badges, and click/long-press actions.
 */
data class AnimeTrackItem(val track: AnimeTrack?, val tracker: Tracker)

package eu.kanade.domain.discover

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/**
 * Repository for user preferences relating to Discover and Tracking features.
 *
 * Manages configuration for automated background cache preloading, backup inclusion,
 * and discovery modes (Tinder swipe deck, swipe dismissal behavior).
 */
class DiscoverPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun autoRefreshIntervalHours(): Preference<Int> =
        preferenceStore.getInt(PREF_DISCOVER_AUTO_REFRESH_HOURS, 24)

    fun includeDiscoverInBackup(): Preference<Boolean> =
        preferenceStore.getBoolean(PREF_INCLUDE_DISCOVER_IN_BACKUP, true)

    fun enableExperimentalShorts(): Preference<Boolean> =
        preferenceStore.getBoolean(PREF_ENABLE_EXPERIMENTAL_SHORTS, false)

    fun enableTinderSwipeDeck(): Preference<Boolean> =
        preferenceStore.getBoolean(PREF_ENABLE_TINDER_SWIPE_DECK, true)

    fun swipeBothSidesDismiss(): Preference<Boolean> =
        preferenceStore.getBoolean(PREF_SWIPE_BOTH_SIDES_DISMISS, false)

    fun lastPreloadTimestamp(): Preference<Long> =
        preferenceStore.getLong(PREF_DISCOVER_LAST_PRELOAD_TS, 0L)

    companion object {
        private const val PREF_DISCOVER_AUTO_REFRESH_HOURS = "pref_discover_auto_refresh_hours"
        private const val PREF_INCLUDE_DISCOVER_IN_BACKUP = "pref_include_discover_in_backup"
        private const val PREF_ENABLE_EXPERIMENTAL_SHORTS = "pref_enable_experimental_shorts"
        private const val PREF_ENABLE_TINDER_SWIPE_DECK = "pref_enable_tinder_swipe_deck"
        private const val PREF_SWIPE_BOTH_SIDES_DISMISS = "pref_swipe_both_sides_dismiss"
        private const val PREF_DISCOVER_LAST_PRELOAD_TS = "pref_discover_last_preload_ts"
    }
}

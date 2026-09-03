package eu.kanade.domain.discover

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class DiscoverPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun autoRefreshIntervalHours(): Preference<Int> {
        return preferenceStore.getInt("pref_discover_auto_refresh_hours", 24)
    }

    fun includeDiscoverInBackup(): Preference<Boolean> {
        return preferenceStore.getBoolean("pref_include_discover_in_backup", true)
    }

    fun enableExperimentalShorts(): Preference<Boolean> {
        return preferenceStore.getBoolean("pref_enable_experimental_shorts", false)
    }

    fun enableTinderSwipeDeck(): Preference<Boolean> {
        return preferenceStore.getBoolean("pref_enable_tinder_swipe_deck", true)
    }

    fun swipeBothSidesDismiss(): Preference<Boolean> {
        return preferenceStore.getBoolean("pref_swipe_both_sides_dismiss", false)
    }

    fun lastPreloadTimestamp(): Preference<Long> {
        return preferenceStore.getLong("pref_discover_last_preload_ts", 0L)
    }
}

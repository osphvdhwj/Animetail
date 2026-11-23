package eu.kanade.presentation.more.settings.screen

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import eu.kanade.domain.discover.DiscoverPreferences
import eu.kanade.presentation.more.settings.Preference
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Settings for the Discover tab: background cache refresh cadence, swipe deck,
 * experimental Shorts, and backup inclusion.
 */
object SettingsDiscoverScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    @StringRes
    override fun getTitleRes() = TLMR.strings.pref_category_discover

    @Composable
    override fun getPreferences(): List<Preference> {
        val discoverPreferences = remember { Injekt.get<DiscoverPreferences>() }

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(TLMR.strings.pref_category_discover),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.ListPreference(
                        preference = discoverPreferences.autoRefreshIntervalHours(),
                        entries = mapOf(
                            0 to stringResource(TLMR.strings.pref_discover_auto_refresh_off),
                            6 to stringResource(TLMR.strings.pref_discover_auto_refresh_hours, 6),
                            12 to stringResource(TLMR.strings.pref_discover_auto_refresh_hours, 12),
                            24 to stringResource(TLMR.strings.pref_discover_auto_refresh_hours, 24),
                            48 to stringResource(TLMR.strings.pref_discover_auto_refresh_hours, 48),
                        ),
                        title = stringResource(TLMR.strings.pref_discover_auto_refresh),
                        subtitle = stringResource(TLMR.strings.pref_discover_auto_refresh_summary),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = discoverPreferences.enableTinderSwipeDeck(),
                        title = stringResource(TLMR.strings.pref_discover_swipe_deck),
                        subtitle = stringResource(TLMR.strings.pref_discover_swipe_deck_summary),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = discoverPreferences.swipeBothSidesDismiss(),
                        title = stringResource(TLMR.strings.pref_discover_swipe_both_sides),
                        subtitle = stringResource(TLMR.strings.pref_discover_swipe_both_sides_summary),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = discoverPreferences.enableExperimentalShorts(),
                        title = stringResource(TLMR.strings.pref_discover_shorts),
                        subtitle = stringResource(TLMR.strings.pref_discover_shorts_summary),
                    ),
                ),
            ),
        )
    }
}

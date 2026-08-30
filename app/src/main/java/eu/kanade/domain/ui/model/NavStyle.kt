package eu.kanade.domain.ui.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.History
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.history.HistoriesTab
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import eu.kanade.tachiyomi.ui.discover.TrackingTab
import tachiyomi.i18n.aniyomi.AYMR

enum class NavStyle(
    val titleRes: StringResource,
    val moreTab: Tab,
) {
    MOVE_MANGA_TO_MORE(titleRes = AYMR.strings.pref_bottom_nav_no_manga, moreTab = LibraryTab),
    MOVE_UPDATES_TO_MORE(titleRes = AYMR.strings.pref_bottom_nav_no_updates, moreTab = UpdatesTab),
    MOVE_HISTORY_TO_MORE(titleRes = AYMR.strings.pref_bottom_nav_no_history, moreTab = HistoriesTab),
    MOVE_BROWSE_TO_MORE(titleRes = AYMR.strings.pref_bottom_nav_no_browse, moreTab = BrowseTab),
    ;

    val moreIcon: ImageVector
        @Composable
        get() = when (this) {
            MOVE_MANGA_TO_MORE -> Icons.Outlined.CollectionsBookmark
            MOVE_UPDATES_TO_MORE -> ImageVector.vectorResource(id = R.drawable.ic_updates_outline_24dp)
            MOVE_HISTORY_TO_MORE -> Icons.Outlined.History
            MOVE_BROWSE_TO_MORE -> Icons.Outlined.Explore
        }

    val tabs: List<Tab>
        get() {
            return mutableListOf(
                eu.kanade.tachiyomi.ui.home.HomeTab,
                LibraryTab,
                UpdatesTab,
                HistoriesTab,
                TrackingTab,
                BrowseTab,
                MoreTab,
            ).apply { remove(this@NavStyle.moreTab) }
        }
}


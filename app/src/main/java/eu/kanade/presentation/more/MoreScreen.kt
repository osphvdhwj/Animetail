package eu.kanade.presentation.more

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.VideoSettings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.model.NavStyle
import eu.kanade.presentation.design.DS
import eu.kanade.presentation.design.glassBento
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.core.common.Constants
import eu.kanade.tachiyomi.ui.more.DownloadQueueState
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A recently watched/read entry surfaced as a native preference row on More.
 */
data class RecentHistoryEntry(
    val id: Long,
    val title: String,
    val subtitle: String,
    val coverUrl: String,
    val progress: Float = 0f, // 0.0 to 1.0
    val isAnime: Boolean = true,
    val onClick: () -> Unit = {},
    val onResumeClick: () -> Unit = {},
)

/**
 * M3 Expressive Bento grouping card for settings and options.
 */
@Composable
private fun MoreBentoGroup(
    title: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, bottom = 6.dp),
            )
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .glassBento(
                    shape = DS.radiusBento,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.70f),
                    borderColor = DS.Glass.borderColor(0.25f),
                    elevation = DS.elevationLow,
                ),
            shape = DS.radiusBento,
            color = Color.Transparent,
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun MoreScreen(
    downloadQueueStateProvider: () -> DownloadQueueState,
    downloadedOnly: Boolean,
    onDownloadedOnlyChange: (Boolean) -> Unit,
    incognitoMode: Boolean,
    onIncognitoModeChange: (Boolean) -> Unit,
    // SY -->
    showNavUpdates: Boolean,
    showNavHistory: Boolean,
    // SY <--
    navStyle: NavStyle,
    recentHistoryProvider: () -> List<RecentHistoryEntry> = { emptyList() },
    onClickHistory: () -> Unit = {},
    onClickAlt: () -> Unit,
    onClickDownloadQueue: () -> Unit,
    onClickCategories: () -> Unit,
    onClickStats: () -> Unit,
    onClickPlayLocalVideo: () -> Unit = {},
    onClickNetworkStream: () -> Unit,
    onClickStorage: () -> Unit,
    onClickDataAndStorage: () -> Unit,
    onClickPlayerSettings: () -> Unit,
    onClickSettings: () -> Unit,
    onClickSupport: () -> Unit,
    onClickAbout: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    Scaffold { contentPadding ->
        ScrollbarLazyColumn(contentPadding = contentPadding) {
            item {
                LogoHeader(
                    iconPadding = PaddingValues(vertical = 24.dp),
                )
            }
            item {
                eu.kanade.presentation.more.components.AnimetailFeatureHubScreen(
                    onNavigateToMultiSync = onClickStats,
                    onNavigateToHabitTracker = onClickStats,
                    onNavigateToForum = onClickStats,
                    onNavigateToWebsitePortal = onClickCategories,
                    onNavigateToDownloadManager = onClickDownloadQueue,
                    onNavigateToLNReader = onClickPlayerSettings,
                    onNavigateToStaffSheet = onClickStats,
                )
            }
            val recentHistory = recentHistoryProvider()
            if (recentHistory.isNotEmpty()) {
                item {
                    eu.kanade.presentation.history.components.YouTubeRecentHistoryRail(
                        historyItems = recentHistory.map { entry ->
                            eu.kanade.presentation.history.components.RecentHistoryEntry(
                                id = entry.id,
                                title = entry.title,
                                subtitle = entry.subtitle,
                                coverUrl = entry.coverUrl,
                                progress = entry.progress,
                                isAnime = entry.isAnime,
                                onClick = entry.onClick,
                                onResumeClick = entry.onResumeClick,
                            )
                        },
                        onViewAllClick = onClickHistory,
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Preferences Bento Group
            item(key = "more_prefs_group") {
                MoreBentoGroup(title = stringResource(TLMR.strings.more_group_preferences)) {
                    SwitchPreferenceWidget(
                        title = stringResource(MR.strings.label_downloaded_only),
                        subtitle = stringResource(MR.strings.downloaded_only_summary),
                        icon = Icons.Outlined.CloudOff,
                        checked = downloadedOnly,
                        onCheckedChanged = onDownloadedOnlyChange,
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    SwitchPreferenceWidget(
                        title = stringResource(MR.strings.pref_incognito_mode),
                        subtitle = stringResource(AYMR.strings.pref_incognito_mode_summary),
                        icon = ImageVector.vectorResource(R.drawable.ic_glasses_24dp),
                        checked = incognitoMode,
                        onCheckedChanged = onIncognitoModeChange,
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    TextPreferenceWidget(
                        title = navStyle.moreTab.options.title,
                        icon = navStyle.moreIcon,
                        onPreferenceClick = onClickAlt,
                    )
                }
            }

            // Library & Data Bento Group
            item(key = "more_library_data_group") {
                MoreBentoGroup(title = stringResource(TLMR.strings.more_group_library_data)) {
                    val downloadQueueState = downloadQueueStateProvider()
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.label_download_queue),
                        subtitle = when (downloadQueueState) {
                            DownloadQueueState.Stopped -> null

                            is DownloadQueueState.Paused -> {
                                val pending = downloadQueueState.pending
                                if (pending == 0) {
                                    stringResource(MR.strings.paused)
                                } else {
                                    "${stringResource(MR.strings.paused)} • ${
                                        pluralStringResource(
                                            MR.plurals.download_queue_summary,
                                            count = pending,
                                            pending,
                                        )
                                    }"
                                }
                            }

                            is DownloadQueueState.Downloading -> {
                                val pending = downloadQueueState.pending
                                pluralStringResource(
                                    MR.plurals.download_queue_summary,
                                    count = pending,
                                    pending,
                                )
                            }
                        },
                        icon = Icons.Outlined.GetApp,
                        onPreferenceClick = onClickDownloadQueue,
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    TextPreferenceWidget(
                        title = stringResource(AYMR.strings.general_categories),
                        icon = Icons.AutoMirrored.Outlined.Label,
                        onPreferenceClick = onClickCategories,
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.label_stats),
                        icon = Icons.Outlined.QueryStats,
                        onPreferenceClick = onClickStats,
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    TextPreferenceWidget(
                        title = stringResource(TLMR.strings.local_video_title),
                        subtitle = stringResource(TLMR.strings.local_video_summary),
                        icon = Icons.Outlined.VideoLibrary,
                        onPreferenceClick = onClickPlayLocalVideo,
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    TextPreferenceWidget(
                        title = stringResource(TLMR.strings.network_stream),
                        subtitle = stringResource(TLMR.strings.network_stream_summary),
                        icon = Icons.Outlined.Link,
                        onPreferenceClick = onClickNetworkStream,
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.label_data_storage),
                        icon = Icons.Outlined.Storage,
                        onPreferenceClick = onClickDataAndStorage,
                    )
                }
            }

            // Embedded MAL Community & Forum Discussions (from MALClient)
            item {
                eu.kanade.presentation.more.stats.components.MALCommunityForumScreen(
                    topics = listOf(
                        eu.kanade.presentation.more.stats.components.ForumTopicItem(1L, "Weekly Anime Episode Discussion & Recommendations Thread", "MALCommunity", "Anime Discussion", 142, "2h ago"),
                        eu.kanade.presentation.more.stats.components.ForumTopicItem(2L, "Top Manga & Webtoon Recommendations 2026", "AnimetailStaff", "Manga Discussion", 89, "5h ago"),
                        eu.kanade.presentation.more.stats.components.ForumTopicItem(3L, "Light Novel Scraper & Reading Engine Updates", "LNReaderApp", "News & Updates", 45, "1d ago"),
                    ),
                    onTopicClick = { topic ->
                        uriHandler.openUri("https://myanimelist.net/forum/?topicid=${topic.id}")
                    },
                )
            }

            // Settings & Support Bento Group
            item(key = "more_settings_group") {
                MoreBentoGroup(title = stringResource(TLMR.strings.more_group_settings)) {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.label_settings),
                        icon = Icons.Outlined.Settings,
                        onPreferenceClick = onClickSettings,
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    TextPreferenceWidget(
                        title = stringResource(AYMR.strings.label_player_settings),
                        icon = Icons.Outlined.VideoSettings,
                        onPreferenceClick = onClickPlayerSettings,
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.pref_category_about),
                        icon = Icons.Outlined.Info,
                        onPreferenceClick = onClickAbout,
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.label_help),
                        icon = Icons.AutoMirrored.Outlined.HelpOutline,
                        onPreferenceClick = { uriHandler.openUri(Constants.URL_HELP) },
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(88.dp))
            }
        }
    }
}

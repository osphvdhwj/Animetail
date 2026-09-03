package eu.kanade.tachiyomi.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.home.HomeFeedScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.discover.TrackingTab
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import mihon.app.di.appGraph
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import androidx.compose.runtime.collectAsState
import tachiyomi.presentation.core.util.collectAsState

data object HomeTab : Tab {
    private fun readResolve(): Any = HomeTab

    val openSettingsSheetEvent = Channel<Unit>()
    val activeSubTab = MutableStateFlow(0) // 0: For You (Feed), 1: Discover & Trackers

    suspend fun requestOpenSettingsSheet() {
        openSettingsSheetEvent.send(Unit)
    }

    override val options: TabOptions
        @Composable
        get() {
            return TabOptions(
                index = 0u,
                title = stringResource(MR.strings.label_home),
                icon = rememberVectorPainter(Icons.Outlined.Home),
            )
        }

    @Composable
    override fun isEnabled(): Boolean {
        val context = LocalContext.current
        val uiPreferences = remember { context.appGraph.uiPreferences }
        val showHomeTab by uiPreferences.showHomeTab.collectAsState()
        return showHomeTab
    }

    override suspend fun onReselect(navigator: Navigator) {
        if (activeSubTab.value != 0) {
            activeSubTab.value = 0
        } else {
            requestOpenSettingsSheet()
        }
    }

    @Composable
    override fun Content() {
        val subTab by activeSubTab.collectAsState()

        Box(modifier = Modifier.fillMaxSize()) {
            when (subTab) {
                0 -> {
                    val screenModel = metroViewModel<HomeFeedScreenModel>()
                    HomeFeedScreen(screenModel = screenModel)
                }
                1 -> {
                    TrackingTab.Content()
                }
            }
        }
    }
}

@Composable
fun HomeTabSwitcher(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Surface(
                onClick = { onSelectTab(0) },
                shape = RoundedCornerShape(50),
                color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Home,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "For You",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }

            Surface(
                onClick = { onSelectTab(1) },
                shape = RoundedCornerShape(50),
                color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Explore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Discover",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

package eu.kanade.presentation.browse.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import eu.kanade.presentation.design.DS
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Website-style portal header for the Browse sources screens: a compact gradient
 * hero identifying the catalogue, followed by an aligned grid of labeled quick
 * action tiles (Global Search, Extensions, Web Portals, Feed, Migrate, Filter).
 *
 * Tiles whose callbacks are null are omitted, so the grid only advertises
 * destinations that are actually reachable in the current tab configuration.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrowsePortalHeader(
    isAnime: Boolean,
    onGlobalSearchClick: () -> Unit,
    onExtensionsClick: () -> Unit,
    onWebPortalsClick: (() -> Unit)? = null,
    onFeedClick: (() -> Unit)? = null,
    onMigrateClick: (() -> Unit)? = null,
    onFilterClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Compact hero strip
        Surface(
            shape = DS.radiusHero,
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DS.radiusHero)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                            ),
                        ),
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Explore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column {
                        Text(
                            text = if (isAnime) "Anime Catalogues" else "Manga Catalogues",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = if (isAnime) {
                                "Stream and discover anime across installed sources"
                            } else {
                                "Read and discover manga across installed sources"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        // Quick action tile grid — website category cards
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 3,
        ) {
            BrowsePortalTile(
                icon = Icons.Outlined.TravelExplore,
                label = "Global Search",
                onClick = onGlobalSearchClick,
                modifier = Modifier.weight(1f),
            )
            BrowsePortalTile(
                icon = Icons.Outlined.Extension,
                label = "Extensions",
                onClick = onExtensionsClick,
                modifier = Modifier.weight(1f),
            )
            if (onWebPortalsClick != null) {
                BrowsePortalTile(
                    icon = Icons.Outlined.Public,
                    label = "Web Portals",
                    onClick = onWebPortalsClick,
                    modifier = Modifier.weight(1f),
                )
            }
            if (onFeedClick != null) {
                BrowsePortalTile(
                    icon = Icons.Outlined.RssFeed,
                    label = "Feed",
                    onClick = onFeedClick,
                    modifier = Modifier.weight(1f),
                )
            }
            if (onMigrateClick != null) {
                BrowsePortalTile(
                    icon = Icons.Outlined.SwapHoriz,
                    label = "Migrate",
                    onClick = onMigrateClick,
                    modifier = Modifier.weight(1f),
                )
            }
            if (onFilterClick != null) {
                BrowsePortalTile(
                    icon = Icons.Outlined.FilterList,
                    label = "Filter",
                    onClick = onFilterClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** A single quick-action category tile — shared [QuickActionTile] design. */
@Composable
private fun BrowsePortalTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    eu.kanade.presentation.components.QuickActionTile(
        icon = icon,
        label = label,
        onClick = onClick,
        modifier = modifier,
    )
}

/**
 * Empty Sources Discovery Card with call-to-action button to install extensions.
 */
@Composable
fun EmptySourcesDiscoveryCard(
    isAnime: Boolean,
    onInstallExtensionsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = DS.radiusHero,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Extension,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
            }

            Text(
                text = if (isAnime) "No Anime Sources Installed" else "No Manga Sources Installed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = if (isAnime) {
                    "Install extensions from the Extensions tab to stream and browse anime titles."
                } else {
                    "Install extensions from the Extensions tab to read and browse manga titles."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )

            Button(
                onClick = onInstallExtensionsClick,
                shape = DS.radiusCard,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Extension,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Browse Extensions")
            }
        }
    }
}

package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import eu.kanade.presentation.design.DS
import eu.kanade.presentation.design.glassBento
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Storage & Download Footprint Micro-Card.
 *
 * Displays downloaded count, local files count, and storage distribution.
 */
@Composable
fun StorageFootprintCard(
    isAnime: Boolean,
    downloadCount: Int,
    localTitlesCount: Int,
    globalUpdateCount: Int,
    totalTitles: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = DS.radiusHero,
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .glassBento(
                shape = DS.radiusHero,
                containerColor = DS.Glass.containerColorHigh(0.85f),
                borderColor = DS.Glass.borderColor(0.35f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderSpecial,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column {
                    Text(
                        text = "Storage & Local Footprint",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Downloaded media and local file statistics",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                }
            }

            // Metric Triplet
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                FootprintItem(
                    value = downloadCount.toString(),
                    label = if (isAnime) "Downloaded Ep" else "Downloaded Ch",
                )
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .padding(horizontal = 4.dp)
                )
                FootprintItem(
                    value = localTitlesCount.toString(),
                    label = "Local Titles",
                )
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .padding(horizontal = 4.dp)
                )
                FootprintItem(
                    value = globalUpdateCount.toString(),
                    label = "Auto-Updates",
                )
            }
        }
    }
}

@Composable
private fun FootprintItem(
    value: String,
    label: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
        )
    }
}

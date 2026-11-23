package eu.kanade.presentation.more.stats.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val FORMAT_PALETTE = listOf(
    Color(0xFF6366F1), // Indigo
    Color(0xFF10B981), // Emerald
    Color(0xFFF59E0B), // Amber
    Color(0xFFEC4899), // Pink
    Color(0xFF8B5CF6), // Purple
    Color(0xFF06B6D4), // Cyan
)

/**
 * Format & Release Type Deep-Dive Component.
 *
 * Visualizes the breakdown of media types in the user's library
 * (TV Series, Movies, OVAs, ONAs, Specials for Anime;
 * Manga, Manhwa, Manhua, Light Novels, One-shots for Manga).
 */
@Composable
fun FormatDistributionCard(
    formatDistribution: Map<String, Int>,
    totalCount: Int,
    isAnime: Boolean,
    modifier: Modifier = Modifier,
) {
    if (formatDistribution.isEmpty() || totalCount == 0) return

    val sortedFormats = remember(formatDistribution) {
        formatDistribution.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
    }

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
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Category,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column {
                    Text(
                        text = if (isAnime) "Anime Format Breakdown" else "Manga Release Formats",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Distribution across release types in library",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                }
            }

            // Segmented Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
            ) {
                sortedFormats.forEachIndexed { index, entry ->
                    val color = FORMAT_PALETTE[index % FORMAT_PALETTE.size]
                    val weight = (entry.value.toFloat() / totalCount).coerceAtLeast(0.01f)
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .height(10.dp)
                            .background(color),
                    )
                }
            }

            // Format Rows
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sortedFormats.forEachIndexed { index, (format, count) ->
                    val color = FORMAT_PALETTE[index % FORMAT_PALETTE.size]
                    val pct = if (totalCount > 0) ((count.toFloat() / totalCount) * 100).toInt() else 0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(color),
                            )
                            Text(
                                text = format,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Text(
                            text = "$count ($pct%)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = color,
                        )
                    }
                }
            }
        }
    }
}

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
import androidx.compose.material.icons.outlined.DonutLarge
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private data class StatusItemData(
    val label: String,
    val count: Int,
    val color: Color,
)

/**
 * Status Matrix Detailed Breakdown Card.
 *
 * Displays individual status metrics (Watching, Completed, Plan to Watch, On Hold, Dropped)
 * with counts, percentages, and visual bars.
 */
@Composable
fun StatusMatrixCard(
    isAnime: Boolean,
    watchingCount: Int,
    completedCount: Int,
    planToWatchCount: Int,
    onHoldCount: Int,
    droppedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        StatusItemData(if (isAnime) "Watching" else "Reading", watchingCount, Color(0xFF6366F1)),
        StatusItemData("Completed", completedCount, Color(0xFF10B981)),
        StatusItemData(if (isAnime) "Plan to Watch" else "Plan to Read", planToWatchCount, Color(0xFF8B5CF6)),
        StatusItemData("On Hold", onHoldCount, Color(0xFFF59E0B)),
        StatusItemData("Dropped", droppedCount, Color(0xFFEF4444)),
    )

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
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DonutLarge,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column {
                    Text(
                        text = "Library Status Matrix",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Micro distribution across all 5 progress states",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                }
            }

            // Status List
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEachIndexed { index, status ->
                    val fraction = if (totalCount > 0) (status.count.toFloat() / totalCount).coerceIn(0f, 1f) else 0f
                    val percentage = (fraction * 100).toInt()

                    var targetFraction by remember(status.count) { mutableFloatStateOf(0f) }
                    LaunchedEffect(status.count) {
                        delay(60L + index * 40L)
                        targetFraction = fraction
                    }
                    val animatedFraction by animateFloatAsState(
                        targetValue = targetFraction,
                        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                        label = "statusProgress_${status.label}",
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                        .background(status.color),
                                )
                                Text(
                                    text = status.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Text(
                                text = "${status.count} ($percentage%)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = status.color,
                            )
                        }

                        LinearProgressIndicator(
                            progress = { animatedFraction },
                            color = status.color,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                        )
                    }
                }
            }
        }
    }
}

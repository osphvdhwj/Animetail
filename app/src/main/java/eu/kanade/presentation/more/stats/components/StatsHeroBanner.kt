package eu.kanade.presentation.more.stats.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Full-width gradient hero banner displayed at the very top of the Stats screen.
 *
 * Shows the primary collection count with an animated count-up effect,
 * total watch/read time summary, and a prominent icon.
 *
 * @param count Total items in library (anime or manga count).
 * @param label Short label displayed below the count, e.g. "Anime in your library".
 * @param isAnime Whether this is for the Anime tab (affects icon and gradient).
 * @param totalDurationMs Total watch/read duration in milliseconds for the subtitle.
 */
@Composable
fun StatsHeroBanner(
    count: Int,
    label: String,
    isAnime: Boolean,
    totalDurationMs: Long,
    modifier: Modifier = Modifier,
) {
    // Derive friendly time summary
    val totalHours = totalDurationMs / 3_600_000L
    val totalDays = totalHours / 24L
    val timeSummary = when {
        totalDays >= 1 -> "$totalDays days ${totalHours % 24}h watched"
        totalHours >= 1 -> "$totalHours hours watched"
        else -> "${totalDurationMs / 60_000L} minutes watched"
    }

    // Animated count-up from 0 to real count
    var targetCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(count) {
        delay(120L)
        targetCount = count
    }
    val animatedCount by animateIntAsState(
        targetValue = targetCount,
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label = "heroCount",
    )

    val gradient = if (isAnime) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.tertiary,
            ),
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.primary,
            ),
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(gradient)
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = animatedCount.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 64.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f),
                ) {
                    Text(
                        text = timeSummary,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.95f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Large icon badge
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isAnime) Icons.Outlined.PlayCircle else Icons.Outlined.AutoStories,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
    }
}

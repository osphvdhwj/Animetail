package eu.kanade.presentation.more.stats.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Modern Material 3 1-10 Score Distribution Bar Chart.
 * Modeled after AniList, Dantotsu, AL-chan, and Nekome clients.
 */
@Composable
fun ScoreDistributionBarChart(
    scoreDistribution: Map<Int, Int>,
    meanScore: Double,
    modifier: Modifier = Modifier,
) {
    val maxCount = remember(scoreDistribution) {
        (scoreDistribution.values.maxOrNull() ?: 1).coerceAtLeast(1).toFloat()
    }
    val totalScored = remember(scoreDistribution) {
        scoreDistribution.values.sum()
    }

    val meanScoreFormatted = if (meanScore.isNaN()) {
        "—"
    } else {
        String.format(java.util.Locale.getDefault(), "%.1f", meanScore)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Mean Rating",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "★ $meanScoreFormatted / 10",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (totalScored > 0) {
                Text(
                    text = "$totalScored scored",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            (1..10).forEach { score ->
                val count = scoreDistribution[score] ?: 0
                val ratio = if (totalScored > 0) count / maxCount else 0f

                var targetFraction by remember(score, count) { mutableFloatStateOf(0f) }
                LaunchedEffect(score, count) {
                    delay(30L + score * 30L)
                    targetFraction = ratio
                }

                val animatedHeight by animateFloatAsState(
                    targetValue = targetFraction,
                    animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                    label = "scoreBar_$score",
                )

                val barColor = when (score) {
                    in 1..4 -> MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                    in 5..6 -> Color(0xFFF59E0B)
                    in 7..8 -> MaterialTheme.colorScheme.primary
                    else -> Color(0xFF10B981)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Text(
                        text = if (count > 0) count.toString() else "",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction = animatedHeight.coerceIn(0.04f, 1f))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(if (count > 0) barColor else MaterialTheme.colorScheme.surfaceContainerHighest),
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = score.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = if (count > 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (count > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

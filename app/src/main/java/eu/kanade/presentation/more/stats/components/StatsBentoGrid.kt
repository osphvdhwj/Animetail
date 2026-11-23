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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LibraryAddCheck
import androidx.compose.material.icons.outlined.Pending
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Videocam
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Bento-style 2×3 mini-cards grid showing micro stats at a glance.
 *
 * For Anime: episodes watched, downloads, completed %, mean score, watching now, backlog.
 * For Manga: chapters read, downloads, completed %, mean score, reading now, backlog.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsBentoGrid(
    isAnime: Boolean,
    // Shared
    downloadCount: Int,
    completedCount: Int,
    totalCount: Int,
    meanScore: Double,
    activeCount: Int,   // watching/reading now
    backlogCount: Int,  // plan to watch/read
    // Anime-specific
    episodesWatched: Int = 0,
    totalEpisodes: Int = 0,
    // Manga-specific
    chaptersRead: Int = 0,
    totalChapters: Int = 0,
    modifier: Modifier = Modifier,
) {
    val completionPct = if (totalCount > 0) (completedCount.toFloat() / totalCount) * 100f else 0f
    val meanScoreStr = if (meanScore.isNaN() || meanScore <= 0.0) "—" else String.format(Locale.US, "%.1f", meanScore)

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        maxItemsInEachRow = 3,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Card 1: Episodes/Chapters consumed
        BentoMiniCard(
            value = if (isAnime) episodesWatched.toFormattedString() else chaptersRead.toFormattedString(),
            label = if (isAnime) "Episodes\nWatched" else "Chapters\nRead",
            icon = if (isAnime) Icons.Outlined.Videocam else Icons.Outlined.LibraryAddCheck,
            iconColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )

        // Card 2: Downloads
        BentoMiniCard(
            value = downloadCount.toFormattedString(),
            label = "Downloaded",
            icon = Icons.Outlined.CloudDownload,
            iconColor = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )

        // Card 3: Completion %
        BentoMiniCard(
            value = "${completionPct.toInt()}%",
            label = "Complete",
            icon = Icons.Outlined.EmojiEvents,
            iconColor = Color(0xFF10B981),
            modifier = Modifier.weight(1f),
            showProgress = true,
            progressFraction = completionPct / 100f,
            progressColor = Color(0xFF10B981),
        )

        // Card 4: Mean Score
        BentoMiniCard(
            value = "★ $meanScoreStr",
            label = "Mean Score",
            icon = Icons.Outlined.Star,
            iconColor = Color(0xFFFFB300),
            modifier = Modifier.weight(1f),
        )

        // Card 5: Watching/Reading now
        BentoMiniCard(
            value = activeCount.toFormattedString(),
            label = if (isAnime) "Watching\nNow" else "Reading\nNow",
            icon = Icons.Outlined.Videocam,
            iconColor = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f),
        )

        // Card 6: Backlog
        BentoMiniCard(
            value = backlogCount.toFormattedString(),
            label = "In\nBacklog",
            icon = Icons.Outlined.Pending,
            iconColor = Color(0xFF8B5CF6),
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * A single bento-style mini stat card with icon, big value, label, and optional progress bar.
 */
@Composable
private fun BentoMiniCard(
    value: String,
    label: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    showProgress: Boolean = false,
    progressFraction: Float = 0f,
    progressColor: Color = MaterialTheme.colorScheme.primary,
) {
    var targetProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(progressFraction) {
        delay(200L)
        targetProgress = progressFraction
    }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "bentoProgress",
    )

    Surface(
        shape = DS.radiusCard,
        color = Color.Transparent,
        modifier = modifier.glassBento(
            shape = DS.radiusCard,
            containerColor = DS.Glass.containerColorHigh(0.85f),
            borderColor = DS.Glass.borderColor(0.35f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Icon badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp),
                )
            }

            // Big value
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                maxLines = 1,
            )

            // Label
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 13.sp,
                textAlign = TextAlign.Start,
            )

            // Optional progress bar
            if (showProgress) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    LinearProgressIndicator(
                        progress = { animatedProgress.coerceIn(0f, 1f) },
                        color = progressColor,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(CircleShape),
                    )
                }
            }
        }
    }
}

/** Format large integers compactly: 1200 → "1.2K", 1500000 → "1.5M" */
private fun Int.toFormattedString(): String = when {
    this >= 1_000_000 -> String.format(Locale.US, "%.1fM", this / 1_000_000.0)
    this >= 10_000 -> "${this / 1000}K"
    this >= 1_000 -> String.format(Locale.US, "%.1fK", this / 1000.0)
    else -> this.toString()
}

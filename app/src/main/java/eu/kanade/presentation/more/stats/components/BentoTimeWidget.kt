package eu.kanade.presentation.more.stats.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun BentoTimeWidget(
    totalDurationMs: Long,
    isAnime: Boolean,
    modifier: Modifier = Modifier,
) {
    val totalMinutes = totalDurationMs / 60_000L
    val days = (totalMinutes / 1440).toInt()
    val hours = ((totalMinutes % 1440) / 60).toInt()
    val minutes = (totalMinutes % 60).toInt()

    var targetDays by remember { mutableIntStateOf(0) }
    var targetHours by remember { mutableIntStateOf(0) }
    var targetMinutes by remember { mutableIntStateOf(0) }

    LaunchedEffect(totalDurationMs) {
        delay(60L)
        targetDays = days
        targetHours = hours
        targetMinutes = minutes
    }

    val animatedDays by animateIntAsState(
        targetValue = targetDays,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "animatedDays",
    )
    val animatedHours by animateIntAsState(
        targetValue = targetHours,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "animatedHours",
    )
    val animatedMinutes by animateIntAsState(
        targetValue = targetMinutes,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "animatedMinutes",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isAnime) Icons.Outlined.HourglassTop else Icons.Outlined.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp),
            )
        }

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            TimeBlock(value = animatedDays.toString(), label = "Days")
            TimeBlock(value = animatedHours.toString(), label = "Hours")
            TimeBlock(value = animatedMinutes.toString(), label = "Minutes")
        }
    }
}

@Composable
private fun TimeBlock(
    value: String,
    label: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
        )
    }
}

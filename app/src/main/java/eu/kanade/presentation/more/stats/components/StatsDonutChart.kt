package eu.kanade.presentation.more.stats.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class DonutSegment(
    val label: String,
    val count: Int,
    val color: Color,
)

@Composable
fun StatsDonutChart(
    segments: List<DonutSegment>,
    modifier: Modifier = Modifier,
    centerLabel: String = "Total",
    strokeWidth: Dp = 18.dp,
) {
    val total = remember(segments) { segments.sumOf { it.count }.coerceAtLeast(0) }

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(segments) {
        delay(80L)
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 900,
                easing = FastOutSlowInEasing,
            ),
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Donut Canvas with Center Text
        Box(
            modifier = Modifier.size(130.dp),
            contentAlignment = Alignment.Center,
        ) {
            val progress = animationProgress.value
            Canvas(modifier = Modifier.size(130.dp)) {
                val strokeWidthPx = strokeWidth.toPx()
                val halfStroke = strokeWidthPx / 2f
                val arcSize = Size(
                    width = size.width - strokeWidthPx,
                    height = size.height - strokeWidthPx,
                )
                val topLeft = Offset(halfStroke, halfStroke)

                if (total == 0) {
                    drawOval(
                        color = Color.Gray.copy(alpha = 0.2f),
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx),
                    )
                } else {
                    var currentStartAngle = -90f
                    segments.forEach { segment ->
                        if (segment.count > 0) {
                            val sweepAngle = (segment.count.toFloat() / total) * 360f * progress
                            if (sweepAngle > 0f) {
                                drawArc(
                                    color = segment.color,
                                    startAngle = currentStartAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Butt),
                                )
                                currentStartAngle += sweepAngle
                            }
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = total.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = centerLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                )
            }
        }

        // Legend Column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            segments.forEach { segment ->
                val count = segment.count
                val isZero = count == 0
                val percentage = if (total > 0) ((count.toFloat() / total) * 100).toInt() else 0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .padding(vertical = 2.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(segment.color.copy(alpha = if (isZero) 0.35f else 1f)),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = segment.label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isZero) FontWeight.Normal else FontWeight.Medium,
                        color = if (isZero) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "$count ($percentage%)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isZero) FontWeight.Normal else FontWeight.Bold,
                        color = if (isZero) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }
        }
    }
}

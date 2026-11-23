package eu.kanade.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.design.DS
import androidx.compose.foundation.border

/**
 * Data item representing an individual action inside a Speed Dial stack.
 */
data class SpeedDialItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val containerColor: Color = Color.Unspecified,
    val contentColor: Color = Color.Unspecified,
)

/**
 * WhatsApp 2026-style Contextual Floating Action Button with Speed Dial.
 *
 * Features:
 * - Collapses from Extended FAB (with label) to compact circle on scroll.
 * - Long-press or tap to open an animated vertical stack of quick-action pills.
 * - Rotating morph icon from primary action to Close (✕) when opened.
 * - Tap-outside scrim dismisser.
 */
@Composable
fun SpeedDialFab(
    mainIcon: ImageVector,
    mainLabel: String,
    items: List<SpeedDialItem>,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = true,
    onMainClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    var isDialOpen by remember { mutableStateOf(false) }

    val rotationAngle by animateFloatAsState(
        targetValue = if (isDialOpen) 45f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "fabRotation",
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd,
    ) {
        // Scrim overlay when dial is open
        if (isDialOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { isDialOpen = false },
                    ),
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Speed Dial Mini Actions
            AnimatedVisibility(
                visible = isDialOpen,
                enter = fadeIn(tween(200)) + scaleIn(tween(200, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(150)) + scaleOut(tween(150, easing = FastOutSlowInEasing)),
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 4.dp, end = 4.dp),
                ) {
                    items.forEachIndexed { index, item ->
                        SpeedDialActionRow(
                            item = item,
                            onDismiss = { isDialOpen = false },
                        )
                    }
                }
            }

            // Main Primary FAB
            if (isExpanded && !isDialOpen) {
                ExtendedFloatingActionButton(
                    text = {
                        Text(
                            text = mainLabel,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = mainIcon,
                            contentDescription = mainLabel,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    onClick = {
                        if (items.isNotEmpty() && onMainClick == null) {
                            isDialOpen = !isDialOpen
                        } else {
                            onMainClick?.invoke()
                        }
                    },
                    shape = DS.radiusHero,
                    containerColor = containerColor,
                    contentColor = contentColor,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 3.dp,
                        pressedElevation = 6.dp,
                    ),
                )
            } else {
                FloatingActionButton(
                    onClick = {
                        if (items.isNotEmpty()) {
                            isDialOpen = !isDialOpen
                        } else {
                            onMainClick?.invoke()
                        }
                    },
                    shape = CircleShape,
                    containerColor = if (isDialOpen) MaterialTheme.colorScheme.surfaceContainerHighest else containerColor,
                    contentColor = if (isDialOpen) MaterialTheme.colorScheme.onSurface else contentColor,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 7.dp,
                    ),
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        imageVector = if (isDialOpen) Icons.Default.Close else mainIcon,
                        contentDescription = mainLabel,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(if (isDialOpen) 0f else rotationAngle),
                    )
                }
            }
        }
    }
}

/**
 * Single action row in the speed dial stack containing an action label chip and a mini FAB.
 */
@Composable
private fun SpeedDialActionRow(
    item: SpeedDialItem,
    onDismiss: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Label Chip
        Surface(
            shape = DS.radiusMedium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
            shadowElevation = 2.dp,
            modifier = Modifier
                .clip(DS.radiusMedium).border(DS.strokeHairline, DS.Glass.borderColor(0.30f), DS.radiusMedium)
                .clickable {
                    item.onClick()
                    onDismiss()
                },
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }

        // Mini FAB
        SmallFloatingActionButton(
            onClick = {
                item.onClick()
                onDismiss()
            },
            shape = CircleShape,
            containerColor = if (item.containerColor != Color.Unspecified) {
                item.containerColor
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            contentColor = if (item.contentColor != Color.Unspecified) {
                item.contentColor
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

package eu.kanade.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Contextual Floating Action Button modes for quick actions across primary tabs and screens.
 */
enum class FabContextMode {
    OPEN_LOCAL_VIDEO,
    TOGGLE_SWIPE_DECK,
    RESUME_RECENT,
    GLOBAL_SEARCH,
    ADD_CATEGORY,
    ADD_TO_LIBRARY,
    SPEED_DIAL,
    HIDDEN,
}

/**
 * Context-aware WhatsApp 2026-style Floating Action Button that gracefully animates visibility,
 * expands/collapses with scroll state, and optionally supports Speed Dial actions.
 */
@Composable
fun AppFloatingActionButton(
    mode: FabContextMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    isSwipeDeckActive: Boolean = false,
    isExpanded: Boolean = false,
    speedDialItems: List<SpeedDialItem> = emptyList(),
) {
    if (mode == FabContextMode.SPEED_DIAL && speedDialItems.isNotEmpty()) {
        SpeedDialFab(
            mainIcon = Icons.Outlined.Add,
            mainLabel = "Actions",
            items = speedDialItems,
            modifier = modifier,
            isExpanded = isExpanded,
            onMainClick = onClick,
        )
        return
    }

    AnimatedVisibility(
        visible = visible && mode != FabContextMode.HIDDEN,
        enter = scaleIn(animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f)) + fadeIn(),
        exit = scaleOut(animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f)) + fadeOut(),
        modifier = modifier,
    ) {
        val (icon: ImageVector, label: String) = when (mode) {
            FabContextMode.OPEN_LOCAL_VIDEO -> Icons.Outlined.FolderOpen to "Local Video"
            FabContextMode.TOGGLE_SWIPE_DECK -> if (isSwipeDeckActive) Icons.Outlined.DynamicFeed to "Feed" else Icons.Outlined.Style to "Swipe Deck"
            FabContextMode.RESUME_RECENT -> Icons.Outlined.PlayArrow to "Resume"
            FabContextMode.GLOBAL_SEARCH -> Icons.Outlined.Search to "Search"
            FabContextMode.ADD_CATEGORY -> Icons.Outlined.Add to "New Category"
            FabContextMode.ADD_TO_LIBRARY -> Icons.Outlined.Add to "Add Title"
            FabContextMode.SPEED_DIAL -> Icons.Outlined.Add to "Actions"
            FabContextMode.HIDDEN -> Icons.Outlined.VideoLibrary to ""
        }

        if (isExpanded) {
            ExtendedFloatingActionButton(
                text = {
                    Text(
                        text = label,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(24.dp),
                    )
                },
                onClick = onClick,
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 3.dp,
                    pressedElevation = 6.dp,
                ),
            )
        } else {
            FloatingActionButton(
                onClick = onClick,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 3.dp,
                    pressedElevation = 6.dp,
                ),
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

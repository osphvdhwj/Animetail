package eu.kanade.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class FabContextMode {
    OPEN_LOCAL_VIDEO,
    TOGGLE_SWIPE_DECK,
    RESUME_RECENT,
    HIDDEN,
}

@Composable
fun AppFloatingActionButton(
    mode: FabContextMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSwipeDeckActive: Boolean = false,
) {
    AnimatedVisibility(
        visible = mode != FabContextMode.HIDDEN,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier,
    ) {
        val (icon: ImageVector, description: String) = when (mode) {
            FabContextMode.OPEN_LOCAL_VIDEO -> Icons.Outlined.FolderOpen to "Open Local Video"
            FabContextMode.TOGGLE_SWIPE_DECK -> if (isSwipeDeckActive) Icons.Outlined.DynamicFeed to "Switch to Feed" else Icons.Outlined.Style to "Swipe Deck"
            FabContextMode.RESUME_RECENT -> Icons.Outlined.PlayArrow to "Resume"
            FabContextMode.HIDDEN -> Icons.Outlined.VideoLibrary to "Action"
        }

        FloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .padding(end = 16.dp, bottom = 16.dp)
                .size(56.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

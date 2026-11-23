package eu.kanade.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.design.DS
import eu.kanade.presentation.design.glassIsland

/**
 * Animetail M3 Expressive Floating Island Navigation Bar.
 *
 * Elevates the primary bottom navigation into a floating pill island with
 * a glassmorphic translucent surface, subtle border stroke, and smooth spring physics.
 */
@Composable
fun FloatingIslandNavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = DS.Glass.containerColor(0.88f),
    contentColor: Color = MaterialTheme.colorScheme.contentColorFor(containerColor),
    windowInsets: WindowInsets = NavigationBarDefaults.windowInsets,
    elevation: Dp = DS.elevationIsland,
    content: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(windowInsets)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .glassIsland(
                    shape = DS.radiusPill,
                    containerColor = containerColor,
                    borderColor = DS.Glass.borderColor(0.40f),
                    elevation = elevation,
                ),
            color = Color.Transparent,
            contentColor = contentColor,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .selectableGroup(),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

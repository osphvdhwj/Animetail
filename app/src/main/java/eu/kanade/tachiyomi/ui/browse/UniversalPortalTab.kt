package eu.kanade.tachiyomi.ui.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import eu.kanade.presentation.browse.components.UniversalWebsitePortalScreen
import eu.kanade.presentation.components.TabContent
import tachiyomi.i18n.MR

/**
 * Sub-tab in Browse presenting curated online anime & manga web portals and databases.
 */
@Composable
fun Screen.universalPortalTab(): TabContent {
    return TabContent(
        titleRes = MR.strings.label_sources,
        content = { contentPadding, _ ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                UniversalWebsitePortalScreen(
                    onWebsiteClick = { /* Handled internally by UniversalWebsitePortalScreen */ },
                )
            }
        },
    )
}

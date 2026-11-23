package eu.kanade.tachiyomi.ui.more

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.more.NewUpdateScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.system.openInBrowser

/**
 * Screen-level composable hierarchy for NewUpdateScreen.
 *
 * Configures the scaffold, top app bars, scroll behavior,
 * and responsive content layout for this screen.
 */
class NewUpdateScreen(
    private val versionName: String,
    private val changelogInfo: String,
    private val releaseLink: String,
    private val downloadLink: String,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val viewModel =
            assistedMetroViewModel<NewUpdateScreenModel, NewUpdateScreenModel.Factory> {
                create(changelogInfo = changelogInfo, downloadLink = downloadLink)
            }

        val state by viewModel.state.collectAsState()

        NewUpdateScreen(
            versionName = versionName,
            stage = state.stage,
            downloadProgress = { state.downloadProgress },
            changelogInfo = state.changelogInfo,
            onOpenInBrowser = { context.openInBrowser(releaseLink) },
            onAcceptUpdate = {
                when (state.stage) {
                    NewUpdateScreenModel.Stage.Available, NewUpdateScreenModel.Stage.Failed -> viewModel.startDownload()
                    NewUpdateScreenModel.Stage.Downloaded -> viewModel.installUpdate()
                    else -> Unit
                }
            },
            onRejectUpdate = navigator::pop,
        )
    }
}

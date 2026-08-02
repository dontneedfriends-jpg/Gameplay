package app.gamenative.ui.screen.xserver

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.gamenative.PluviaApp
import app.gamenative.service.SteamService
import app.gamenative.ui.data.Achievement
import app.gamenative.ui.screen.library.SteamAchievementsPage
import app.gamenative.utils.ContainerUtils
import com.winlator.container.Container
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * In-game achievements overlay state. Hosted by QuickMenu (same constraint as
 * InGameContainerSettings: XServerScreen is at the dex register limit), and
 * shares the overlay pause flag with it so the game stays suspended while
 * the page is open.
 */
class InGameAchievements(
    private val container: Container,
) {
    var visible by mutableStateOf(false)
        private set
    var achievements by mutableStateOf<List<Achievement>?>(null)
        private set
    var loading by mutableStateOf(false)
        private set

    val gameName: String
        get() = container.name

    fun open() {
        InGameContainerSettings.overlayActive = true
        if (!container.suspendPolicy.equals(Container.SUSPEND_POLICY_NEVER, ignoreCase = true)) {
            PluviaApp.xEnvironment?.onPause()
            PluviaApp.isOverlayPaused = true
        }
        visible = true

        if (achievements == null && !loading) {
            loading = true
            val gameId = runCatching {
                ContainerUtils.extractGameIdFromContainerId(container.id)
            }.getOrNull()
            CoroutineScope(Dispatchers.IO).launch {
                val list = gameId?.let {
                    runCatching { SteamService.fetchAchievementsForDisplay(it) }
                        .onFailure { error -> Timber.e(error, "Failed to fetch achievements in game") }
                        .getOrNull()
                }
                achievements = list ?: emptyList()
                loading = false
            }
        }
    }

    fun close() {
        visible = false
        InGameContainerSettings.overlayActive = false
        if (!PluviaApp.isOverlayPaused) return
        if (container.suspendPolicy.equals(Container.SUSPEND_POLICY_NEVER, ignoreCase = true)) {
            PluviaApp.isOverlayPaused = false
            return
        }
        if (container.suspendPolicy.equals(Container.SUSPEND_POLICY_MANUAL, ignoreCase = true)) return
        PluviaApp.xEnvironment?.onResume()
        PluviaApp.isOverlayPaused = false
    }
}

@Composable
fun InGameAchievementsOverlay(
    state: InGameAchievements,
) {
    if (!state.visible) return

    val currentAchievements = state.achievements
    if (currentAchievements == null) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    } else {
        SteamAchievementsPage(
            gameName = state.gameName,
            achievements = currentAchievements,
            onBack = state::close,
        )
    }
}

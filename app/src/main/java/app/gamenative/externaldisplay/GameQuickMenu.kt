package app.gamenative.externaldisplay

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.gamenative.PrefManager
import app.gamenative.R
import app.gamenative.data.SteamApp
import app.gamenative.ui.component.QuickMenu
import app.gamenative.ui.data.PerformanceHudConfig
import app.gamenative.ui.screen.xserver.InGameContainerSettings
import app.gamenative.utils.rememberHasExternalDisplay
import com.winlator.container.Container
import com.winlator.renderer.GLRenderer
import com.winlator.renderer.VulkanRenderer
import com.winlator.winhandler.ProcessInfo
import com.winlator.widget.FrameRating

/**
 * Dual-display-aware wrapper around [QuickMenu]. When an external (second)
 * display is attached, the menu remains visible there while the game keeps
 * input focus. Back activates the second-display menu; Back again returns
 * focus to the game. Without a second display this renders as the usual
 * full-screen overlay.
 *
 * The close/resume callback is fired here because on the second display the
 * menu's composition is torn down with the presentation instead of running its
 * exit animation.
 */
@Composable
fun GameQuickMenu(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onItemSelected: (Int) -> Boolean,
    renderer: VulkanRenderer? = null,
    glRenderer: GLRenderer? = null,
    container: Container? = null,
    wineProcesses: List<ProcessInfo> = emptyList(),
    isWineProcessesLoading: Boolean = false,
    onToolsVisibilityChanged: (Boolean) -> Unit = {},
    onEndWineProcess: (ProcessInfo) -> Unit = {},
    isPerformanceHudEnabled: Boolean = false,
    performanceHudConfig: PerformanceHudConfig = PerformanceHudConfig(),
    fpsLimiterEnabled: Boolean = true,
    fpsLimiterTarget: Int = 60,
    fpsLimiterMax: Int = 60,
    game: SteamApp? = null,
    frameRating: FrameRating? = null,
    onPerformanceHudConfigChanged: (PerformanceHudConfig) -> Unit = {},
    onFpsLimiterEnabledChanged: (Boolean) -> Unit = {},
    onFpsLimiterChanged: (Int) -> Unit = {},
    hasPhysicalController: Boolean = false,
    isTouchscreenModeActive: Boolean = false,
    onTouchGestureSettingsClick: () -> Unit = {},
    isShooterModeActive: Boolean = false,
    onShooterModeSettingsClick: () -> Unit = {},
    activeToggleIds: Set<Int> = emptySet(),
    // LSFG hot-reload state (tab only visible when isLsfgAvailable)
    isLsfgAvailable: Boolean = false,
    lsfgMultiplier: Int = 2,
    lsfgFlowScale: Float = 0.80f,
    lsfgPerformanceMode: Boolean = true,
    onLsfgMultiplierChanged: (Int) -> Unit = {},
    onLsfgFlowScaleChanged: (Float) -> Unit = {},
    onLsfgPerformanceModeChanged: (Boolean) -> Unit = {},
    onAnimationComplete: (Boolean) -> Unit = {},
    /** Lets the menu open itself when the running game asks for its Steam invite dialog. */
    onRequestOpen: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val hasExternalDisplay = rememberHasExternalDisplay()
    val dashboardSubtitle = stringResource(R.string.second_screen_fps_target, fpsLimiterTarget)
    val dashboardTitle = game?.name.orEmpty().ifBlank { container?.name.orEmpty() }
    val dashboardImageUrl = game?.getCapsuleUrl(large = true)
        ?.takeIf { it.isNotBlank() }
        ?: game?.headerUrl.orEmpty()
    val dashboardLogoUrl = game?.let { app ->
        app.getLogoUrl(large = true)
            ?: app.getLogoUrl()
            ?: app.logoUrl.takeIf { app.logoHash.isNotBlank() }
    }.orEmpty().takeIf { PrefManager.dualScreenGameUseLogo }.orEmpty()
    val hudFpsMultiplier = if (isLsfgAvailable && lsfgMultiplier >= 2) lsfgMultiplier else 1
    val lowerPanel = remember(container?.id) {
        mutableStateOf(PrefManager.dualScreenGameDefaultPanel)
    }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            lowerPanel.value = 1
        } else if (!InGameContainerSettings.overlayActive) {
            lowerPanel.value = PrefManager.dualScreenGameDefaultPanel
        }
    }

    if (hasExternalDisplay) {
        SideEffect {
            DsHomeSecondScreen.publish(DsHomeSecondScreen.Model(
                owner = DsHomeSecondScreen.Owner.GAME,
                mode = if (lowerPanel.value == 1) {
                    DsHomeSecondScreen.Mode.QUICK_MENU
                } else {
                    DsHomeSecondScreen.Mode.GAME_DASHBOARD
                },
                dashboardTitle = dashboardTitle,
                dashboardSubtitle = dashboardSubtitle,
                dashboardImageUrl = dashboardImageUrl,
                dashboardLogoUrl = dashboardLogoUrl,
                performanceHudEnabled = isPerformanceHudEnabled,
                performanceHudConfig = performanceHudConfig,
                performanceHudFpsProvider = {
                    val raw = frameRating?.currentFPS ?: 0f
                    if (raw.isFinite()) raw.coerceAtLeast(0f) * hudFpsMultiplier else 0f
                },
                performanceHudKey = 31 * System.identityHashCode(frameRating) + hudFpsMultiplier,
                onShowDashboard = {
                    lowerPanel.value = 0
                    if (isVisible) onDismiss()
                },
                onShowMenu = {
                    lowerPanel.value = 1
                    // Force an outer state change as well: the Presentation owns
                    // a separate Compose frame clock, so changing only the state
                    // captured by this callback is not enough to reliably wake
                    // the main-display publisher on every device. The lower
                    // window remains NOT_FOCUSABLE, so controller focus still
                    // stays with the game.
                    if (!isVisible) onRequestOpen()
                },
                menuContent = {
                    QuickMenu(
                            isVisible = true,
                            onDismiss = onDismiss,
                            onItemSelected = onItemSelected,
                            renderer = renderer,
                            glRenderer = glRenderer,
                            container = container,
                            wineProcesses = wineProcesses,
                            isWineProcessesLoading = isWineProcessesLoading,
                            onToolsVisibilityChanged = { toolsVisible ->
                                onToolsVisibilityChanged(isVisible && toolsVisible)
                            },
                            onEndWineProcess = onEndWineProcess,
                            isPerformanceHudEnabled = isPerformanceHudEnabled,
                            performanceHudConfig = performanceHudConfig,
                            fpsLimiterEnabled = fpsLimiterEnabled,
                            fpsLimiterTarget = fpsLimiterTarget,
                            fpsLimiterMax = fpsLimiterMax,
                            onPerformanceHudConfigChanged = onPerformanceHudConfigChanged,
                            onFpsLimiterEnabledChanged = onFpsLimiterEnabledChanged,
                            onFpsLimiterChanged = onFpsLimiterChanged,
                            hasPhysicalController = hasPhysicalController,
                            isTouchscreenModeActive = isTouchscreenModeActive,
                            onTouchGestureSettingsClick = onTouchGestureSettingsClick,
                            isShooterModeActive = isShooterModeActive,
                            onShooterModeSettingsClick = onShooterModeSettingsClick,
                            activeToggleIds = activeToggleIds,
                            isLsfgAvailable = isLsfgAvailable,
                            lsfgMultiplier = lsfgMultiplier,
                            lsfgFlowScale = lsfgFlowScale,
                            lsfgPerformanceMode = lsfgPerformanceMode,
                            onLsfgMultiplierChanged = onLsfgMultiplierChanged,
                            onLsfgFlowScaleChanged = onLsfgFlowScaleChanged,
                            onLsfgPerformanceModeChanged = onLsfgPerformanceModeChanged,
                            // Visual visibility is permanent on the second
                            // screen; pause/resume follows input activation and
                            // is reported by the outer effect below.
                            onAnimationComplete = {},
                            onRequestOpen = onRequestOpen,
                            fullScreen = true,
                            modifier = Modifier.fillMaxSize(),
                    )
                },
            ))
        }

        // The panel no longer opens/closes visually, so report only activation
        // changes. This keeps the game running while the menu is merely visible.
        val reportedActive = remember { mutableStateOf(false) }
        val presentationActive = DsHomeSecondScreen.presentationActive
        LaunchedEffect(isVisible, presentationActive) {
            if (presentationActive && reportedActive.value != isVisible) {
                onAnimationComplete(isVisible)
                reportedActive.value = isVisible
            }
        }

        // Leaving the game while the menu is open would otherwise leave a stale
        // QUICK_MENU model (with callbacks into a disposed composition) on the
        // second display. Clear it.
        DisposableEffect(hasExternalDisplay) {
            onDispose {
                DsHomeSecondScreen.clear(DsHomeSecondScreen.Owner.GAME)
            }
        }
    }

    // Keep the invisible main-display composition alive so invite polling can
    // open the passive second-screen menu. If Presentation.show() fails, this
    // same instance remains the fully usable visible fallback.
    if (!hasExternalDisplay || !DsHomeSecondScreen.presentationActive || !isVisible) {
        QuickMenu(
            isVisible = isVisible,
            onDismiss = onDismiss,
            onItemSelected = onItemSelected,
            renderer = renderer,
            glRenderer = glRenderer,
            container = container,
            wineProcesses = wineProcesses,
            isWineProcessesLoading = isWineProcessesLoading,
            onToolsVisibilityChanged = onToolsVisibilityChanged,
            onEndWineProcess = onEndWineProcess,
            isPerformanceHudEnabled = isPerformanceHudEnabled,
            performanceHudConfig = performanceHudConfig,
            fpsLimiterEnabled = fpsLimiterEnabled,
            fpsLimiterTarget = fpsLimiterTarget,
            fpsLimiterMax = fpsLimiterMax,
            onPerformanceHudConfigChanged = onPerformanceHudConfigChanged,
            onFpsLimiterEnabledChanged = onFpsLimiterEnabledChanged,
            onFpsLimiterChanged = onFpsLimiterChanged,
            hasPhysicalController = hasPhysicalController,
            isTouchscreenModeActive = isTouchscreenModeActive,
            onTouchGestureSettingsClick = onTouchGestureSettingsClick,
            isShooterModeActive = isShooterModeActive,
            onShooterModeSettingsClick = onShooterModeSettingsClick,
            activeToggleIds = activeToggleIds,
            isLsfgAvailable = isLsfgAvailable,
            lsfgMultiplier = lsfgMultiplier,
            lsfgFlowScale = lsfgFlowScale,
            lsfgPerformanceMode = lsfgPerformanceMode,
            onLsfgMultiplierChanged = onLsfgMultiplierChanged,
            onLsfgFlowScaleChanged = onLsfgFlowScaleChanged,
            onLsfgPerformanceModeChanged = onLsfgPerformanceModeChanged,
            onAnimationComplete = { visible ->
                if (!hasExternalDisplay || !DsHomeSecondScreen.presentationActive) {
                    onAnimationComplete(visible)
                }
            },
            onRequestOpen = onRequestOpen,
            modifier = modifier,
        )
    }
}

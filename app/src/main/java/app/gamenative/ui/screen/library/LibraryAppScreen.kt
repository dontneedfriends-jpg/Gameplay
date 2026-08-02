@file:OptIn(ExperimentalFoundationApi::class)

package app.gamenative.ui.screen.library

import android.content.Intent
import android.content.res.Configuration
import app.gamenative.ui.screen.library.components.ambient.AmbientDownloadOverlay
import android.content.ActivityNotFoundException
import android.net.Uri
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import app.gamenative.ui.component.dialog.AlertDialog
import app.gamenative.ui.component.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.gamenative.NetworkMonitor
import app.gamenative.PrefManager
import app.gamenative.R
import app.gamenative.data.GameSource
import app.gamenative.data.LibraryItem
import app.gamenative.service.SteamService
import app.gamenative.ui.component.GamepadAction
import app.gamenative.ui.component.GamepadActionBar
import app.gamenative.ui.component.GamepadButton
import app.gamenative.ui.component.focusRing
import app.gamenative.ui.component.LoadingScreen
import app.gamenative.ui.data.AppMenuOption
import app.gamenative.ui.data.Achievement
import app.gamenative.ui.data.GameDisplayInfo
import app.gamenative.ui.enums.AppOptionMenuType
import app.gamenative.ui.internal.fakeAppInfo
import app.gamenative.ui.screen.library.appscreen.AmazonAppScreen
import app.gamenative.ui.screen.library.appscreen.CustomGameAppScreen
import app.gamenative.ui.screen.library.appscreen.EpicAppScreen
import app.gamenative.ui.screen.library.appscreen.GOGAppScreen
import app.gamenative.ui.screen.library.appscreen.SteamAppScreen
import app.gamenative.ui.screen.library.components.GameOptionsPanel
import app.gamenative.ui.screen.library.components.GameSourceIcon
import app.gamenative.utils.HltbService
import app.gamenative.ui.theme.PluviaTheme
import app.gamenative.ui.theme.isReduceMotionEnabled
import app.gamenative.ui.util.shouldShowGamepadUI
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import com.winlator.container.ContainerData
import com.winlator.core.KeyValueSet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import timber.log.Timber

// https://partner.steamgames.com/doc/store/assets/libraryassets#4

@Composable
private fun SkeletonText(
    modifier: Modifier = Modifier,
    lines: Int = 1,
    lineHeight: Int = 16,
) {
    val alpha = if (isReduceMotionEnabled()) {
        0.18f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
        val animatedAlpha by infiniteTransition.animateFloat(
            initialValue = 0.1f,
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "alpha",
        )
        animatedAlpha
    }

    val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)

    Column(modifier = modifier) {
        repeat(lines) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (index == lines - 1) 0.7f else 1f)
                    .height(lineHeight.dp)
                    .background(
                        color = color,
                        shape = RoundedCornerShape(4.dp),
                    ),
            )
            if (index < lines - 1) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isInstalled: Boolean = false,
    isDownloading: Boolean = false,
    downloadProgress: Float = 0f,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onProgressBarPositioned: ((Rect) -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "primaryActionScale",
    )

    val buttonColor = when {
        isDownloading -> PluviaTheme.colors.statusDownloading
        isInstalled -> PluviaTheme.colors.statusInstalled
        else -> PluviaTheme.colors.statusAvailable
    }

    Box(
        modifier = modifier
            .heightIn(min = 52.dp)
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (enabled) buttonColor else buttonColor.copy(alpha = 0.5f),
            )
            .focusRing(interactionSource, RoundedCornerShape(8.dp), width = 2.dp)
            .focusRequester(focusRequester)
            .selectable(
                selected = isFocused,
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isDownloading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
                LinearProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier
                        .width(80.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .then(
                            if (onProgressBarPositioned != null) {
                                Modifier.onGloballyPositioned { coordinates ->
                                    onProgressBarPositioned(coordinates.boundsInRoot())
                                }
                            } else {
                                Modifier
                            },
                        ),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f),
                )
                Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.CenterEnd) {
                    Text(
                        text = "${(downloadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                        ),
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = if (isInstalled) Icons.Default.PlayArrow else Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
            }
        }
    }
}


/**
 * Icon-only action button for the overlay action bar
 */
@Composable
private fun ActionIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "actionIconScale",
    )

    Box(
        modifier = modifier
            .scale(scale)
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isFocused) {
                    Color.White.copy(alpha = 0.2f)
                } else {
                    Color.White.copy(alpha = 0.1f)
                },
            )
            .focusRing(interactionSource, RoundedCornerShape(8.dp), width = 2.dp)
            .selectable(
                selected = isFocused,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun GameRuntimeSummary(
    config: ContainerData,
    modifier: Modifier = Modifier,
) {
    val dxConfig = remember(config.dxwrapperConfig) { KeyValueSet(config.dxwrapperConfig) }
    val directXName = remember(config.dxwrapper, config.dxwrapperConfig) {
        when (config.dxwrapper.lowercase(Locale.ROOT)) {
            "dxvk" -> listOf("DXVK", dxConfig.get("version")).filter { it.isNotBlank() }.joinToString(" ")
            "vkd3d" -> listOf("VKD3D", dxConfig.get("vkd3dVersion")).filter { it.isNotBlank() }.joinToString(" ")
            else -> config.dxwrapper.replaceFirstChar { it.titlecase(Locale.ROOT) }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        RuntimeSummaryValue(
            label = stringResource(R.string.game_runtime_driver),
            value = config.graphicsDriverVersion.ifBlank { config.graphicsDriver },
        )
        RuntimeSummaryValue(
            label = stringResource(R.string.game_runtime_wine_proton),
            value = config.wineVersion.ifBlank { "—" },
        )
        RuntimeSummaryValue(
            label = stringResource(R.string.game_runtime_directx),
            value = directXName.ifBlank { "—" },
        )
    }
}

@Composable
private fun RuntimeSummaryValue(
    label: String,
    value: String,
) {
    Column(modifier = Modifier.widthIn(min = 132.dp, max = 260.dp)) {
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.54f),
            maxLines = 1,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = Color.White.copy(alpha = 0.92f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Info card for game details with optional status indicator
 */
@Composable
private fun InfoCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    statusColor: Color? = null,
    isCompact: Boolean = false,
    focusableForNavigation: Boolean = false,
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val cardModifier = if (focusableForNavigation) {
        modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { state ->
                isFocused = state.isFocused
                if (state.isFocused) {
                    scope.launch { bringIntoViewRequester.bringIntoView() }
                }
            }
            .focusable(interactionSource = interactionSource)
            .focusRing(interactionSource, RoundedCornerShape(16.dp), width = 2.dp)
    } else {
        modifier
    }

    Surface(
        modifier = cardModifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(if (isCompact) 14.dp else 18.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (statusColor != null) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(statusColor, CircleShape),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = value,
                    style = if (isCompact) {
                        MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    } else {
                        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    },
                    color = if (statusColor != null) statusColor else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HltbInfoBar(
    stats: HltbService.Stats,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val items = listOf(
        stringResource(R.string.hltb_main_story) to stats.mainHours,
        stringResource(R.string.hltb_main_plus_extras) to stats.mainPlusHours,
        stringResource(R.string.hltb_completionist) to stats.completeHours,
        stringResource(R.string.hltb_all_styles) to stats.allStylesHours,
    )
    val canOpenHltb = stats.gameId > 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(enabled = canOpenHltb) {
                try {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("${HltbService.GAME_URL}${stats.gameId}")),
                    )
                } catch (e: ActivityNotFoundException) {
                    Timber.tag("HLTB").w(e, "No handler for HLTB game URL")
                }
            }
            .padding(14.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.hltb_section_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
            if (canOpenHltb) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .widthIn(min = maxWidth),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { (label, hours) ->
                    Column(
                        modifier = Modifier
                            .widthIn(min = 48.dp)
                            .padding(horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = if (hours == HltbService.UNKNOWN_HOURS) {
                                HltbService.UNKNOWN_HOURS
                            } else {
                                stringResource(R.string.hltb_hours_value, hours)
                            },
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(
    libraryItem: LibraryItem,
    onClickPlay: (Boolean) -> Unit,
    onTestGraphics: () -> Unit,
    onPlayWithDiagnostics: () -> Unit,
    onBack: () -> Unit,
    onSourceClick: (GameSource) -> Unit = {},
    runPrimaryActionOnOpen: Boolean = false,
    onPrimaryActionConsumed: () -> Unit = {},
) {
    // Get the appropriate screen model based on game source
    val screenModel = remember(libraryItem.gameSource) {
        when (libraryItem.gameSource) {
            app.gamenative.data.GameSource.STEAM -> SteamAppScreen()
            app.gamenative.data.GameSource.CUSTOM_GAME -> CustomGameAppScreen()
            app.gamenative.data.GameSource.GOG -> GOGAppScreen()
            app.gamenative.data.GameSource.EPIC -> EpicAppScreen()
            app.gamenative.data.GameSource.AMAZON -> AmazonAppScreen()
        }
    }

    // Render the content using the model
    screenModel.Content(
        libraryItem = libraryItem,
        onClickPlay = onClickPlay,
        onTestGraphics = onTestGraphics,
        onPlayWithDiagnostics = onPlayWithDiagnostics,
        onBack = onBack,
        onSourceClick = onSourceClick,
        runPrimaryActionOnOpen = runPrimaryActionOnOpen,
        onPrimaryActionConsumed = onPrimaryActionConsumed,
    )
}

/**
 * Formats bytes into a human-readable string (KB, MB, GB).
 * Uses binary units (1024 base).
 */
private fun formatBytes(bytes: Long): String {
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        bytes >= gb -> String.format("%.1f GB", bytes / gb)
        bytes >= mb -> String.format("%.1f MB", bytes / mb)
        bytes >= kb -> String.format("%.1f KB", bytes / kb)
        else -> "$bytes B"
    }
}

private data class AppScreenNetworkState(
    val hasInternet: Boolean,
    val hasWifiOrEthernet: Boolean,
    val downloadStatusMessage: String?,
)

private class AppScreenRuntimeState {
    val scrollState = androidx.compose.foundation.ScrollState(0)
    val playButtonFocusRequester = FocusRequester()
    var optionsMenuVisible by mutableStateOf(false)
    var progressBarBounds by mutableStateOf<Rect?>(null)
    var ambientInteractionCounter by mutableStateOf(0)

    fun downloadAllowed(network: AppScreenNetworkState): Boolean =
        !PrefManager.downloadOnWifiOnly || network.hasWifiOrEthernet

    fun pauseResumeEnabled(
        isDownloading: Boolean,
        hasPartialDownload: Boolean,
        network: AppScreenNetworkState,
    ): Boolean = if (!isDownloading && hasPartialDownload) downloadAllowed(network) else true

    fun buttonEnabled(
        isInstalled: Boolean,
        isValidToDownload: Boolean,
        network: AppScreenNetworkState,
    ): Boolean {
        val installEnabled = if (!isInstalled) downloadAllowed(network) && network.hasInternet else true
        return if (isInstalled) installEnabled else installEnabled && isValidToDownload
    }

    fun startActionEnabled(
        isInstalled: Boolean,
        isValidToDownload: Boolean,
        isDownloading: Boolean,
        hasPartialDownload: Boolean,
        network: AppScreenNetworkState,
    ): Boolean = if (isDownloading || hasPartialDownload) {
        pauseResumeEnabled(isDownloading, hasPartialDownload, network)
    } else {
        buttonEnabled(isInstalled, isValidToDownload, network)
    }

    fun performStartAction(
        isDownloading: Boolean,
        hasPartialDownload: Boolean,
        onPauseResumeClick: () -> Unit,
        onDownloadInstallClick: () -> Unit,
    ) {
        if (isDownloading || hasPartialDownload) onPauseResumeClick() else onDownloadInstallClick()
    }

    fun handleKeyEvent(
        event: KeyEvent,
        startActionEnabled: Boolean,
        onStartAction: () -> Unit,
        onBack: () -> Unit,
    ): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        return when (event.keyCode) {
            KeyEvent.KEYCODE_BUTTON_SELECT -> {
                optionsMenuVisible = true
                true
            }
            KeyEvent.KEYCODE_BUTTON_START -> {
                if (!optionsMenuVisible && startActionEnabled) onStartAction()
                true
            }
            KeyEvent.KEYCODE_BUTTON_B -> {
                if (optionsMenuVisible) optionsMenuVisible = false else onBack()
                true
            }
            else -> false
        }
    }

    fun downloadTimeLeftText(
        downloadProgress: Float,
        downloadInfo: app.gamenative.data.DownloadInfo?,
        isDownloading: Boolean,
        statusMessage: String?,
    ): String {
        val etaMs = downloadInfo?.getEstimatedTimeRemaining()
        return if (etaMs != null && etaMs > 0L) {
            val totalSeconds = etaMs / 1000
            "${totalSeconds / 60}m ${totalSeconds % 60}s left"
        } else if (isDownloading && downloadProgress >= 1f) {
            statusMessage?.takeUnless { it.isBlank() } ?: "Unpacking..."
        } else if (downloadProgress in 0f..1f && downloadProgress < 1f) {
            statusMessage?.takeUnless { it.isBlank() } ?: ""
        } else {
            ""
        }
    }

    fun downloadSizeText(downloadInfo: app.gamenative.data.DownloadInfo?, downloadingLabel: String): String {
        val (bytesDone, bytesTotal) = downloadInfo?.getBytesProgress() ?: (0L to 0L)
        return when {
            bytesTotal > 0L -> "${formatBytes(bytesDone)} / ${formatBytes(bytesTotal)}"
            bytesDone > 0L -> formatBytes(bytesDone)
            else -> downloadingLabel
        }
    }
}

@Composable
private fun rememberAppScreenNetworkState(
    downloadInfo: app.gamenative.data.DownloadInfo?,
): AppScreenNetworkState {
    val hasInternet by NetworkMonitor.hasInternet.collectAsState()
    val hasWifiOrEthernet by NetworkMonitor.hasWifiOrEthernet.collectAsState()
    val statusFlow = remember(downloadInfo) { downloadInfo?.getStatusMessageFlow() }
    val statusMessage by (
        statusFlow?.collectAsState(initial = statusFlow.value)
            ?: remember { mutableStateOf<String?>(null) }
        )
    return AppScreenNetworkState(hasInternet, hasWifiOrEthernet, statusMessage)
}

@Composable
private fun AppScreenGamepadActions(
    runtime: AppScreenRuntimeState,
    network: AppScreenNetworkState,
    isInstalled: Boolean,
    isValidToDownload: Boolean,
    isDownloading: Boolean,
    hasPartialDownload: Boolean,
    onPauseResumeClick: () -> Unit,
    onDownloadInstallClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val runPrimaryAction = {
        if (runtime.startActionEnabled(isInstalled, isValidToDownload, isDownloading, hasPartialDownload, network)) {
            runtime.performStartAction(isDownloading, hasPartialDownload, onPauseResumeClick, onDownloadInstallClick)
        }
    }
    val primaryLabel = when {
        isInstalled -> R.string.run_app
        isDownloading -> R.string.pause_download
        hasPartialDownload -> R.string.resume_download
        else -> R.string.install_app
    }
    GamepadActionBar(
        actions = listOf(
            GamepadAction(GamepadButton.START, primaryLabel, runPrimaryAction),
            GamepadAction(GamepadButton.SELECT, R.string.options) {
                runtime.optionsMenuVisible = true
            },
            GamepadAction(GamepadButton.B, R.string.back, onBack),
        ),
        modifier = modifier,
        visible = !runtime.optionsMenuVisible,
    )
}

@Composable
private fun BoxScope.GameHeroBackdrop(
    displayInfo: GameDisplayInfo,
    parallaxOffset: Float,
) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .graphicsLayer { translationY = parallaxOffset },
    ) {
        if (displayInfo.heroImageUrl != null) {
            CoilImage(
                modifier = Modifier.fillMaxSize(),
                imageModel = { displayInfo.heroImageUrl },
                imageOptions = ImageOptions(contentScale = ContentScale.Crop),
                loading = { LoadingScreen() },
                failure = { GameHeroFallback() },
                previewPlaceholder = painterResource(R.drawable.testhero),
            )
        } else {
            GameHeroFallback()
        }
        // Gradients live inside the parallax layer so the shading stays glued
        // to the image while it drifts (previously they were pinned and the
        // shadow appeared to slide off the picture on scroll).
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.85f),
                        ),
                        endY = Float.POSITIVE_INFINITY,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.15f),
                            Color.Transparent,
                        ),
                        endY = Float.POSITIVE_INFINITY,
                    ),
                ),
        )
    }
}

@Composable
private fun GameHeroFallback() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    )
}

@Composable
private fun AppScreenBody(content: @Composable () -> Unit) {
    content()
}

@Composable
internal fun AppScreenContent(
    modifier: Modifier = Modifier,
    displayInfo: GameDisplayInfo,
    isInstalled: Boolean,
    isValidToDownload: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    hasPartialDownload: Boolean,
    hasLeftoverInstall: Boolean = false,
    isUpdatePending: Boolean,
    downloadInfo: app.gamenative.data.DownloadInfo? = null,
    onDownloadInstallClick: () -> Unit,
    onPauseResumeClick: () -> Unit,
    onDeleteDownloadClick: () -> Unit,
    onUpdateClick: () -> Unit,
    onBack: () -> Unit = {},
    optionsMenu: List<AppMenuOption>,
    dialogOpen: Boolean = false,
    otherSources: List<GameSource> = emptyList(),
    isInstalledOnOtherSource: Boolean = false,
    onSourceClick: (GameSource) -> Unit = {},
    runtimeConfig: ContainerData? = null,
    achievements: List<Achievement>? = null,
) {
    val context = LocalContext.current
    // reactive — recomposes when network state changes
    val runtime = remember { AppScreenRuntimeState() }
    var achievementsVisible by rememberSaveable(displayInfo.appId) { mutableStateOf(false) }
    val network = rememberAppScreenNetworkState(downloadInfo)
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

    LaunchedEffect(displayInfo.appId) {
        runtime.scrollState.animateScrollTo(0)
    }

    LaunchedEffect(Unit) {
        runtime.playButtonFocusRequester.requestFocus()
    }

    // Restore focus when options menu, dialogs
    LaunchedEffect(runtime.optionsMenuVisible, dialogOpen) {
        if (!runtime.optionsMenuVisible && !dialogOpen) {
            kotlinx.coroutines.delay(100) // Brief delay for menu/dialog animation
            try {
                runtime.playButtonFocusRequester.requestFocus()
            } catch (_: IllegalStateException) {
                // FocusRequester not attached
            }
        }
    }

    // Handle back press when options panel is open
    BackHandler(enabled = runtime.optionsMenuVisible && !achievementsVisible) {
        runtime.optionsMenuVisible = false
    }

    AppScreenBody {
        Box(
            modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val pointerEvent = awaitPointerEvent(PointerEventPass.Initial)
                        if (pointerEvent.changes.any { it.changedToDownIgnoreConsumed() }) {
                            runtime.ambientInteractionCounter++
                        }
                    }
                }
            }
            .onKeyEvent {
                if (achievementsVisible) {
                    false
                } else {
                    if (it.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                        runtime.ambientInteractionCounter++
                    }
                    runtime.handleKeyEvent(
                        event = it.nativeKeyEvent,
                        startActionEnabled = runtime.startActionEnabled(
                            isInstalled,
                            isValidToDownload,
                            isDownloading,
                            hasPartialDownload,
                            network,
                        ),
                        onStartAction = {
                            runtime.performStartAction(
                                isDownloading,
                                hasPartialDownload,
                                onPauseResumeClick,
                                onDownloadInstallClick,
                            )
                        },
                        onBack = onBack,
                    )
                }
            },
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Room for the gamepad action bar overlaying the bottom edge,
                // so it never covers the last content row (achievements etc.).
                .padding(bottom = if (shouldShowGamepadUI()) 56.dp else 0.dp)
                .verticalScroll(runtime.scrollState),
        ) {
            // Hero Section (Parallax)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (isPortrait) 390.dp else 440.dp)
                    .clipToBounds(),
            ) {
                GameHeroBackdrop(
                    displayInfo = displayInfo,
                    parallaxOffset = runtime.scrollState.value * 0.5f,
                )

                // Back button (top left).
                // The hero image is intentionally drawn full-bleed through the status bar
                // and any display cutout (notch / hole-punch / side cutout). The button
                // itself, however, has to stay tappable, so it's pushed inwards by whichever
                // is larger of the status bar inset or the cutout inset on each affected
                // edge before the visual 16dp padding is applied.
                ActionIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    onClick = onBack,
                    modifier = Modifier
                        .windowInsetsPadding(
                            WindowInsets.statusBars
                                .union(WindowInsets.displayCutout)
                                .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                        )
                        .padding(16.dp),
                )

                // Bottom overlay with title and action bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = if (isPortrait) 152.dp else 184.dp,
                            start = if (isPortrait) 20.dp else 48.dp,
                            end = if (isPortrait) 20.dp else 48.dp,
                            bottom = 24.dp,
                        ),
                ) {
                    // Game title
                    Text(
                        text = displayInfo.name,
                        style = (if (isPortrait) {
                            MaterialTheme.typography.headlineLarge
                        } else {
                            MaterialTheme.typography.displaySmall
                        }).copy(
                            fontWeight = FontWeight.Bold,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.6f),
                                offset = Offset(0f, 2f),
                                blurRadius = 8f,
                            ),
                        ),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 840.dp),
                    )

                    // Developer and year
                    val releaseYear = remember(displayInfo.releaseDate) {
                        if (displayInfo.releaseDate > 0) {
                            SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(displayInfo.releaseDate * 1000))
                        } else {
                            ""
                        }
                    }
                    Text(
                        text = listOf(displayInfo.developer, releaseYear)
                            .filter { it.isNotBlank() }
                            .joinToString(" • "),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.78f),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Integrated action bar - overlaid on hero
                    Column(
                        modifier = Modifier
                            .widthIn(max = 820.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.42f))
                            .padding(12.dp),
                    ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusGroup(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Primary action button (left-aligned)
                        if (isDownloading || hasPartialDownload) {
                            PrimaryActionButton(
                                text = if (isDownloading) {
                                    stringResource(R.string.pause_download)
                                } else {
                                    stringResource(R.string.resume_download)
                                },
                                onClick = onPauseResumeClick,
                                enabled = runtime.pauseResumeEnabled(isDownloading, hasPartialDownload, network),
                                isInstalled = false,
                                isDownloading = isDownloading,
                                downloadProgress = downloadProgress,
                                focusRequester = runtime.playButtonFocusRequester,
                                onProgressBarPositioned = { runtime.progressBarBounds = it },
                            )
                        } else {
                            val text = when {
                                isInstalled -> stringResource(R.string.run_app)
                                !network.hasInternet -> stringResource(R.string.library_need_internet)
                                !network.hasWifiOrEthernet && PrefManager.downloadOnWifiOnly -> stringResource(R.string.library_wifi_only_enabled)
                                else -> stringResource(R.string.install_app)
                            }
                            PrimaryActionButton(
                                text = text,
                                onClick = onDownloadInstallClick,
                                enabled = runtime.buttonEnabled(isInstalled, isValidToDownload, network),
                                isInstalled = isInstalled,
                                focusRequester = runtime.playButtonFocusRequester,
                            )
                        }

                        // Download size / ETA text — inline only in landscape
                        if (isDownloading && !isPortrait) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                verticalArrangement = Arrangement.Center,
                            ) {
                                if (runtime.downloadSizeText(downloadInfo, stringResource(R.string.downloading)).isNotEmpty()) {
                                    Text(
                                        text = runtime.downloadSizeText(downloadInfo, stringResource(R.string.downloading)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.9f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (runtime.downloadTimeLeftText(downloadProgress, downloadInfo, isDownloading, network.downloadStatusMessage).isNotEmpty()) {
                                    Text(
                                        text = runtime.downloadTimeLeftText(downloadProgress, downloadInfo, isDownloading, network.downloadStatusMessage),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.65f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        // Secondary action icons (right-aligned)
                        ActionIconButton(
                            icon = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.options),
                            onClick = { runtime.optionsMenuVisible = true },
                        )

                        if (isInstalled || hasPartialDownload || hasLeftoverInstall) {
                            ActionIconButton(
                                icon = Icons.Default.Delete,
                                contentDescription = if (isInstalled || hasLeftoverInstall) stringResource(R.string.uninstall) else stringResource(R.string.delete_app),
                                onClick = onDeleteDownloadClick,
                            )
                        }
                    }

                    if (runtimeConfig != null) {
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 10.dp),
                            color = Color.White.copy(alpha = 0.14f),
                        )
                        GameRuntimeSummary(
                            config = runtimeConfig,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }

                    if (isDownloading && isPortrait) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (runtime.downloadSizeText(downloadInfo, stringResource(R.string.downloading)).isNotEmpty()) {
                                Text(
                                    text = runtime.downloadSizeText(downloadInfo, stringResource(R.string.downloading)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f),
                                    maxLines = 1,
                                )
                            }
                            if (runtime.downloadTimeLeftText(downloadProgress, downloadInfo, isDownloading, network.downloadStatusMessage).isNotEmpty()) {
                                Text(
                                    text = runtime.downloadTimeLeftText(downloadProgress, downloadInfo, isDownloading, network.downloadStatusMessage),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.65f),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                    }

                    // Compatibility status (if applicable)
                    if (displayInfo.compatibilityMessage != null && displayInfo.compatibilityColor != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = displayInfo.compatibilityMessage,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(displayInfo.compatibilityColor),
                        )
                    }
                }
            }

            // Content section below hero with solid background
            Column(
                modifier = Modifier
                    .widthIn(max = 1180.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .padding(
                        start = if (isPortrait) 20.dp else 48.dp,
                        end = if (isPortrait) 20.dp else 48.dp,
                        top = 26.dp,
                        bottom = 96.dp,
                    ),
            ) {
                // Update available banner
                if (isUpdatePending) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.update_available),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            Button(
                                onClick = onUpdateClick,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                ),
                            ) {
                                Text(stringResource(R.string.update_now))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (!isInstalled && isInstalledOnOtherSource) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            text = stringResource(R.string.library_already_installed_on_other_store),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Game information section
                Text(
                    text = stringResource(R.string.game_information),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                // Info cards in 2-column grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val statusText = when {
                        isInstalled -> stringResource(R.string.installed)
                        isDownloading -> stringResource(R.string.installing)
                        else -> stringResource(R.string.not_installed)
                    }
                    val statusColor = when {
                        isInstalled -> PluviaTheme.colors.statusInstalled
                        isDownloading -> MaterialTheme.colorScheme.tertiary
                        else -> null
                    }
                    InfoCard(
                        label = stringResource(R.string.status),
                        value = statusText,
                        statusColor = statusColor,
                        isCompact = true,
                        modifier = Modifier.weight(1f),
                        focusableForNavigation = true,
                    )
                    InfoCard(
                        label = stringResource(R.string.size),
                        value = when {
                            isInstalled && displayInfo.sizeOnDisk != null -> displayInfo.sizeOnDisk
                            !isInstalled && displayInfo.sizeFromStore != null -> displayInfo.sizeFromStore
                            else -> stringResource(R.string.library_compatibility_unknown)
                        },
                        isCompact = true,
                        modifier = Modifier.weight(1f),
                        focusableForNavigation = true,
                    )
                }

                displayInfo.hltbStats?.let { hltb ->
                    Spacer(modifier = Modifier.height(10.dp))
                    HltbInfoBar(hltb)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    InfoCard(
                        label = stringResource(R.string.developer),
                        value = displayInfo.developer,
                        isCompact = true,
                        modifier = Modifier.weight(1f),
                        focusableForNavigation = true,
                    )
                    InfoCard(
                        label = stringResource(R.string.release_date),
                        value = remember(displayInfo.releaseDate) {
                            if (displayInfo.releaseDate > 0) {
                                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                    .format(Date(displayInfo.releaseDate * 1000))
                            } else {
                                context.getString(R.string.library_compatibility_unknown)
                            }
                        },
                        isCompact = true,
                        modifier = Modifier.weight(1f),
                        focusableForNavigation = true,
                    )
                }

                // Install location (when installed)
                if (isInstalled && displayInfo.installLocation != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    InfoCard(
                        label = stringResource(R.string.location),
                        value = displayInfo.installLocation,
                        isCompact = true,
                        modifier = Modifier.fillMaxWidth(),
                        focusableForNavigation = true,
                    )
                }

                // Play time and last played
                if (displayInfo.playtimeText != null || displayInfo.lastPlayedText != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (displayInfo.playtimeText != null) {
                            InfoCard(
                                label = stringResource(R.string.play_time),
                                value = displayInfo.playtimeText,
                                isCompact = true,
                                modifier = Modifier.weight(1f),
                                focusableForNavigation = true,
                            )
                        }
                        if (displayInfo.lastPlayedText != null) {
                            InfoCard(
                                label = stringResource(R.string.last_played),
                                value = displayInfo.lastPlayedText,
                                isCompact = true,
                                modifier = Modifier.weight(1f),
                                focusableForNavigation = true,
                            )
                        }
                    }
                }

                if (!achievements.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    AchievementSummaryButton(
                        achievements = achievements,
                        onClick = { achievementsVisible = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (otherSources.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.library_available_on),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        otherSources.forEach { source ->
                            val chipInteractionSource = remember { MutableInteractionSource() }
                            val chipFocused by chipInteractionSource.collectIsFocusedAsState()
                            val chipShape = RoundedCornerShape(8.dp)
                            Surface(
                                onClick = { onSourceClick(source) },
                                shape = chipShape,
                                color = if (chipFocused) {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                modifier = Modifier.focusRing(chipInteractionSource, chipShape, width = 2.dp),
                                interactionSource = chipInteractionSource,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    GameSourceIcon(gameSource = source, iconSize = 16)
                                    Text(
                                        text = when (source) {
                                            GameSource.STEAM -> stringResource(R.string.tab_steam)
                                            GameSource.GOG -> stringResource(R.string.tab_gog)
                                            GameSource.EPIC -> stringResource(R.string.tab_epic)
                                            GameSource.AMAZON -> stringResource(R.string.tab_amazon)
                                            GameSource.CUSTOM_GAME -> stringResource(R.string.tab_local)
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }
                }

            }
        }

        if (!achievementsVisible) AppScreenGamepadActions(
            runtime = runtime,
            network = network,
            isInstalled = isInstalled,
            isValidToDownload = isValidToDownload,
            isDownloading = isDownloading,
            hasPartialDownload = hasPartialDownload,
            onPauseResumeClick = onPauseResumeClick,
            onDownloadInstallClick = onDownloadInstallClick,
            onBack = onBack,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        // Options panel - slides in from right
        if (!achievementsVisible) GameOptionsPanel(
            isOpen = runtime.optionsMenuVisible,
            onDismiss = { runtime.optionsMenuVisible = false },
            options = optionsMenu.toList(),
            modifier = Modifier.align(Alignment.CenterEnd),
        )

        // Ambient mode during downloads
        if (isDownloading && !achievementsVisible) {
            AmbientDownloadOverlay(
                gameName = displayInfo.name,
                downloadProgress = downloadProgress,
                iconUrl = displayInfo.iconUrl,
                originBounds = runtime.progressBarBounds,
                userInteractionCounter = runtime.ambientInteractionCounter,
            )
        }

        if (achievementsVisible && !achievements.isNullOrEmpty()) {
            SteamAchievementsPage(
                gameName = displayInfo.name,
                achievements = achievements,
                onBack = { achievementsVisible = false },
            )
        }
        }
    }
}

@Composable
fun GameMigrationDialog(
    progress: Float,
    currentFile: String,
    movedFiles: Int,
    totalFiles: Int,
) {
    AlertDialog(
        onDismissRequest = {
            // We don't allow dismissal during move.
        },
        icon = { Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null) },
        title = { Text(text = stringResource(R.string.moving_files)) },
        text = {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.library_file_count, movedFiles + 1, totalFiles),
                    style = MaterialTheme.typography.bodyLarge,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = currentFile,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                )

                Spacer(modifier = Modifier.height(16.dp))

                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = { progress },
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "${(progress * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {},
    )
}

/***********
 * PREVIEW *
 ***********/

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(
    device = "spec:width=1920px,height=1080px,dpi=440",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
) // Odin2 Mini
@Composable
private fun Preview_AppScreen() {
    val context = LocalContext.current
    PrefManager.init(context)
    val intent = Intent(context, SteamService::class.java)
    context.startForegroundService(intent)
    var isDownloading by remember { mutableStateOf(false) }
    val fakeApp = fakeAppInfo(1)
    val displayInfo = GameDisplayInfo(
        name = fakeApp.name,
        developer = fakeApp.developer,
        releaseDate = fakeApp.releaseDate,
        heroImageUrl = fakeApp.getHeroUrl(),
        iconUrl = fakeApp.iconUrl,
        gameId = fakeApp.id,
        appId = "STEAM_${fakeApp.id}",
        installLocation = null,
        sizeOnDisk = null,
        sizeFromStore = null,
        lastPlayedText = null,
        playtimeText = null,
    )
    PluviaTheme {
        Surface {
            AppScreenContent(
                displayInfo = displayInfo,
                isInstalled = false,
                isValidToDownload = true,
                isDownloading = isDownloading,
                downloadProgress = .50f,
                hasPartialDownload = false,
                isUpdatePending = false,
                downloadInfo = null,
                onDownloadInstallClick = { isDownloading = !isDownloading },
                onPauseResumeClick = { },
                onDeleteDownloadClick = { },
                onUpdateClick = { },
                optionsMenu = AppOptionMenuType.entries.map {
                    AppMenuOption(
                        optionType = it,
                        onClick = { },
                    )
                },
            )
        }
    }
}

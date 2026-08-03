package app.gamenative.externaldisplay

import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import app.gamenative.PrefManager
import app.gamenative.data.GameCompatibilityStatus
import app.gamenative.data.LibraryItem
import app.gamenative.ui.component.CompatibilityBadge
import app.gamenative.ui.component.GameStatsRow
import app.gamenative.ui.data.GameCardStats
import app.gamenative.ui.enums.PaneType
import app.gamenative.ui.screen.library.components.DsGameGrid
import app.gamenative.ui.screen.library.components.GameSourceIcon
import app.gamenative.ui.screen.library.components.GridImageUrls
import app.gamenative.ui.screen.library.components.getGridImageUrl
import app.gamenative.ui.theme.PluviaTheme
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * Bridge between the main-screen library state and the second-display
 * presentation. The main screen publishes the model; the presentation's own
 * composition subscribes to it.
 */
object DsHomeSecondScreen {

    enum class Mode {
        // Icon grid, navigable with the gamepad (DS_HOME and grid panes).
        GRID,

        // Passive details panel for the focused game (carousel/hero panes).
        DETAILS,
    }

    class Model(
        val mode: Mode = Mode.GRID,
        val items: List<LibraryItem>,
        val focusedIndex: Int = 0,
        val focusedItem: LibraryItem? = null,
        val focusedStats: GameCardStats? = null,
        val focusedCompat: GameCompatibilityStatus? = null,
        val onNavigate: (String) -> Unit,
        val onFocused: (Int) -> Unit,
    )

    var model by mutableStateOf<Model?>(null)
}

private class PresentationLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    fun handlePresentationStart() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun handlePresentationStop() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}

class DsHomePresentation(
    context: Context,
    display: Display,
) : Presentation(context, display) {

    private val presentationLifecycleOwner = PresentationLifecycleOwner()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(presentationLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(presentationLifecycleOwner)
            setContent {
                PluviaTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        val model = DsHomeSecondScreen.model
                        if (model != null) {
                            when (model.mode) {
                                DsHomeSecondScreen.Mode.GRID -> DsHomeSecondScreenGrid(model)
                                DsHomeSecondScreen.Mode.DETAILS -> DsHomeSecondScreenDetails(model)
                            }
                        }
                    }
                }
            }
        }
        setContentView(composeView)
        presentationLifecycleOwner.handlePresentationStart()
    }

    override fun onDetachedFromWindow() {
        presentationLifecycleOwner.handlePresentationStop()
        super.onDetachedFromWindow()
    }
}

@Composable
private fun DsHomeSecondScreenGrid(model: DsHomeSecondScreen.Model) {
    val gridState = rememberLazyGridState()
    val firstItemFocusRequester = remember { FocusRequester() }
    val cellMinSize = when (PrefManager.dsHomeIconScale) {
        0 -> 72.dp
        2 -> 128.dp
        else -> 96.dp
    }
    // The presentation window gains real focus a moment after show(); wait for
    // window focus before bootstrapping compose focus so the initial selection
    // sticks instead of being stolen back by the main display.
    val windowInfo = LocalWindowInfo.current
    LaunchedEffect(model.items.size) {
        if (model.items.isEmpty()) return@LaunchedEffect
        withTimeoutOrNull(2_000) {
            snapshotFlow { windowInfo.isWindowFocused }
                .filter { it }
                .first()
        }
        var retries = 0
        while (retries < 8) {
            try {
                firstItemFocusRequester.requestFocus()
                break
            } catch (_: IllegalStateException) {
                retries++
                delay(32)
            }
        }
    }
    DsGameGrid(
        items = model.items,
        listState = gridState,
        cellMinSize = cellMinSize,
        focusTargetIndex = model.focusedIndex,
        firstItemFocusRequester = firstItemFocusRequester,
        onFocusedIndexChanged = model.onFocused,
        onNavigate = model.onNavigate,
        onScaleCycle = {
            PrefManager.dsHomeIconScale = (PrefManager.dsHomeIconScale + 1) % 3
        },
        modifier = Modifier.fillMaxSize(),
    )
}

/**
 * Passive details panel for the focused game, shown on the second display when
 * the main display hosts a carousel/hero pane.
 */
@Composable
private fun DsHomeSecondScreenDetails(model: DsHomeSecondScreen.Model) {
    val item = model.focusedItem
    if (item == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No game selected",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    val context = LocalContext.current
    val imageUrls by produceState(
        initialValue = GridImageUrls("", ""),
        key1 = item.appId,
    ) {
        value = withContext(Dispatchers.IO) {
            getGridImageUrl(context, item, PaneType.GRID_HERO)
        }
    }
    val imageUrl = imageUrls.primary.ifEmpty { imageUrls.fallback }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
        ) {
            if (imageUrl.isNotEmpty()) {
                CoilImage(
                    modifier = Modifier.fillMaxSize(),
                    imageModel = { imageUrl },
                    imageOptions = ImageOptions(
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                    ),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f),
                            ),
                        ),
                    ),
            )
            Text(
                text = item.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GameSourceIcon(
                    gameSource = item.gameSource,
                    iconSize = 18,
                    alignmentBoxSize = 26,
                )
                val compat = model.focusedCompat
                if (compat != null) {
                    CompatibilityBadge(
                        status = compat,
                        showLabel = true,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }

            GameStatsRow(
                stats = model.focusedStats,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                animate = false,
            )

            if (item.sizeBytes > 0) {
                Text(
                    text = formatBytes(item.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return ""
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format("%.1f MB", kb / 1024.0)
    return String.format("%.1f GB", kb / 1024.0 / 1024.0)
}

/** Shows/dismisses the second-display grid while a DS_HOME model is published. */
@Composable
fun DsHomePresentationHost() {
    val context = LocalContext.current
    val hasModel = DsHomeSecondScreen.model != null

    DisposableEffect(hasModel) {
        if (!hasModel) {
            return@DisposableEffect onDispose { }
        }
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        val display = displayManager?.displays?.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
        if (display == null) {
            return@DisposableEffect onDispose { }
        }
        val presentation = DsHomePresentation(context.applicationContext, display)
        runCatching { presentation.show() }
            .onFailure { Timber.e(it, "Failed to show DS_HOME presentation") }
        onDispose {
            runCatching { presentation.dismiss() }
        }
    }
}

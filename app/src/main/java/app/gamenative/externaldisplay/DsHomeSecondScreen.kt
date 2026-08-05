package app.gamenative.externaldisplay

import android.app.Presentation
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.setViewTreeOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
import app.gamenative.enums.AppTheme
import app.gamenative.ui.component.CompatibilityBadge
import app.gamenative.ui.component.GamepadAction
import app.gamenative.ui.component.GamepadActionBar
import app.gamenative.ui.component.GamepadButton
import app.gamenative.ui.component.focusRing
import app.gamenative.ui.component.GameStatsRow
import app.gamenative.ui.data.GameCardStats
import app.gamenative.ui.data.PerformanceHudConfig
import app.gamenative.ui.data.shouldLoadNextLibraryPage
import app.gamenative.ui.enums.LibraryTab
import app.gamenative.ui.enums.PaneType
import app.gamenative.ui.screen.library.components.DsGameGrid
import app.gamenative.ui.screen.library.components.GameSourceIcon
import app.gamenative.ui.screen.library.components.GridImageUrls
import app.gamenative.ui.screen.library.components.getGridImageUrl
import app.gamenative.ui.theme.PluviaTheme
import app.gamenative.ui.widget.PerformanceHudView
import app.gamenative.utils.rememberExternalDisplay
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * Bridge between the main-screen library state and the second-display
 * presentation. The main screen publishes the model; the presentation's own
 * composition subscribes to it.
 */
object DsHomeSecondScreen {

    enum class Owner {
        LIBRARY,
        SYSTEM,
        GAME_CARD,
        SETTINGS,
        DIALOG,
        GAME,
    }

    enum class Mode {
        // Icon grid, navigable with the gamepad (DS_HOME and grid panes).
        GRID,

        // Passive details panel for the focused game (carousel/hero panes).
        DETAILS,

        // Full game-card content (the part below the hero) rendered by the
        // main display, shown on the second display while a card is open.
        CARD,

        // The in-game QuickMenu rendered on the second display while a game is
        // running and actively controlled by the gamepad.
        QUICK_MENU,

        // The same QuickMenu remains visible while input stays with the game.
        // Back promotes it to QUICK_MENU; Back again returns to this mode.
        QUICK_MENU_PASSIVE,

        // Passive, glanceable runtime status while the game owns controller focus.
        GAME_DASHBOARD,

        // Full settings workspace rendered on the lower display.
        SETTINGS,
    }

    class Model(
        val owner: Owner,
        val mode: Mode = Mode.GRID,
        val items: List<LibraryItem> = emptyList(),
        val focusedIndex: Int = 0,
        val focusedItem: LibraryItem? = null,
        val focusedStats: GameCardStats? = null,
        val focusedCompat: GameCompatibilityStatus? = null,
        val libraryLayout: PaneType = PaneType.GRID_CAPSULE,
        val currentTab: LibraryTab = LibraryTab.ALL,
        val isLoading: Boolean = false,
        val isSearching: Boolean = false,
        val searchQuery: String = "",
        val totalItemCount: Int = items.size,
        val currentPage: Int = 1,
        val lastPage: Int = 1,
        val tabCounts: Map<LibraryTab, Int> = emptyMap(),
        val onSearchQuery: (String) -> Unit = {},
        val onSearchToggle: () -> Unit = {},
        val onPreviousTab: () -> Unit = {},
        val onNextTab: () -> Unit = {},
        val onOptions: () -> Unit = {},
        val onSystemMenu: () -> Unit = {},
        val onOpenSettings: () -> Unit = {},
        val onAddGame: () -> Unit = {},
        val onQuickActions: () -> Unit = {},
        val onLayoutCycle: () -> Unit = {},
        val onRefresh: () -> Unit = {},
        val onPageChange: (Int) -> Unit = {},
        val cardContent: (@Composable () -> Unit)? = null,
        val menuContent: (@Composable () -> Unit)? = null,
        val settingsContent: (@Composable () -> Unit)? = null,
        val dashboardTitle: String = "",
        val dashboardSubtitle: String = "",
        val dashboardImageUrl: String = "",
        val dashboardLogoUrl: String = "",
        val performanceHudEnabled: Boolean = false,
        val performanceHudConfig: PerformanceHudConfig = PerformanceHudConfig(),
        val performanceHudFpsProvider: () -> Float = { 0f },
        val performanceHudKey: Int = 0,
        val onShowDashboard: () -> Unit = {},
        val onShowMenu: () -> Unit = {},
        val onBack: () -> Unit = {},
        val onNavigate: (String) -> Unit = {},
        val onFocused: (Int) -> Unit = {},
    )

    private val models = mutableMapOf<Owner, Model>()

    var model by mutableStateOf<Model?>(null)
        private set

    /** True only after the Presentation window was shown successfully. */
    var presentationActive by mutableStateOf(false)
        private set

    fun markPresentationActive(active: Boolean) {
        presentationActive = active
    }

    fun publish(model: Model) {
        models[model.owner] = model
        this.model = models.values.maxByOrNull { it.owner.priority }
    }

    fun clear(owner: Owner) {
        models.remove(owner)
        model = models.values.maxByOrNull { it.owner.priority }
    }

    fun clearAll() {
        models.clear()
        model = null
    }

    private val Owner.priority: Int
        get() = when (this) {
            Owner.LIBRARY -> 0
            Owner.SYSTEM -> 1
            Owner.SETTINGS -> 2
            Owner.GAME_CARD -> 3
            Owner.DIALOG -> 4
            Owner.GAME -> 5
        }
}

private class PresentationLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner, OnBackPressedDispatcherOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
    override val onBackPressedDispatcher = OnBackPressedDispatcher()

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
    private val hostContext: Context,
    display: Display,
) : Presentation(hostContext, display) {

    private val presentationLifecycleOwner = PresentationLifecycleOwner()
    private lateinit var composeView: ComposeView
    private var composeWindowContext: Context? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateInputMode(DsHomeSecondScreen.model?.mode ?: DsHomeSecondScreen.Mode.DETAILS)
        // A Presentation window is TYPE_PRESENTATION (2037). Compose Dialog()
        // creates its own window with LayoutParams.type TYPE_APPLICATION (2) and
        // Android 13+ rejects that mismatch with "Window type mismatch". Build
        // the ComposeView on a TYPE_APPLICATION window context bound to the same
        // display so dialogs opened from second-screen content (settings auth,
        // quick menu, etc.) show without crashing.
        val composeContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                context.createWindowContext(
                    display,
                    WindowManager.LayoutParams.TYPE_APPLICATION,
                    null,
                ).also { composeWindowContext = it }
            }.getOrElse { context }
        } else {
            context
        }
        composeView = ComposeView(composeContext).apply {
            setViewTreeLifecycleOwner(presentationLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(presentationLifecycleOwner)
            setViewTreeOnBackPressedDispatcherOwner(presentationLifecycleOwner)
            setContent {
                // setViewTreeOnBackPressedDispatcherOwner only wires the ViewTree
                // owner; the Compose composition still needs the local explicitly,
                // otherwise nested BackHandlers (e.g. SteamAchievementsPage) throw
                // "No OnBackPressedDispatcherOwner was provided".
                CompositionLocalProvider(
                    LocalOnBackPressedDispatcherOwner provides presentationLifecycleOwner,
                    LocalActivityResultRegistryOwner provides requireNotNull(
                        hostContext as? ActivityResultRegistryOwner,
                    ) { "Dual-screen presentation requires an ActivityResultRegistryOwner host" },
                ) {
                    val appTheme = PrefManager.appTheme
                    val isDark = when (appTheme) {
                        AppTheme.AUTO -> (context.resources.configuration.uiMode and
                            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                        AppTheme.DAY -> false
                        AppTheme.NIGHT, AppTheme.AMOLED -> true
                    }
                    PluviaTheme(
                        isDark = isDark,
                        isAmoled = appTheme == AppTheme.AMOLED,
                        style = PrefManager.appThemePalette,
                        customThemeJson = PrefManager.customThemeJson
                            .takeIf { PrefManager.customThemeEnabled },
                        reduceMotion = PrefManager.reduceMotion,
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background,
                        ) {
                            val model = DsHomeSecondScreen.model
                            if (model != null) {
                                SideEffect { updateInputMode(model.mode) }
                                when (model.mode) {
                                    DsHomeSecondScreen.Mode.GRID -> DsHomeSecondScreenGrid(model)
                                    DsHomeSecondScreen.Mode.DETAILS -> DsHomeSecondScreenDetails(model)
                                    DsHomeSecondScreen.Mode.CARD -> model.cardContent?.invoke()
                                    DsHomeSecondScreen.Mode.QUICK_MENU -> SecondScreenGamePanel(
                                        model = model,
                                        showMenu = true,
                                    )
                                    DsHomeSecondScreen.Mode.QUICK_MENU_PASSIVE -> model.menuContent?.invoke()
                                    DsHomeSecondScreen.Mode.GAME_DASHBOARD -> SecondScreenGamePanel(
                                        model = model,
                                        showMenu = false,
                                    )
                                    DsHomeSecondScreen.Mode.SETTINGS -> model.settingsContent?.invoke()
                                }
                            } else {
                                SideEffect { updateInputMode(DsHomeSecondScreen.Mode.DETAILS) }
                                SecondScreenStandby()
                            }
                        }
                    }
                }
            }
        }
        setContentView(composeView)
        presentationLifecycleOwner.handlePresentationStart()
    }

    private fun updateInputMode(mode: DsHomeSecondScreen.Mode) {
        val gamePanel = mode == DsHomeSecondScreen.Mode.GAME_DASHBOARD ||
            mode == DsHomeSecondScreen.Mode.QUICK_MENU
        val passive = mode == DsHomeSecondScreen.Mode.DETAILS ||
            mode == DsHomeSecondScreen.Mode.QUICK_MENU_PASSIVE
        if (gamePanel) {
            // In-game panels stay touchable on the lower screen while hardware
            // keys remain routed to the game on the upper display.
            window?.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        } else if (passive) {
            window?.addFlags(
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            )
        } else {
            // Library, global menu and settings are controller workspaces on the
            // lower display. They must own window focus for D-pad traversal and A.
            window?.clearFlags(
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            )
            window?.decorView?.requestFocus()
        }
    }

    // The physical back button on the second display must not dismiss the
    // Presentation itself. First let the Compose tree try to consume it (the
    // game card routes it to close the card -> back to library); if nothing
    // in the composition handled it, fall back to the OnBackPressedDispatcher
    // so in-menu BackHandlers (e.g. QuickMenu dismiss) work too. Consuming the
    // event also stops the Dialog's default dismiss.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val currentModel = DsHomeSecondScreen.model
        if (event.action == KeyEvent.ACTION_DOWN && currentModel?.mode == DsHomeSecondScreen.Mode.SETTINGS) {
            when (keyCode) {
                KeyEvent.KEYCODE_BUTTON_L1,
                KeyEvent.KEYCODE_BUTTON_L2,
                -> {
                    currentModel.onPreviousTab()
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_R1,
                KeyEvent.KEYCODE_BUTTON_R2,
                -> {
                    currentModel.onNextTab()
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_B -> {
                    currentModel.onBack()
                    return true
                }
            }
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (currentModel?.mode == DsHomeSecondScreen.Mode.SETTINGS) {
                currentModel.onBack()
                return true
            }
            if (event.action == KeyEvent.ACTION_DOWN) {
                val handledInCompose = composeView.dispatchKeyEvent(event)
                if (!handledInCompose) {
                    presentationLifecycleOwner.onBackPressedDispatcher.onBackPressed()
                }
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true
        }
        return super.onKeyUp(keyCode, event)
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
    var scaleStep by remember { mutableStateOf(PrefManager.dsHomeIconScale) }
    val cellMinSize = when (scaleStep) {
        0 -> 96.dp
        2 -> 144.dp
        else -> 116.dp
    }
    LaunchedEffect(model.currentTab) {
        gridState.scrollToItem(0)
    }
    LaunchedEffect(gridState, model.items.size, model.totalItemCount) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                if (shouldLoadNextLibraryPage(lastVisibleIndex, model.items.size, model.totalItemCount)) {
                    model.onPageChange(1)
                }
            }
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
    val usesList = model.libraryLayout == PaneType.LIST ||
        model.libraryLayout == PaneType.INSTALLED_COMPACT
    val actions = listOf(
        GamepadAction(GamepadButton.A, app.gamenative.R.string.action_select),
        GamepadAction(GamepadButton.SELECT, app.gamenative.R.string.options, model.onOptions),
        GamepadAction(GamepadButton.Y, app.gamenative.R.string.search, model.onSearchToggle),
        GamepadAction(GamepadButton.B, app.gamenative.R.string.menu, model.onQuickActions),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.ButtonL1, Key.ButtonL2 -> {
                        model.onPreviousTab()
                        true
                    }
                    Key.ButtonR1, Key.ButtonR2 -> {
                        model.onNextTab()
                        true
                    }
                    Key.ButtonSelect -> {
                        model.onOptions()
                        true
                    }
                    Key.ButtonStart -> {
                        model.onSystemMenu()
                        true
                    }
                    Key.ButtonY -> {
                        model.onSearchToggle()
                        true
                    }
                    Key.ButtonX -> {
                        model.onAddGame()
                        true
                    }
                    Key.ButtonB -> {
                        model.onQuickActions()
                        true
                    }
                    else -> false
                }
            },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DualLibraryHeader(model)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))

            Box(modifier = Modifier.weight(1f)) {
                when {
                    model.isLoading && model.items.isEmpty() -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    model.items.isEmpty() -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp),
                            )
                            Text(
                                text = stringResource(app.gamenative.R.string.library_no_results),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(onClick = model.onRefresh) {
                                Text(stringResource(app.gamenative.R.string.action_refresh))
                            }
                        }
                    }
                    usesList -> DsHomeSecondScreenList(
                        model = model,
                        firstItemFocusRequester = firstItemFocusRequester,
                    )
                    else -> DsGameGrid(
                        items = model.items,
                        listState = gridState,
                        cellMinSize = cellMinSize,
                        focusTargetIndex = model.focusedIndex,
                        firstItemFocusRequester = firstItemFocusRequester,
                        onFocusedIndexChanged = model.onFocused,
                        onNavigate = model.onNavigate,
                        onScaleCycle = {
                            scaleStep = (scaleStep + 1) % 3
                            PrefManager.dsHomeIconScale = scaleStep
                        },
                        showLabels = true,
                        cellAspectRatio = 0.76f,
                        preferSquareIcon = false,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 68.dp),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        GamepadActionBar(
            actions = actions,
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = !model.isSearching,
            forceVisible = true,
            compact = true,
        )
    }
}

@Composable
private fun DualLibraryHeader(model: DsHomeSecondScreen.Model) {
    if (model.isSearching) {
        OutlinedTextField(
            value = model.searchQuery,
            onValueChange = model.onSearchQuery,
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = model.onSearchToggle) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(app.gamenative.R.string.back))
                }
            },
            label = { Text(stringResource(app.gamenative.R.string.search)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        IconButton(onClick = model.onPreviousTab) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
        }
        val currentTabCount = model.tabCounts[model.currentTab] ?: model.totalItemCount
        Text(
            text = stringResource(
                app.gamenative.R.string.library_tab_with_count,
                stringResource(model.currentTab.labelResId),
                currentTabCount,
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        IconButton(onClick = model.onNextTab) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
        if (model.lastPage > 1) {
            Text(
                text = "${model.currentPage.coerceAtMost(model.lastPage)} / ${model.lastPage}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = model.onLayoutCycle) {
            Icon(Icons.Default.GridView, contentDescription = stringResource(app.gamenative.R.string.ds_home_icon_size_action))
        }
        IconButton(onClick = model.onSearchToggle) {
            Icon(Icons.Default.Search, contentDescription = stringResource(app.gamenative.R.string.search))
        }
        IconButton(onClick = model.onOptions) {
            Icon(Icons.Default.Tune, contentDescription = stringResource(app.gamenative.R.string.options))
        }
        IconButton(onClick = model.onSystemMenu) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = stringResource(app.gamenative.R.string.system_hub_open),
            )
        }
        IconButton(onClick = model.onOpenSettings) {
            Icon(Icons.Default.Settings, contentDescription = stringResource(app.gamenative.R.string.settings_title))
        }
        IconButton(onClick = model.onAddGame) {
            Icon(Icons.Default.Add, contentDescription = stringResource(app.gamenative.R.string.action_add_game))
        }
    }
}

@Composable
private fun DsHomeSecondScreenList(
    model: DsHomeSecondScreen.Model,
    firstItemFocusRequester: FocusRequester,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(model.currentTab) {
        listState.scrollToItem(0)
    }
    LaunchedEffect(listState, model.items.size, model.totalItemCount) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                if (shouldLoadNextLibraryPage(lastVisibleIndex, model.items.size, model.totalItemCount)) {
                    model.onPageChange(1)
                }
            }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 68.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        itemsIndexed(model.items, key = { _, item -> item.appId }) { index, item ->
            DualLibraryListRow(
                item = item,
                selected = index == model.focusedIndex,
                onFocused = { model.onFocused(index) },
                onClick = { model.onNavigate(item.appId) },
                focusRequester = if (index == model.focusedIndex) firstItemFocusRequester else null,
            )
        }
    }
}

@Composable
private fun DualLibraryListRow(
    item: LibraryItem,
    selected: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    focusRequester: FocusRequester?,
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    val imageUrls by produceState(initialValue = GridImageUrls("", ""), key1 = item.appId) {
        value = withContext(Dispatchers.IO) { getGridImageUrl(context, item, PaneType.GRID_CAPSULE) }
    }
    val imageUrl = imageUrls.primary.ifEmpty { imageUrls.fallback }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { if (it.isFocused) onFocused() }
            .clip(shape)
            .background(
                if (selected || isFocused) MaterialTheme.colorScheme.surfaceContainerHigh
                else MaterialTheme.colorScheme.surfaceContainerLow,
            )
            .focusRing(interactionSource, shape, width = 2.dp)
            .selectable(
                selected = isFocused,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = item.name }
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier
                .width(96.dp)
                .height(60.dp),
            shape = RoundedCornerShape(7.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            if (imageUrl.isNotEmpty()) {
                CoilImage(
                    modifier = Modifier.fillMaxSize(),
                    imageModel = { imageUrl },
                    imageOptions = ImageOptions(contentScale = ContentScale.Crop, contentDescription = null),
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                GameSourceIcon(gameSource = item.gameSource, iconSize = 15, alignmentBoxSize = 20)
                if (item.sizeBytes > 0) {
                    Text(
                        text = formatBytes(item.sizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
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
                text = stringResource(app.gamenative.R.string.second_screen_no_game_selected),
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

@Composable
private fun SecondScreenStandby() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Gamepad,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(42.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(app.gamenative.R.string.second_screen_ready),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(app.gamenative.R.string.second_screen_ready_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun SecondScreenGamePanel(
    model: DsHomeSecondScreen.Model,
    showMenu: Boolean,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            InGamePanelTab(
                text = stringResource(app.gamenative.R.string.second_screen_tab_game),
                selected = !showMenu,
                onClick = model.onShowDashboard,
            )
            InGamePanelTab(
                text = stringResource(app.gamenative.R.string.quick_menu_title),
                selected = showMenu,
                onClick = model.onShowMenu,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (showMenu) {
                model.menuContent?.invoke()
            } else {
                SecondScreenGameDashboard(model)
            }
        }
    }
}

@Composable
private fun RowScope.InGamePanelTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .height(48.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SecondScreenGameDashboard(model: DsHomeSecondScreen.Model) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier
                    .width(128.dp)
                    .height(184.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                if (model.dashboardImageUrl.isNotBlank()) {
                    CoilImage(
                        modifier = Modifier.fillMaxSize(),
                        imageModel = { model.dashboardImageUrl },
                        imageOptions = ImageOptions(
                            contentScale = ContentScale.Crop,
                            contentDescription = null,
                        ),
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(app.gamenative.R.string.second_screen_game_running),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (model.dashboardLogoUrl.isNotBlank()) {
                    CoilImage(
                        imageModel = { model.dashboardLogoUrl },
                        imageOptions = ImageOptions(
                            contentScale = ContentScale.Fit,
                            contentDescription = model.dashboardTitle,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp)
                            .padding(top = 8.dp),
                    )
                } else {
                    Text(
                        text = model.dashboardTitle.ifBlank {
                            stringResource(app.gamenative.R.string.second_screen_game_running)
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (model.dashboardSubtitle.isNotBlank()) {
                    Text(
                        text = model.dashboardSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (model.performanceHudEnabled) {
                    key(model.performanceHudKey) {
                        AndroidView(
                            factory = { context ->
                                PerformanceHudView(
                                    context = context,
                                    fpsProvider = model.performanceHudFpsProvider,
                                    initialConfig = model.performanceHudConfig,
                                    initialCompactMode = true,
                                )
                            },
                            update = { hud -> hud.setConfig(model.performanceHudConfig) },
                            modifier = Modifier
                                .padding(top = 18.dp)
                                .wrapContentSize(Alignment.CenterStart),
                        )
                    }
                }
            }
        }
    }
}

/** Keeps the external-display surface alive while the feature is enabled. */
@Composable
fun DsHomePresentationHost() {
    val context = LocalContext.current
    val secondScreenEnabled = app.gamenative.PrefManager.dualScreenLauncherState.value
    val display = rememberExternalDisplay()

    // When the master switch is turned off, drop any published model so the
    // presentation is dismissed and the second screen goes clean immediately.
    DisposableEffect(secondScreenEnabled) {
        if (!secondScreenEnabled) {
            DsHomeSecondScreen.clearAll()
        }
        onDispose { }
    }

    DisposableEffect(secondScreenEnabled, display?.displayId) {
        if (!secondScreenEnabled || display == null) {
            DsHomeSecondScreen.markPresentationActive(false)
            return@DisposableEffect onDispose { }
        }
        val presentation = DsHomePresentation(context, display)
        runCatching { presentation.show() }
            .onSuccess { DsHomeSecondScreen.markPresentationActive(true) }
            .onFailure { Timber.e(it, "Failed to show DS_HOME presentation") }
        onDispose {
            DsHomeSecondScreen.markPresentationActive(false)
            runCatching { presentation.dismiss() }
        }
    }
}

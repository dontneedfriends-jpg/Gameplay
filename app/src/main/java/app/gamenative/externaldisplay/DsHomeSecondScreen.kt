package app.gamenative.externaldisplay

import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
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
import app.gamenative.data.LibraryItem
import app.gamenative.ui.screen.library.components.DsGameGrid
import app.gamenative.ui.theme.PluviaTheme
import timber.log.Timber

/**
 * Bridge between the main-screen DS_HOME state and the second-display
 * presentation. The main screen publishes the model; the presentation's own
 * composition subscribes to it.
 */
object DsHomeSecondScreen {
    class Model(
        val items: List<LibraryItem>,
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
                            DsHomeSecondScreenGrid(model)
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
    val cellMinSize = when (PrefManager.dsHomeIconScale) {
        0 -> 72.dp
        2 -> 128.dp
        else -> 96.dp
    }
    DsGameGrid(
        items = model.items,
        listState = gridState,
        cellMinSize = cellMinSize,
        focusTargetIndex = null,
        firstItemFocusRequester = null,
        onFocusedIndexChanged = model.onFocused,
        onNavigate = model.onNavigate,
        onScaleCycle = {
            PrefManager.dsHomeIconScale = (PrefManager.dsHomeIconScale + 1) % 3
        },
        modifier = Modifier.fillMaxSize(),
    )
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

package app.gamenative.utils

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Dual-screen handheld detection (AYN Thor and similar clamshell Android
 * gaming devices). Two ways to be "dual-screen": a known dual-screen model,
 * or a second physical display currently attached.
 */
object DualScreenDevice {

    private val knownDualScreenModels = setOf(
        "thor",
        "ayn_thor",
        "ayn-thor",
    )

    fun isKnownDualScreenModel(
        manufacturer: String = Build.MANUFACTURER,
        model: String = Build.MODEL,
        device: String = Build.DEVICE,
    ): Boolean {
        val haystack = listOf(manufacturer, model, device)
            .joinToString(" ")
            .lowercase()
        return knownDualScreenModels.any { it in haystack }
    }

    fun hasExternalDisplay(context: Context): Boolean {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            ?: return false
        return displayManager.displays.any { it.displayId != Display.DEFAULT_DISPLAY }
    }

    fun isDualScreen(context: Context): Boolean =
        isKnownDualScreenModel() || hasExternalDisplay(context)
}

@Composable
fun rememberIsDualScreenDevice(): Boolean {
    val context = LocalContext.current
    return remember { DualScreenDevice.isDualScreen(context) }
}

@Composable
fun rememberHasExternalDisplay(): Boolean {
    val context = LocalContext.current
    return remember { DualScreenDevice.hasExternalDisplay(context) }
}

package app.gamenative.ui.component.dialog

import android.os.IBinder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.window.DialogProperties

/**
 * Window type/token for dialogs opened from second-screen (Presentation) content.
 *
 * A Presentation window is TYPE_PRESENTATION (2037). Compose Dialog() creates its
 * own window with LayoutParams.type TYPE_APPLICATION (2) and Android 13+ rejects
 * that on a secondary display with "BadTokenException: token null". The
 * presentation host provides its window token here so dialogs opened from
 * settings / quick-menu / game-card content bind to the same presentation window
 * instead of crashing. On the main display these locals stay null and dialogs
 * behave as before.
 */
val LocalSecondScreenDialogWindowType = staticCompositionLocalOf<Int?> { null }
val LocalSecondScreenDialogWindowToken = staticCompositionLocalOf<IBinder?> { null }

/**
 * Returns [base] enriched with the second-screen window type/token when the
 * current composition is hosted inside a Presentation (both locals set).
 */
@Composable
fun secondScreenDialogProperties(base: DialogProperties = DialogProperties()): DialogProperties {
    val windowType = LocalSecondScreenDialogWindowType.current
    val windowToken = LocalSecondScreenDialogWindowToken.current
    if (windowType == null || windowToken == null) {
        if (DEBUG_LOCAL_LOGS) {
            android.util.Log.d(
                "SecondScreenDialogProps",
                "SKIPPED type=$windowType token=${windowToken != null} from=${Thread.currentThread().stackTrace[2].methodName}",
            )
        }
        return base
    }
    val result = DialogProperties(
        dismissOnBackPress = base.dismissOnBackPress,
        dismissOnClickOutside = base.dismissOnClickOutside,
        usePlatformDefaultWidth = base.usePlatformDefaultWidth,
        decorFitsSystemWindows = base.decorFitsSystemWindows,
        windowType = windowType,
        windowToken = windowToken,
    )
    if (DEBUG_LOCAL_LOGS) {
        android.util.Log.d("SecondScreenDialogProps", "applied type=$windowType token=${windowToken != null} from=${Thread.currentThread().stackTrace[2].methodName}")
    }
    return result
}

private const val DEBUG_LOCAL_LOGS = true

package app.gamenative.ui.component

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Stable high-contrast outline shared by controller-navigable components.
 *
 * Focus remains obvious without continuous animation or decorative neon effects.
 * The duration parameter remains for source compatibility with existing call sites.
 */
@Composable
fun Modifier.focusRing(
    interactionSource: InteractionSource,
    shape: Shape,
    width: Dp = 4.dp,
    @Suppress("UNUSED_PARAMETER") durationMillis: Int = 5000,
): Modifier {
    val focused by interactionSource.collectIsFocusedAsState()
    return focusRing(focused, shape, width)
}

/**
 * Same ring driven by an explicit focus flag, for components whose
 * InteractionSource is not exposed (e.g. library settings tiles). Pair with
 * `Modifier.onFocusChanged`.
 */
@Composable
fun Modifier.focusRing(
    focused: Boolean,
    shape: Shape,
    width: Dp = 4.dp,
): Modifier {
    if (!focused) return this

    val color = MaterialTheme.colorScheme.tertiary
    val strokePx = with(LocalDensity.current) { width.toPx() }

    return drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        onDrawWithContent {
            drawContent()
            drawOutline(outline, color = color, style = Stroke(strokePx))
        }
    }
}

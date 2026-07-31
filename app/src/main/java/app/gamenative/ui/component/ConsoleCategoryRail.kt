package app.gamenative.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Shared controller-first category navigation for complex settings surfaces. */
@Composable
fun <T> ConsoleCategoryRail(
    items: List<T>,
    selectedItem: T,
    label: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    footer: String,
    modifier: Modifier = Modifier,
    requestInitialFocus: Boolean = false,
    compact: Boolean = false,
) {
    val initialFocusRequester = remember { FocusRequester() }
    LaunchedEffect(requestInitialFocus, items) {
        if (requestInitialFocus && items.isNotEmpty()) runCatching { initialFocusRequester.requestFocus() }
    }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(
                horizontal = if (compact) 10.dp else 14.dp,
                vertical = if (compact) 10.dp else 16.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp),
    ) {
        items.forEach { item ->
            val interactionSource = remember(item) { MutableInteractionSource() }
            val focused by interactionSource.collectIsFocusedAsState()
            val selected = item == selectedItem
            val shape = RoundedCornerShape(10.dp)
            Text(
                text = label(item),
                modifier = Modifier
                    .then(if (requestInitialFocus && item == items.firstOrNull()) Modifier.focusRequester(initialFocusRequester) else Modifier)
                    .fillMaxWidth()
                    .focusRing(interactionSource, shape, width = 2.dp)
                    .clip(shape)
                    .background(
                        when {
                            selected -> MaterialTheme.colorScheme.primaryContainer
                            focused -> MaterialTheme.colorScheme.surfaceContainerHighest
                            else -> Color.Transparent
                        },
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onSelected(item) },
                    )
                    .padding(
                        horizontal = if (compact) 12.dp else 16.dp,
                        vertical = if (compact) 8.dp else 13.dp,
                    ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    selected -> MaterialTheme.colorScheme.onPrimaryContainer
                    focused -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = footer,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = if (compact) 2.dp else 6.dp,
            ),
        )
    }
}

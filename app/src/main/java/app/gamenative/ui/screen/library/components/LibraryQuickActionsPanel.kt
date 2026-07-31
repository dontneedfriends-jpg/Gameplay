package app.gamenative.ui.screen.library.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.gamenative.R
import app.gamenative.data.GameSource
import app.gamenative.data.LibraryItem
import app.gamenative.ui.component.focusRing
import app.gamenative.ui.util.adaptivePanelWidth

/** Context-sensitive library menu opened by the controller B/Circle button. */
@Composable
fun LibraryQuickActionsPanel(
    isOpen: Boolean,
    focusedItem: LibraryItem?,
    onDismiss: () -> Unit,
    onPrimaryAction: (LibraryItem) -> Unit,
    onDetails: (LibraryItem) -> Unit,
    onLibraryOptions: () -> Unit,
    onSearch: () -> Unit,
    onAddGame: () -> Unit,
) {
    BackHandler(enabled = isOpen, onBack = onDismiss)
    val firstFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isOpen, focusedItem?.appId) {
        if (isOpen) {
            kotlinx.coroutines.delay(80)
            runCatching { firstFocusRequester.requestFocus() }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn(tween(140)),
            exit = fadeOut(tween(110)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.58f))
                    .clickable(onClick = onDismiss),
            )
        }

        AnimatedVisibility(
            visible = isOpen,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally(tween(180)) { it },
            exit = slideOutHorizontally(tween(140)) { it },
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(adaptivePanelWidth(460.dp)),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = stringResource(R.string.quick_menu_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    focusedItem?.let { item ->
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        if (!item.isRecommended) {
                            val canRunImmediately = item.isInstalled || item.gameSource == GameSource.CUSTOM_GAME
                            QuickActionItem(
                                icon = if (canRunImmediately) Icons.Default.PlayArrow else Icons.Default.Download,
                                label = stringResource(if (canRunImmediately) R.string.run_app else R.string.install_app),
                                onClick = { onPrimaryAction(item) },
                                modifier = Modifier.focusRequester(firstFocusRequester),
                                emphasized = true,
                            )
                        }
                        QuickActionItem(
                            icon = Icons.Default.Info,
                            label = stringResource(R.string.action_details),
                            onClick = { onDetails(item) },
                            modifier = if (item.isRecommended) Modifier.focusRequester(firstFocusRequester) else Modifier,
                        )
                    }

                    QuickActionItem(
                        icon = Icons.Default.Tune,
                        label = stringResource(R.string.options),
                        onClick = onLibraryOptions,
                        modifier = if (focusedItem == null) Modifier.focusRequester(firstFocusRequester) else Modifier,
                    )
                    QuickActionItem(
                        icon = Icons.Default.Search,
                        label = stringResource(R.string.search),
                        onClick = onSearch,
                    )
                    if (focusedItem == null) {
                        QuickActionItem(
                            icon = Icons.Default.Add,
                            label = stringResource(R.string.action_add_game),
                            onClick = onAddGame,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.console_back_hint),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .focusRing(interactionSource, shape, width = 2.dp)
            .background(
                when {
                    focused -> MaterialTheme.colorScheme.primaryContainer
                    emphasized -> MaterialTheme.colorScheme.surfaceContainerHigh
                    else -> Color.Transparent
                },
                shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(23.dp),
            tint = if (focused) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (focused || emphasized) FontWeight.SemiBold else FontWeight.Normal,
            color = if (focused) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        )
    }
}

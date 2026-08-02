package app.gamenative.ui.screen.library.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.gamenative.R
import app.gamenative.data.LibraryItem
import app.gamenative.ui.component.focusRing
import app.gamenative.ui.data.LibraryState
import app.gamenative.ui.data.statsFor
import app.gamenative.ui.enums.PaneType
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Switch-style home row over a blurred hero backdrop of the focused game.
 *
 * Works on every library tab:
 * - INSTALLED: square icon cells with the game name under the icon
 *   (left-aligned), row scrolls circularly left/right.
 * - ALL / store tabs: regular info cards (same look as the grid card view).
 */
@Composable
internal fun LibraryCompactRowPane(
    state: LibraryState,
    isInstalledTab: Boolean,
    firstItemFocusRequester: FocusRequester? = null,
    focusTargetListIndex: Int? = null,
    onFocusedIndexChanged: (Int) -> Unit = {},
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = state.appInfoList
    val itemCount = items.size
    val focusedIndex = (focusTargetListIndex ?: 0).coerceIn(0, itemCount.coerceAtLeast(1) - 1)
    val listState = rememberLazyListState()

    // Circular scroll: start in the middle of a virtually infinite row.
    LaunchedEffect(itemCount, isInstalledTab) {
        if (isInstalledTab && itemCount > 1) {
            val middle = Int.MAX_VALUE / 2 - (Int.MAX_VALUE / 2 % itemCount)
            listState.scrollToItem(middle)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LibraryDynamicBackdrop(
            appInfo = items.getOrNull(focusedIndex),
            imageRefreshCounter = state.imageRefreshCounter,
            modifier = Modifier.fillMaxSize(),
        )

        if (items.isEmpty()) {
            Text(
                text = stringResource(R.string.library_empty_installed),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (isInstalledTab) {
            LazyRow(
                state = listState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 84.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(
                    count = if (itemCount > 1) Int.MAX_VALUE else itemCount,
                    key = { rawIndex -> rawIndex.toLong() * 31 + (rawIndex % itemCount.coerceAtLeast(1)) },
                ) { rawIndex ->
                    val index = rawIndex % itemCount
                    val item = items[index]
                    CompactIconCell(
                        item = item,
                        onClick = { onNavigate(item.appId) },
                        onFocused = { onFocusedIndexChanged(index) },
                        focusRequester = if (firstItemFocusRequester != null && index == focusTargetListIndex) {
                            firstItemFocusRequester
                        } else {
                            null
                        },
                    )
                }
            }
        } else {
            LazyRow(
                state = listState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 84.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    count = itemCount,
                    key = { index -> items[index].appId },
                ) { index ->
                    val item = items[index]
                    Box(
                        modifier = Modifier
                            .width(170.dp)
                            .aspectRatio(2f / 3f),
                    ) {
                        AppItem(
                            appInfo = item,
                            onClick = { onNavigate(item.appId) },
                            paneType = PaneType.GRID_CAPSULE,
                            onFocus = { onFocusedIndexChanged(index) },
                            modifier = if (firstItemFocusRequester != null && index == focusTargetListIndex) {
                                Modifier.focusRequester(firstItemFocusRequester)
                            } else {
                                Modifier
                            },
                            imageRefreshCounter = state.imageRefreshCounter,
                            compatibilityStatus = state.compatibilityMap[item.name],
                            gameStats = state.statsFor(item),
                        )
                    }
                }
            }
        }
    }
}

/** Square icon + left-aligned name underneath, used on the INSTALLED tab. */
@Composable
private fun CompactIconCell(
    item: LibraryItem,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.07f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "compactIconCellScale",
    )

    val imageUrls by produceState(
        initialValue = GridImageUrls("", ""),
        key1 = item.appId,
    ) {
        value = withContext(Dispatchers.IO) {
            getGridImageUrl(context, item, PaneType.GRID_CAPSULE)
        }
    }
    val imageUrl = imageUrls.primary.ifEmpty { imageUrls.fallback }

    Column(
        modifier = Modifier
            .width(132.dp)
            .scale(scale)
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                },
            )
            .onFocusChanged { if (it.isFocused) onFocused() },
    ) {
        Box(
            modifier = Modifier
                .width(132.dp)
                .aspectRatio(1f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .focusRing(interactionSource, shape, width = 2.dp)
                .selectable(
                    selected = isFocused,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (imageUrl.isNotEmpty()) {
                CoilImage(
                    modifier = Modifier.fillMaxSize(),
                    imageModel = { imageUrl },
                    imageOptions = ImageOptions(contentScale = ContentScale.Crop),
                )
            }
        }
        Text(
            text = item.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isFocused) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
        )
    }
}

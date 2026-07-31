package app.gamenative.ui.screen.settings

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.gamenative.PrefManager
import app.gamenative.R
import app.gamenative.enums.AppTheme
import app.gamenative.ui.component.focusRing
import app.gamenative.ui.component.ConsoleCategoryRail
import app.gamenative.ui.theme.PluviaTheme
import com.materialkolor.PaletteStyle

private enum class SettingsCategory(val titleRes: Int, val icon: ImageVector) {
    EMULATION(R.string.settings_emulation_title, Icons.Default.Gamepad),
    APPEARANCE(R.string.settings_interface_title, Icons.Default.Palette),
    LIBRARY(R.string.settings_interface_custom_games, Icons.Default.LibraryBooks),
    DOWNLOADS(R.string.settings_downloads_title, Icons.Default.Download),
    INFO(R.string.settings_info_title, Icons.Default.Info),
    DEBUG(R.string.settings_debug_title, Icons.Default.BugReport),
}

@Composable
fun SettingsScreen(
    appTheme: AppTheme,
    paletteStyle: PaletteStyle,
    onAppTheme: (AppTheme) -> Unit,
    onPaletteStyle: (PaletteStyle) -> Unit,
    onBack: () -> Unit,
) {
    SettingsScreenContent(
        appTheme = appTheme,
        paletteStyle = paletteStyle,
        onAppTheme = onAppTheme,
        onPaletteStyle = onPaletteStyle,
        onBack = onBack,
    )
}

@Composable
private fun SettingsScreenContent(
    appTheme: AppTheme,
    paletteStyle: PaletteStyle,
    onAppTheme: (AppTheme) -> Unit,
    onPaletteStyle: (PaletteStyle) -> Unit,
    onBack: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val categories = remember { SettingsCategory.entries.toList() }
    var selectedCategory by rememberSaveable { mutableStateOf(SettingsCategory.EMULATION) }
    val categoryRailWidth = if (LocalConfiguration.current.screenWidthDp < 600) 156.dp else 228.dp

    LaunchedEffect(selectedCategory) {
        scrollState.scrollTo(0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .displayCutoutPadding(),
        ) {
            SettingsHeader(
                onBack = onBack,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        val currentIndex = categories.indexOf(selectedCategory)
                        when (event.key) {
                            Key.ButtonR1, Key.ButtonR2 -> {
                                selectedCategory = categories[(currentIndex + 1) % categories.size]
                                true
                            }
                            Key.ButtonL1, Key.ButtonL2 -> {
                                selectedCategory = categories[(currentIndex - 1 + categories.size) % categories.size]
                                true
                            }
                            else -> false
                        }
                    },
            ) {
                ConsoleCategoryRail(
                    items = categories,
                    selectedItem = selectedCategory,
                    label = { stringResource(it.titleRes) },
                    onSelected = { selectedCategory = it },
                    footer = stringResource(R.string.container_config_console_controls_hint),
                    requestInitialFocus = true,
                    modifier = Modifier.width(categoryRailWidth),
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(start = 22.dp, end = 22.dp, bottom = 32.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = selectedCategory.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = stringResource(selectedCategory.titleRes),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    Spacer(modifier = Modifier.height(8.dp))

                    when (selectedCategory) {
                        SettingsCategory.EMULATION -> SettingsGroupEmulation()
                        SettingsCategory.APPEARANCE -> SettingsGroupInterface(
                            appTheme = appTheme,
                            paletteStyle = paletteStyle,
                            onAppTheme = onAppTheme,
                            onPaletteStyle = onPaletteStyle,
                            section = InterfaceSettingsSection.APPEARANCE,
                        )
                        SettingsCategory.LIBRARY -> SettingsGroupInterface(
                            appTheme = appTheme,
                            paletteStyle = paletteStyle,
                            onAppTheme = onAppTheme,
                            onPaletteStyle = onPaletteStyle,
                            section = InterfaceSettingsSection.LIBRARY,
                        )
                        SettingsCategory.DOWNLOADS -> SettingsGroupInterface(
                            appTheme = appTheme,
                            paletteStyle = paletteStyle,
                            onAppTheme = onAppTheme,
                            onPaletteStyle = onPaletteStyle,
                            section = InterfaceSettingsSection.DOWNLOADS,
                        )
                        SettingsCategory.INFO -> SettingsGroupInfo()
                        SettingsCategory.DEBUG -> SettingsGroupDebug()
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsHeader(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BackButton(onClick = onBack)

        // Title
        Column {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = PluviaTheme.colors.textMuted,
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(shape)
            .background(
                if (isFocused) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    PluviaTheme.colors.surfaceElevated
                },
            )
            .focusRing(interactionSource, shape, width = 2.dp)
            .selectable(
                selected = isFocused,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = stringResource(R.string.back),
            tint = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    device = "spec:width=1920px,height=1080px,dpi=440,orientation=landscape",
)
@Composable
private fun Preview_SettingsScreen() {
    val isPreview = LocalInspectionMode.current
    if (!isPreview) {
        val context = LocalContext.current
        PrefManager.init(context)
    }
    PluviaTheme {
        SettingsScreenContent(
            appTheme = AppTheme.DAY,
            paletteStyle = PaletteStyle.TonalSpot,
            onAppTheme = { },
            onPaletteStyle = { },
            onBack = { },
        )
    }
}

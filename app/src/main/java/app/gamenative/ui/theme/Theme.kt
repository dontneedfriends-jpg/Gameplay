package app.gamenative.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.alorma.compose.settings.ui.base.internal.SettingsTileColors
import com.alorma.compose.settings.ui.base.internal.SettingsTileDefaults
import com.materialkolor.PaletteStyle

/**
 * Custom color system for Pluvia, extending Material3.
 * Provides app-specific colors beyond the Material ColorScheme.
 */
@Immutable
data class PluviaColors(
    // Status colors
    val statusInstalled: Color,
    val statusDownloading: Color,
    val statusAvailable: Color,
    val statusAway: Color,
    val statusOffline: Color,

    // Friend status
    val friendOnline: Color,
    val friendOffline: Color,
    val friendInGame: Color,
    val friendAwayOrSnooze: Color,
    val friendInGameAwayOrSnooze: Color,
    val friendBlocked: Color,

    // Accents
    val accentCyan: Color,
    val accentPurple: Color,
    val accentPink: Color,
    val accentSuccess: Color,
    val accentWarning: Color,
    val accentDanger: Color,

    // Surfaces
    val surfacePanel: Color,
    val surfaceElevated: Color,

    // Utility
    val borderDefault: Color,
    val textMuted: Color,

    // Compatibility
    val compatibilityGood: Color,
    val compatibilityGoodBackground: Color,
    val compatibilityPartial: Color,
    val compatibilityPartialBackground: Color,
    val compatibilityUnknown: Color,
    val compatibilityUnknownBackground: Color,
    val compatibilityBad: Color,
    val compatibilityBadBackground: Color,
)

/**
 * Dark theme color palette.
 */
private fun pluviaColors(
    isDark: Boolean,
    isAmoled: Boolean,
    accent: Color,
    customPalette: GameplayThemePalette? = null,
    highContrast: Boolean = false,
): PluviaColors = PluviaColors(
    statusInstalled = customPalette?.success?.let(GameplayThemeCodec::color) ?: StatusInstalled,
    statusDownloading = StatusDownloading,
    statusAvailable = StatusAvailable,
    statusAway = StatusAway,
    statusOffline = StatusOffline,

    friendOnline = FriendOnline,
    friendOffline = FriendOffline,
    friendInGame = FriendInGame,
    friendAwayOrSnooze = FriendAwayOrSnooze,
    friendInGameAwayOrSnooze = FriendInGameAwayOrSnooze,
    friendBlocked = FriendBlocked,

    accentCyan = customPalette?.primary?.let(GameplayThemeCodec::color) ?: accent,
    accentPurple = lerp(accent, PluviaPurple, 0.42f),
    accentPink = lerp(accent, PluviaPink, 0.58f),
    accentSuccess = customPalette?.success?.let(GameplayThemeCodec::color) ?: PluviaSuccess,
    accentWarning = customPalette?.warning?.let(GameplayThemeCodec::color) ?: PluviaWarning,
    accentDanger = customPalette?.danger?.let(GameplayThemeCodec::color) ?: PluviaDanger,

    surfacePanel = when {
        isAmoled -> Color(0xFF050607)
        customPalette != null -> GameplayThemeCodec.color(customPalette.surface)
        isDark -> PluviaSurface
        else -> Color(0xFFF2F5F7)
    },
    surfaceElevated = when {
        isAmoled -> Color(0xFF101214)
        customPalette != null -> GameplayThemeCodec.color(customPalette.surfaceElevated)
        isDark -> PluviaSurfaceElevated
        else -> Color(0xFFE4E9ED)
    },

    borderDefault = customPalette?.border?.let(GameplayThemeCodec::color) ?: when {
        highContrast && isDark -> Color(0xFFD7DEE3)
        highContrast -> Color(0xFF293138)
        isDark -> PluviaBorder
        else -> Color(0xFF66727C)
    },
    textMuted = customPalette?.textMuted?.let(GameplayThemeCodec::color) ?: when {
        highContrast && isDark -> Color(0xFFE1E6EA)
        highContrast -> Color(0xFF252B30)
        isDark -> PluviaForegroundMuted
        else -> Color(0xFF4E5B66)
    },

    compatibilityGood = CompatibilityGood,
    compatibilityGoodBackground = CompatibilityGoodBg,
    compatibilityPartial = CompatibilityPartial,
    compatibilityPartialBackground = CompatibilityPartialBg,
    compatibilityUnknown = CompatibilityUnknown,
    compatibilityUnknownBackground = CompatibilityUnknownBg,
    compatibilityBad = CompatibilityBad,
    compatibilityBadBackground = CompatibilityBadBg,
)

private val DarkPluviaColors = pluviaColors(
    isDark = true,
    isAmoled = false,
    accent = PluviaPrimary,
)

val BrandGradient = listOf(PluviaCyan, PluviaPurple, PluviaPink)

private val LocalPluviaColors = staticCompositionLocalOf { DarkPluviaColors }

/**
 * Material3 dark color scheme using Pluvia colors.
 */
private fun themeAccent(seedColor: Color, style: PaletteStyle, isDark: Boolean): Color = when (style.name) {
    "Monochrome" -> if (isDark) Color.White else Color(0xFF111315)
    "Neutral" -> Color(0xFF87939D)
    "Vibrant" -> Color(0xFF65A5C7)
    "Expressive" -> Color(0xFF8F94C9)
    "Rainbow" -> Color(0xFF6F977E)
    "FruitSalad" -> Color(0xFFA47B5D)
    "Fidelity" -> Color(0xFF967080)
    "Content" -> Color(0xFF668B91)
    else -> Color(0xFF6F91AA)
}

private fun darkScheme(accent: Color, isAmoled: Boolean) = darkColorScheme(
    primary = accent,
    onPrimary = PluviaForeground,
    primaryContainer = lerp(if (isAmoled) Color.Black else PluviaCard, accent, 0.24f),
    onPrimaryContainer = PluviaForeground,

    secondary = PluviaSecondary,
    onSecondary = PluviaForeground,
    secondaryContainer = PluviaSecondary.copy(alpha = 0.8f),
    onSecondaryContainer = PluviaForeground,

    tertiary = lerp(accent, PluviaCyan, 0.45f),
    onTertiary = PluviaForeground,
    tertiaryContainer = lerp(if (isAmoled) Color.Black else PluviaCard, accent, 0.18f),
    onTertiaryContainer = PluviaForeground,

    background = if (isAmoled) Color.Black else PluviaBackground,
    onBackground = PluviaForeground,

    surface = if (isAmoled) Color(0xFF050607) else PluviaCard,
    onSurface = PluviaForeground,
    surfaceVariant = PluviaSecondary,
    onSurfaceVariant = PluviaForegroundMuted,
    surfaceTint = accent,

    inverseSurface = PluviaForeground,
    inverseOnSurface = PluviaBackground,
    inversePrimary = accent,

    error = PluviaDestructive,
    onError = PluviaForeground,
    errorContainer = PluviaDestructive.copy(alpha = 0.2f),
    onErrorContainer = PluviaForeground,

    outline = PluviaForegroundMuted,
    outlineVariant = PluviaSecondary,

    scrim = Color.Black.copy(alpha = 0.5f),
    surfaceBright = PluviaSecondary,
    surfaceDim = if (isAmoled) Color.Black else PluviaBackground,
    surfaceContainer = if (isAmoled) Color(0xFF080A0B) else PluviaCard,
    surfaceContainerHigh = if (isAmoled) Color(0xFF111416) else PluviaSecondary,
    surfaceContainerHighest = if (isAmoled) Color(0xFF181C1F) else PluviaSecondary.copy(alpha = 0.9f),
    surfaceContainerLow = if (isAmoled) Color(0xFF030405) else PluviaBackground,
    surfaceContainerLowest = if (isAmoled) Color.Black else PluviaBackground,
)

private fun lightScheme(accent: Color) = lightColorScheme(
    primary = lerp(accent, Color.Black, 0.34f),
    onPrimary = Color.White,
    primaryContainer = lerp(Color(0xFFEAF0F3), accent, 0.18f),
    onPrimaryContainer = Color(0xFF102733),
    secondary = Color(0xFF526572),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE6EB),
    onSecondaryContainer = Color(0xFF17252D),
    tertiary = Color(0xFF665C7A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE9E1F2),
    onTertiaryContainer = Color(0xFF282035),
    background = Color(0xFFF7F9FA),
    onBackground = Color(0xFF182026),
    surface = Color(0xFFF0F3F5),
    onSurface = Color(0xFF182026),
    surfaceVariant = Color(0xFFDDE3E7),
    onSurfaceVariant = Color(0xFF48545D),
    surfaceTint = accent,
    error = Color(0xFF8B3035),
    onError = Color.White,
    errorContainer = Color(0xFFFFDADB),
    onErrorContainer = Color(0xFF3A0710),
    outline = Color(0xFF69757D),
    outlineVariant = Color(0xFFC4CDD2),
    scrim = Color.Black.copy(alpha = 0.42f),
    surfaceBright = Color(0xFFF7F9FA),
    surfaceDim = Color(0xFFD7DDE1),
    surfaceContainer = Color(0xFFEBEFF1),
    surfaceContainerHigh = Color(0xFFE4E9EC),
    surfaceContainerHighest = Color(0xFFDDE3E7),
    surfaceContainerLow = Color(0xFFF1F4F5),
    surfaceContainerLowest = Color.White,
)

private fun ColorScheme.withHighContrast(isDark: Boolean, isAmoled: Boolean): ColorScheme = if (isDark) {
    val background = if (isAmoled) Color.Black else Color(0xFF07090B)
    copy(
        primary = Color.White,
        onPrimary = Color.Black,
        primaryContainer = Color(0xFFE6EBEF),
        onPrimaryContainer = Color.Black,
        background = background,
        onBackground = Color.White,
        surface = if (isAmoled) Color(0xFF050505) else Color(0xFF101316),
        onSurface = Color.White,
        surfaceVariant = Color(0xFF1A1F23),
        onSurfaceVariant = Color(0xFFE1E6EA),
        outline = Color(0xFFD7DEE3),
        outlineVariant = Color(0xFF8B969E),
        surfaceContainerLow = background,
        surfaceContainer = Color(0xFF101316),
        surfaceContainerHigh = Color(0xFF181D21),
        surfaceContainerHighest = Color(0xFF20262B),
    )
} else {
    copy(
        primary = Color(0xFF111315),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF24292D),
        onPrimaryContainer = Color.White,
        background = Color.White,
        onBackground = Color.Black,
        surface = Color(0xFFF5F7F8),
        onSurface = Color.Black,
        surfaceVariant = Color(0xFFE4E8EA),
        onSurfaceVariant = Color(0xFF252B30),
        outline = Color(0xFF293138),
        outlineVariant = Color(0xFF59636A),
        surfaceContainerLow = Color.White,
        surfaceContainer = Color(0xFFF5F7F8),
        surfaceContainerHigh = Color(0xFFE9EDEF),
        surfaceContainerHighest = Color(0xFFDDE2E5),
    )
}

private fun ColorScheme.withCustomPalette(
    palette: GameplayThemePalette,
    isAmoled: Boolean,
): ColorScheme {
    val primary = GameplayThemeCodec.color(palette.primary)
    val background = if (isAmoled) Color.Black else GameplayThemeCodec.color(palette.background)
    val surface = if (isAmoled) Color(0xFF050505) else GameplayThemeCodec.color(palette.surface)
    val elevated = if (isAmoled) Color(0xFF101010) else GameplayThemeCodec.color(palette.surfaceElevated)
    val onBackground = GameplayThemeCodec.color(palette.onBackground)
    val onSurface = GameplayThemeCodec.color(palette.onSurface)
    return copy(
        primary = primary,
        onPrimary = GameplayThemeCodec.color(palette.onPrimary),
        primaryContainer = lerp(surface, primary, 0.22f),
        onPrimaryContainer = onSurface,
        secondary = lerp(primary, onSurface, 0.36f),
        onSecondary = surface,
        secondaryContainer = elevated,
        onSecondaryContainer = onSurface,
        tertiary = lerp(primary, GameplayThemeCodec.color(palette.success), 0.45f),
        onTertiary = surface,
        tertiaryContainer = lerp(surface, primary, 0.14f),
        onTertiaryContainer = onSurface,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = elevated,
        onSurfaceVariant = GameplayThemeCodec.color(palette.textMuted),
        surfaceTint = primary,
        error = GameplayThemeCodec.color(palette.danger),
        onError = surface,
        errorContainer = lerp(surface, GameplayThemeCodec.color(palette.danger), 0.24f),
        onErrorContainer = onSurface,
        outline = GameplayThemeCodec.color(palette.border),
        outlineVariant = lerp(surface, GameplayThemeCodec.color(palette.border), 0.64f),
        surfaceBright = elevated,
        surfaceDim = background,
        surfaceContainer = surface,
        surfaceContainerHigh = elevated,
        surfaceContainerHighest = lerp(elevated, onSurface, 0.08f),
        surfaceContainerLow = lerp(background, surface, 0.45f),
        surfaceContainerLowest = background,
    )
}

@Composable
fun PluviaTheme(
    seedColor: Color = PluviaSeed,
    isDark: Boolean = true,
    isAmoled: Boolean = false,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    customThemeJson: String? = null,
    content: @Composable () -> Unit,
) {
    val highContrast = style.name == "Monochrome" && customThemeJson == null
    val accent = themeAccent(seedColor, style, isDark)
    val customDocument = remember(customThemeJson) {
        when (val result = customThemeJson?.let(GameplayThemeCodec::decode)) {
            is GameplayThemeDecodeResult.Success -> result.document
            else -> null
        }
    }
    val customPalette = customDocument?.let { if (isDark) it.dark else it.light }
    val generatedScheme = if (isDark) darkScheme(accent, isAmoled) else lightScheme(accent)
    val baseScheme = if (highContrast) generatedScheme.withHighContrast(isDark, isAmoled) else generatedScheme
    val colorScheme = customPalette?.let { baseScheme.withCustomPalette(it, isAmoled) } ?: baseScheme
    val pluviaColors = pluviaColors(
        isDark = isDark,
        isAmoled = isAmoled,
        accent = accent,
        customPalette = customPalette,
        highContrast = highContrast,
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = !isDark
        insetsController.isAppearanceLightNavigationBars = !isDark
    }

    CompositionLocalProvider(LocalPluviaColors provides pluviaColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PluviaTypography,
            content = content,
        )
    }
}

/**
 * Accessor for Pluvia custom colors.
 * Usage: PluviaTheme.colors.accentCyan
 */
object PluviaTheme {
    val colors: PluviaColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPluviaColors.current
}

/**
 * Direct access to dark colors for non-Composable contexts.
 * Prefer PluviaTheme.colors when inside a Composable.
 */
object DarkColors {
    val statusInstalled = StatusInstalled
    val statusDownloading = StatusDownloading
    val statusAvailable = StatusAvailable
    val statusAway = StatusAway
    val statusOffline = StatusOffline

    val friendOnline = FriendOnline
    val friendOffline = FriendOffline
    val friendInGame = FriendInGame
    val friendAwayOrSnooze = FriendAwayOrSnooze
    val friendInGameAwayOrSnooze = FriendInGameAwayOrSnooze
    val friendBlocked = FriendBlocked

    val accentCyan = PluviaCyan
    val accentPurple = PluviaPurple
    val accentPink = PluviaPink
    val accentSuccess = PluviaSuccess
    val accentWarning = PluviaWarning
    val accentDanger = PluviaDanger

    val surfacePanel = PluviaSurface
    val surfaceElevated = PluviaSurfaceElevated

    val borderDefault = PluviaBorder
    val textMuted = PluviaForegroundMuted

    val compatibilityGood = CompatibilityGood
    val compatibilityGoodBackground = CompatibilityGoodBg
    val compatibilityPartial = CompatibilityPartial
    val compatibilityPartialBackground = CompatibilityPartialBg
    val compatibilityUnknown = CompatibilityUnknown
    val compatibilityUnknownBackground = CompatibilityUnknownBg
    val compatibilityBad = CompatibilityBad
    val compatibilityBadBackground = CompatibilityBadBg
}

// Settings tile color helpers
@Composable
fun settingsTileColors(): SettingsTileColors = SettingsTileDefaults.colors(
    titleColor = MaterialTheme.colorScheme.onSurface,
    subtitleColor = PluviaTheme.colors.textMuted,
    actionColor = MaterialTheme.colorScheme.primary,
)

@Composable
fun settingsTileColorsAlt(): SettingsTileColors = SettingsTileDefaults.colors(
    titleColor = MaterialTheme.colorScheme.onSurface,
    subtitleColor = PluviaTheme.colors.textMuted,
)

@Composable
fun settingsTileColorsDebug(): SettingsTileColors = SettingsTileDefaults.colors(
    titleColor = MaterialTheme.colorScheme.error,
    subtitleColor = PluviaTheme.colors.textMuted,
    actionColor = MaterialTheme.colorScheme.primary,
)

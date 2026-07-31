package app.gamenative.ui.theme

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@Serializable
data class GameplayThemeDocument(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val name: String,
    val dark: GameplayThemePalette,
    val light: GameplayThemePalette,
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

@Serializable
data class GameplayThemePalette(
    val primary: String,
    val onPrimary: String,
    val background: String,
    val onBackground: String,
    val surface: String,
    val surfaceElevated: String,
    val onSurface: String,
    val textMuted: String,
    val border: String,
    val success: String,
    val warning: String,
    val danger: String,
)

sealed interface GameplayThemeDecodeResult {
    data class Success(val document: GameplayThemeDocument) : GameplayThemeDecodeResult
    data class Error(val reason: String) : GameplayThemeDecodeResult
}

object GameplayThemeCodec {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = false
        explicitNulls = false
    }

    fun decode(source: String): GameplayThemeDecodeResult {
        val document = runCatching { json.decodeFromString<GameplayThemeDocument>(source) }
            .getOrElse { return GameplayThemeDecodeResult.Error("Invalid JSON or missing theme fields") }
        return validate(document)?.let(GameplayThemeDecodeResult::Error)
            ?: GameplayThemeDecodeResult.Success(document)
    }

    fun encode(document: GameplayThemeDocument): String = json.encodeToString(document)

    fun safeDocument(name: String = "Gameplay Slate"): GameplayThemeDocument = GameplayThemeDocument(
        name = name,
        dark = GameplayThemePalette(
            primary = "#6F91AA",
            onPrimary = "#FFFFFF",
            background = "#101419",
            onBackground = "#F2F5F7",
            surface = "#171D24",
            surfaceElevated = "#202831",
            onSurface = "#F2F5F7",
            textMuted = "#B4BEC8",
            border = "#3A4651",
            success = "#5B9B78",
            warning = "#C39A5C",
            danger = "#C56D6C",
        ),
        light = GameplayThemePalette(
            primary = "#315E78",
            onPrimary = "#FFFFFF",
            background = "#F7F9FA",
            onBackground = "#182026",
            surface = "#F0F3F5",
            surfaceElevated = "#E4E9ED",
            onSurface = "#182026",
            textMuted = "#4E5B66",
            border = "#69757D",
            success = "#397253",
            warning = "#805E22",
            danger = "#913F42",
        ),
    )

    fun color(value: String): Color {
        val rgb = value.removePrefix("#").toLong(16)
        return Color(0xFF000000 or rgb)
    }

    private fun validate(document: GameplayThemeDocument): String? {
        if (document.schemaVersion != GameplayThemeDocument.CURRENT_SCHEMA_VERSION) {
            return "Unsupported theme schema version ${document.schemaVersion}"
        }
        if (document.name.trim().length !in 1..48) return "Theme name must contain 1 to 48 characters"

        return validatePalette("dark", document.dark) ?: validatePalette("light", document.light)
    }

    private fun validatePalette(label: String, palette: GameplayThemePalette): String? {
        val fields = mapOf(
            "primary" to palette.primary,
            "onPrimary" to palette.onPrimary,
            "background" to palette.background,
            "onBackground" to palette.onBackground,
            "surface" to palette.surface,
            "surfaceElevated" to palette.surfaceElevated,
            "onSurface" to palette.onSurface,
            "textMuted" to palette.textMuted,
            "border" to palette.border,
            "success" to palette.success,
            "warning" to palette.warning,
            "danger" to palette.danger,
        )
        fields.entries.firstOrNull { !HEX_COLOR.matches(it.value) }?.let {
            return "$label.${it.key} must use #RRGGBB"
        }

        if (contrast(palette.onBackground, palette.background) < 4.5) {
            return "$label.onBackground does not have enough contrast against background"
        }
        if (contrast(palette.onSurface, palette.surface) < 4.5) {
            return "$label.onSurface does not have enough contrast against surface"
        }
        if (contrast(palette.onPrimary, palette.primary) < 3.0) {
            return "$label.onPrimary does not have enough contrast against primary"
        }
        if (contrast(palette.textMuted, palette.background) < 3.0) {
            return "$label.textMuted does not have enough contrast against background"
        }
        return null
    }

    private fun contrast(first: String, second: String): Double {
        val firstLuminance = luminance(first)
        val secondLuminance = luminance(second)
        return (max(firstLuminance, secondLuminance) + 0.05) /
            (min(firstLuminance, secondLuminance) + 0.05)
    }

    private fun luminance(value: String): Double {
        val rgb = value.removePrefix("#").toLong(16)
        fun channel(shift: Int): Double {
            val component = ((rgb shr shift) and 0xFF).toDouble() / 255.0
            return if (component <= 0.04045) component / 12.92 else ((component + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    private val HEX_COLOR = Regex("^#[0-9A-Fa-f]{6}$")
}

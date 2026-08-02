package app.gamenative.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameplayThemeDocumentTest {
    @Test
    fun `duplicated theme keeps palettes and gets independent name`() {
        val original = GameplayThemeCodec.safeDocument("Slate")
        val duplicate = original.copy(name = "Slate copy")

        assertEquals("Slate", original.name)
        assertEquals("Slate copy", duplicate.name)
        assertEquals(original.dark, duplicate.dark)
        assertEquals(original.light, duplicate.light)

        val decoded = GameplayThemeCodec.decode(GameplayThemeCodec.encode(duplicate))
        assertTrue(decoded is GameplayThemeDecodeResult.Success)
        assertEquals("Slate copy", (decoded as GameplayThemeDecodeResult.Success).document.name)
    }
}

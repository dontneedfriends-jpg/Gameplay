package com.winlator.xenvironment.components

import com.winlator.core.envvars.EnvVars
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnvRedactorTest {

    @Test
    fun sensitiveKeysAreMasked() {
        val env = EnvVars()
        env.put("HOME", "/home/user")
        env.put("SteamUser", "someone")
        env.put("STEAMID", "76561198000000000")
        env.put("MY_TOKEN", "abc123")

        val out = EnvRedactor.redact(env)

        assertTrue(out.contains("HOME=/home/user"))
        assertTrue(out.contains("SteamUser=<redacted>"))
        assertTrue(out.contains("STEAMID=<redacted>"))
        assertTrue(out.contains("MY_TOKEN=<redacted>"))
        assertFalse(out.contains("someone"))
        assertFalse(out.contains("76561198000000000"))
        assertFalse(out.contains("abc123"))
    }

    @Test
    fun shellQuoteHandlesSpecialCases() {
        assertEquals("plain", EnvRedactor.shellQuote("plain"))
        assertEquals("/path/with-dash/file_1.exe", EnvRedactor.shellQuote("/path/with-dash/file_1.exe"))
        assertEquals("'C:\\Games\\My Game\\game.exe'", EnvRedactor.shellQuote("C:\\Games\\My Game\\game.exe"))
        assertEquals("'Игра Судьбы.exe'", EnvRedactor.shellQuote("Игра Судьбы.exe"))
        assertEquals("'a&b;\$HOME`x`'", EnvRedactor.shellQuote("a&b;\$HOME`x`"))
        assertEquals("'it'\\''s.exe'", EnvRedactor.shellQuote("it's.exe"))
    }
}

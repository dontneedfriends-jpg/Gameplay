package app.gamenative.localgames

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InstalledExecutableScannerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `finds installed games but excludes system and maintenance executables`() {
        val driveC = temporaryFolder.newFolder("drive_c")
        executable(driveC, "Program Files/Mafia/Mafia.exe")
        executable(driveC, "Program Files/Mafia/unins000.exe")
        executable(driveC, "Program Files/Mafia/GameUpdater.exe")
        executable(driveC, "windows/system32/notepad.exe")
        executable(driveC, "users/xuser/Temp/setup.exe")

        assertEquals(
            listOf("Program Files/Mafia/Mafia.exe"),
            InstalledExecutableScanner.findCandidates(driveC),
        )
    }

    @Test
    fun `prefers shipping binaries and returns each path once`() {
        val driveC = temporaryFolder.newFolder("drive_c")
        executable(driveC, "Program Files/Example/Launcher.exe")
        executable(driveC, "Program Files/Example/Binaries/Win64/Example-Win64-Shipping.exe")

        val candidates = InstalledExecutableScanner.findCandidates(driveC)

        assertEquals("Program Files/Example/Binaries/Win64/Example-Win64-Shipping.exe", candidates.first())
        assertEquals(candidates.distinct(), candidates)
        assertFalse(candidates.any { it.contains("windows", ignoreCase = true) })
    }

    private fun executable(root: File, relativePath: String) {
        File(root, relativePath).apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(0x4d, 0x5a))
        }
    }
}

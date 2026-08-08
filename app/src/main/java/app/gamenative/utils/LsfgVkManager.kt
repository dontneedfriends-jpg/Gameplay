package app.gamenative.utils

import android.content.Context
import app.gamenative.service.SteamService
import com.winlator.container.Container
import com.winlator.core.FileUtils
import com.winlator.core.envvars.EnvVars
import java.io.File
import java.util.Locale
import timber.log.Timber
import kotlin.jvm.JvmStatic

/**
 * Manages the lsfg-vk Vulkan implicit layer for frame generation (2.x config).
 *
 * The layer works by intercepting vkQueuePresentKHR inside the container's
 * Vulkan driver and running Lossless Scaling frame generation (LSFG 3.1)
 * transparently. No overlay, no MediaProjection — it hooks the real swapchain
 * presentation path.
 *
 * Flow:
 * 1. At launch time: install the layer .so + manifest into the container's
 *    filesystem where the Vulkan loader discovers implicit layers.
 * 2. Copy Lossless.dll from the Steam install dir (app 993090) into the
 *    container's ~/.local/share/lsfg-vk/ directory.
 * 3. Write conf.toml (version 2) with a [[profile]] entry, the DLL path,
 *    multiplier, flow scale, and performance mode. The layer reads it on init.
 * 4. Set env vars so the layer finds its config and the active profile
 *    (LSFGVK_PROFILE overrides process-based detection, which is unreliable
 *    under Wine where /proc/self/exe points to the Wine loader).
 */
object LsfgVkManager {
    private const val TAG = "LsfgVkManager"

    // Steam app ID for Lossless Scaling (used to auto-find the DLL)
    const val LOSSLESS_SCALING_APP_ID = 993090
    private const val LOSSLESS_DLL_NAME = "Lossless.dll"

    // Paths inside the container's HOME (relative to rootDir)
    private const val CONFIG_RELATIVE_PATH = ".config/lsfg-vk/conf.toml"
    private const val LIB_RELATIVE_DIR = ".local/lib"
    private const val LAYER_RELATIVE_DIR = ".local/share/vulkan/implicit_layer.d"
    private const val DLL_RELATIVE_DIR = ".local/share/lsfg-vk"
    private const val LIB_FILENAME = "liblsfg-vk-layer.so"
    private const val MANIFEST_FILENAME = "VkLayer_LSFGVK_frame_generation.json"
    private const val VERSION_FILENAME = ".lsfg_vk_runtime_version"

    // Relative path from implicit_layer.d back to lib/
    private const val MANIFEST_LIBRARY_PATH = "../../../lib/$LIB_FILENAME"

    // Profile name written to conf.toml [[profile]] name and selected via the
    // LSFGVK_PROFILE env var (overrides process-based detection).
    private const val PROFILE_NAME = "gamenative-lsfg"

    // Container extra keys
    const val EXTRA_ARMED = "lsfgEnabled"
    const val EXTRA_MULTIPLIER = "lsfgMultiplier"
    const val EXTRA_FLOW_SCALE = "lsfgFlowScale"
    const val EXTRA_PERFORMANCE_MODE = "lsfgPerformanceMode"

    // Environment variables consumed by the lsfg-vk 2.x layer
    private const val ENV_DISABLE = "DISABLE_LSFGVK"
    private const val ENV_CONFIG = "LSFGVK_CONFIG"
    private const val ENV_PROFILE = "LSFGVK_PROFILE"

    // Current runtime version (bumped when the bundled .so changes)
    private const val RUNTIME_VERSION = "v2.0.0-dev39-android-arm64-v8a"

    // Asset paths
    private const val ASSET_DIR = "lsfg_vk/android_arm64_v8a"
    private const val ASSET_LIB = "$ASSET_DIR/$LIB_FILENAME"
    private const val ASSET_MANIFEST = "$ASSET_DIR/$MANIFEST_FILENAME"

    // ---- Public API --------------------------------------------------------

    /** Whether LSFG is supported for this container's variant. */
    @JvmStatic
    fun isSupported(container: Container): Boolean =
        container.containerVariant.equals(Container.BIONIC, ignoreCase = true)

    /** Whether LSFG is armed (enabled + Lossless.dll available in Steam dir) for this container. The DLL is copied into the container at launch time by ensureRuntimeInstalled(). */
    @JvmStatic
    fun isArmed(container: Container): Boolean =
        isSupported(container) &&
            parseBool(container.getExtra(EXTRA_ARMED, "false")) &&
            isDllAvailable()

    /** Whether Lossless Scaling is installed (Lossless.dll exists in Steam dir). */
    @JvmStatic
    fun isDllAvailable(): Boolean = findSteamDll() != null

    /** Whether the user owns Lossless Scaling in their Steam library. */
    @JvmStatic
    fun ownsLosslessScaling(): Boolean =
        SteamService.getAppInfoOf(LOSSLESS_SCALING_APP_ID) != null

    /** Get the DLL path inside the container, or null if the copy doesn't exist. */
    @JvmStatic
    fun containerDllPath(container: Container): String? {
        val dllFile = File(container.rootDir, "$DLL_RELATIVE_DIR/$LOSSLESS_DLL_NAME")
        return dllFile.absolutePath.takeIf { dllFile.isFile }
    }

    /** Get the multiplier (0=Off, 2-4, default 2). */
    fun multiplier(container: Container): Int {
        val raw = container.getExtra(EXTRA_MULTIPLIER, "2").toIntOrNull() ?: 2
        return if (raw == 0) 0 else raw.coerceIn(2, 4)
    }

    /** Get the flow scale (0.25-1.0, default 0.80). */
    fun flowScale(container: Container): Float =
        container.getExtra(EXTRA_FLOW_SCALE, "0.80").toFloatOrNull()?.coerceIn(0.25f, 1.0f) ?: 0.80f

    /** Get whether performance mode is enabled (default true). */
    fun performanceMode(container: Container): Boolean =
        parseBool(container.getExtra(EXTRA_PERFORMANCE_MODE, "true"))

    /**
     * Install the layer runtime + DLL into the container's filesystem.
     * Called during container startup in BionicProgramLauncherComponent.
     *
     * Installs:
     * - liblsfg-vk-layer.so → ~/.local/lib/
     * - VkLayer_LSFGVK_frame_generation.json → ~/.local/share/vulkan/implicit_layer.d/
     * - Lossless.dll → ~/.local/share/lsfg-vk/  (copied from Steam install dir)
     *
     * Uses versioned caching to skip redundant copies.
     *
     * @return true if installation succeeded or was already up-to-date
     */
    @JvmStatic
    fun ensureRuntimeInstalled(context: Context, container: Container): Boolean {
        if (!isSupported(container)) return false

        val rootDir = container.rootDir
        val localLibDir = File(rootDir, LIB_RELATIVE_DIR)
        val layerDir = File(rootDir, LAYER_RELATIVE_DIR)
        val dllDir = File(rootDir, DLL_RELATIVE_DIR)
        val libFile = File(localLibDir, LIB_FILENAME)
        val manifestFile = File(layerDir, MANIFEST_FILENAME)
        val versionFile = File(layerDir, VERSION_FILENAME)

        val installedVersion = versionFile.takeIf { it.exists() }?.readText()?.trim().orEmpty()
        val needsInstall = installedVersion != RUNTIME_VERSION ||
            !libFile.isFile || !manifestFile.isFile

        var success = true

        if (needsInstall) {
            try {
                localLibDir.mkdirs()
                layerDir.mkdirs()

                // Copy the layer .so from assets
                FileUtils.copy(context, ASSET_LIB, libFile)
                // Write the manifest with patched library_path
                val manifestText = context.assets.open(ASSET_MANIFEST)
                    .bufferedReader().use { it.readText() }
                    .replace(
                        "\"library_path\": \"$LIB_FILENAME\"",
                        "\"library_path\": \"$MANIFEST_LIBRARY_PATH\""
                    )
                FileUtils.writeString(manifestFile, manifestText)
                FileUtils.writeString(versionFile, RUNTIME_VERSION)

                // Set executable permissions
                if (libFile.exists()) FileUtils.chmod(libFile, 0b111101101)
                if (manifestFile.exists()) FileUtils.chmod(manifestFile, 0b110100100)
                if (versionFile.exists()) FileUtils.chmod(versionFile, 0b110100100)

                val ok = libFile.isFile && manifestFile.isFile
                if (ok) {
                    Timber.tag(TAG).i("Installed LSFG runtime %s into %s", RUNTIME_VERSION, rootDir)
                } else {
                    Timber.tag(TAG).e("Runtime installation verification failed")
                    success = false
                }
            } catch (t: Throwable) {
                Timber.tag(TAG).e(t, "Failed to install LSFG runtime")
                success = false
            }
        } else {
            Timber.tag(TAG).d("Runtime %s already installed in %s", RUNTIME_VERSION, rootDir)
        }

        // Copy Lossless.dll from Steam install dir into the container
        val dllFile = File(dllDir, LOSSLESS_DLL_NAME)
        val steamDll = findSteamDll()
        if (steamDll != null) {
            try {
                if (!dllFile.isFile || dllFile.length() != steamDll.length()) {
                    dllDir.mkdirs()
                    steamDll.inputStream().use { input ->
                        dllFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (dllFile.exists()) FileUtils.chmod(dllFile, 0b110100100)
                    Timber.tag(TAG).i("Copied Lossless.dll (%d bytes) into %s", dllFile.length(), dllDir)
                }
            } catch (t: Throwable) {
                Timber.tag(TAG).e(t, "Failed to copy Lossless.dll into container")
                success = false
            }
        } else if (parseBool(container.getExtra(EXTRA_ARMED, "false"))) {
            Timber.tag(TAG).w("LSFG enabled but Lossless.dll not found in Steam dir")
            success = false
        }

        return success
    }

    /**
     * Write the lsfg-vk conf.toml for this container.
     *
     * The 2.x layer requires version = 2 and a [[profile]] entry. A profile is
     * only written when frame generation is active: the parser rejects
     * multiplier <= 1, so "off" is expressed by omitting the profile entirely.
     * Selection is done via LSFGVK_PROFILE (set in applyLaunchEnv), which
     * overrides process-based detection.
     *
     * @return true if the config was written successfully
     */
    @JvmStatic
    fun writeConfig(container: Container): Boolean {
        if (!isSupported(container)) return false

        return try {
            val dllPath = containerDllPath(container)
            val savedMultiplier = multiplier(container)
            val frameGenActive = parseBool(container.getExtra(EXTRA_ARMED, "false")) &&
                dllPath != null && savedMultiplier >= 2
            val configFile = File(container.rootDir, CONFIG_RELATIVE_PATH)
            val configText = buildConfigToml(
                dllPath = dllPath,
                enabled = frameGenActive,
                multiplier = savedMultiplier.coerceIn(2, 4),
                flowScale = flowScale(container),
                performanceMode = performanceMode(container) && frameGenActive,
            )
            val ok = FileUtils.writeString(configFile, configText)
            if (ok && configFile.exists()) {
                FileUtils.chmod(configFile, 0b110100100)
            }
            ok
        } catch (t: Throwable) {
            Timber.tag(TAG).e(t, "Failed to write LSFG conf.toml")
            false
        }
    }

    /**
     * Apply LSFG-related environment variables to the launch environment.
     * Called during container startup in BionicProgramLauncherComponent.
     *
     * @return true if LSFG is armed and env vars were applied
     */
    @JvmStatic
    fun applyLaunchEnv(container: Container, envVars: EnvVars): Boolean {
        // Clear any stale env vars first
        envVars.remove(ENV_DISABLE)
        envVars.remove(ENV_CONFIG)
        envVars.remove(ENV_PROFILE)

        if (!isSupported(container)) {
            // Remove the manifest so the Vulkan loader can't find the layer
            disableLayerInContainer(container)
            return false
        }

        val dllPath = containerDllPath(container)
        val armed = parseBool(container.getExtra(EXTRA_ARMED, "false")) && dllPath != null

        if (!armed) {
            // Remove the manifest so the Vulkan loader can't find the layer
            disableLayerInContainer(container)
            Timber.tag(TAG).i("LSFG disabled (enabled=%s, dll=%s)",
                container.getExtra(EXTRA_ARMED, "false"), dllPath ?: "null")
            return false
        }

        envVars.put(ENV_CONFIG, configFile(container).absolutePath)
        envVars.put(ENV_PROFILE, PROFILE_NAME)

        // Add the container's implicit_layer.d to VK_LAYER_PATH so the
        // Vulkan loader discovers the lsfg-vk layer installed there.
        // The static VK_LAYER_PATH only covers /usr/share/vulkan/implicit_layer.d,
        // but we install the layer into the container's ~/.local/share/vulkan/.
        val containerLayerDir = File(container.rootDir, LAYER_RELATIVE_DIR)
        val existingLayerPath = envVars["VK_LAYER_PATH"] ?: ""
        if (existingLayerPath.isNotEmpty()) {
            envVars.put("VK_LAYER_PATH", "$existingLayerPath:${containerLayerDir.absolutePath}")
        } else {
            envVars.put("VK_LAYER_PATH", containerLayerDir.absolutePath)
        }

        Timber.tag(TAG).i(
            "LSFG armed: dll=%s, multiplier=%d, flowScale=%.2f, perf=%s",
            dllPath, multiplier(container), flowScale(container),
            if (performanceMode(container)) "on" else "off"
        )
        return true
    }

    /**
     * Remove the layer manifest so the Vulkan loader can't discover it.
     * Called when LSFG is disabled to ensure no stale layer is loaded.
     */
    private fun disableLayerInContainer(container: Container) {
        val layerDir = File(container.rootDir, LAYER_RELATIVE_DIR)
        val manifest = File(layerDir, MANIFEST_FILENAME)
        if (manifest.exists()) {
            manifest.delete()
            Timber.tag(TAG).d("Removed LSFG manifest to disable layer")
        }
    }

    // ---- DLL discovery -----------------------------------------------------

    /**
     * Find Lossless.dll in the Steam install directory for app 993090.
     * Returns the File if it exists, null otherwise.
     */
    private fun findSteamDll(): File? {
        val appDir = SteamService.getAppDirPath(LOSSLESS_SCALING_APP_ID)
        val dll = File(appDir, LOSSLESS_DLL_NAME)
        return dll.takeIf { it.isFile }
    }

    // ---- Helpers -----------------------------------------------------------

    private fun configFile(container: Container): File =
        File(container.rootDir, CONFIG_RELATIVE_PATH)

    private fun buildConfigToml(
        dllPath: String?,
        enabled: Boolean,
        multiplier: Int,
        flowScale: Float,
        performanceMode: Boolean,
    ): String = buildString {
        appendLine("version = 2")
        appendLine()
        appendLine("[global]")
        if (!dllPath.isNullOrBlank()) {
            appendLine("dll = ${tomlString(dllPath)}")
        }
        appendLine("allow_fp16 = true")
        appendLine()

        // Always write a [[profile]] so the layer stays loaded and hot-reload
        // can toggle between on (multiplier >= 2) and off (multiplier == 1,
        // passthrough) at runtime. Without a profile the layer fails to load
        // and runtime re-enabling can never take effect.
        appendLine("[[profile]]")
        appendLine("name = ${tomlString(PROFILE_NAME)}")
        appendLine("active_in = ${tomlString(PROFILE_NAME)}")
        appendLine("multiplier = ${if (enabled) multiplier.coerceIn(2, 4) else 1}")
        appendLine("flow_scale = ${formatFlowScale(flowScale)}")
        appendLine("performance_mode = ${if (performanceMode && enabled) "true" else "false"}")
        appendLine("pacing = ${tomlString("none")}")
    }

    private fun tomlString(value: String): String = buildString {
        append('"')
        value.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                else -> append(ch)
            }
        }
        append('"')
    }

    /** Parse boolean from container extra (handles "true"/"false" and "1"/"0"). */
    private fun parseBool(value: String): Boolean =
        value.equals("true", ignoreCase = true) || value == "1"

    private fun formatFlowScale(value: Float): String =
        String.format(Locale.US, "%.2f", value.coerceIn(0.25f, 1.0f))

    // ---- Runtime hot-reload -----------------------------------------------

    /**
     * Update the lsfg-vk conf.toml while the container is running.
     * The layer detects the file timestamp change on the next present call and
     * reloads the config, rebuilding the framegen context on the existing
     * swapchain.
     *
     * The config is always rewritten with a [[profile]] entry: multiplier >= 2
     * arms frame generation, multiplier == 1 puts the layer into passthrough
     * (no generated frames) so disabling works on the fly.
     *
     * @param container The running container
     * @param enabled Whether frame generation is active
     * @param multiplier Frame generation multiplier (2-4, ignored when disabled)
     * @param flowScale Flow scale factor (0.25-1.0)
     * @param performanceMode Whether performance mode is enabled
     * @return true if the config was updated successfully
     */
    @JvmStatic
    fun updateConfigAtRuntime(
        container: Container,
        enabled: Boolean,
        multiplier: Int,
        flowScale: Float,
        performanceMode: Boolean,
    ): Boolean {
        if (!isSupported(container)) return false

        val dllPath = containerDllPath(container)
        if (dllPath == null) {
            Timber.tag(TAG).w("Lossless.dll missing, cannot hot-reload")
            return false
        }

        val configFile = File(container.rootDir, CONFIG_RELATIVE_PATH)
        if (!configFile.exists()) {
            Timber.tag(TAG).w("conf.toml not found, cannot hot-reload")
            return false
        }

        return try {
            // multiplier == 1 keeps a valid profile while expressing "off".
            val profileMultiplier = if (enabled) multiplier.coerceIn(2, 4) else 1
            val configText = buildString {
                appendLine("version = 2")
                appendLine()
                appendLine("[global]")
                appendLine("dll = ${tomlString(dllPath)}")
                appendLine("allow_fp16 = true")
                appendLine()

                appendLine("[[profile]]")
                appendLine("name = ${tomlString(PROFILE_NAME)}")
                appendLine("active_in = ${tomlString(PROFILE_NAME)}")
                appendLine("multiplier = $profileMultiplier")
                appendLine("flow_scale = ${formatFlowScale(flowScale)}")
                appendLine("performance_mode = ${if (performanceMode) "true" else "false"}")
                appendLine("pacing = ${tomlString("none")}")
            }

            val ok = FileUtils.writeString(configFile, configText)
            if (ok && configFile.exists()) {
                FileUtils.chmod(configFile, 0b110100100)
            }
            if (ok) {
                Timber.tag(TAG).i(
                    "Hot-reloaded conf.toml: enabled=%s, multiplier=%d, flowScale=%.2f, perf=%s",
                    enabled, profileMultiplier, flowScale, performanceMode
                )
            }
            ok
        } catch (t: Throwable) {
            Timber.tag(TAG).e(t, "Failed to hot-reload conf.toml")
            false
        }
    }
}

# Gameplay development plan

Last updated: 2026-07-31

This document is the working implementation plan for Gameplay, the GPL-3.0 fork of GameNative. It records product requirements, completed work, current priorities, and acceptance criteria.

Status legend: `[x]` complete, `[-]` in progress, `[ ]` planned, `[!]` needs device verification.

## Completed in the current development cycle

- [x] Replaced the remaining GameNative-branded boot/loading presentation with the Gameplay console visual system.
- [x] Expanded the semantic theme engine with editable, importable, exportable, validated themes and additional built-in profiles.
- [x] Reorganized application settings and promoted nested settings to full-screen console pages.
- [x] Added a compact per-game runtime summary for the selected graphics driver, Wine/Proton, and DXVK/VKD3D configuration.
- [x] Added Steam achievements to the game screen using Gameplay's controller-oriented presentation.
- [x] Made Downloads the primary downloads screen and Storage its secondary section.
- [x] Added the WFM runtime-support payload and installer integration from the requested upstream work.
- [x] Ported upstream PR #1721 as an adapted performance set: debounced license processing, transactional license caching, batched Room queries and updates, database indexes, PICS timeout/retry handling, and reduced library recomputation.
- [x] Preserved Gameplay-specific Steam session caching, installed-library counts, achievements, and user-owned license priority while porting upstream performance changes.
- [x] Rebuilt and verified `modernDebug` after the integrated runtime, library, theme, settings, and UI changes.
- [x] Replaced the repository README with Gameplay-specific product, capability, compatibility, build, contribution, and GPL compliance documentation.
- [x] Removed unrelated repository artifacts: an unused screenshot, obsolete affiliate recommendation seed, upstream-only roadmap, and accidentally tracked debug signing files; replaced upstream contribution rules with Gameplay-specific guidance.
- [x] Extracted the library side-panel chrome (scrim, slide-from-right motion, surface, Back/B dismiss, focus-on-open) into a shared `ConsoleSidePanel` shell with a shared `ConsoleMenuActionItem` and back-hint row; migrated the system menu, import, quick-action, and library-options panels to it.
- [x] Converted the remaining specialized configuration surfaces to category-based console layouts: touch gestures and radial menu moved onto the shared full-screen page with the controller category rail (L1/R1 switching), and the in-game screen-effects tab gained a Scaling/Color/Shaders category strip for both Vulkan and GL renderers.
- [x] Removed duplicated settings group titles: the right content pane no longer repeats the selected category name (rail remains the single source), and the Downloads group no longer renders a third copy.
- [x] Added PlayStation glyph variants to the controller hint system: new DualShock/DualSense vector set (face buttons, L1/R1/L2/R2, Options, Share), `ControllerFamily` detection by vendor ID and device name with hot-plug recomposition, family-aware glyphs in the gamepad action bar and side-panel back hints.
- [x] Added a Presets tab to the container configuration with five built-in compatibility presets (performance, compatibility, DirectDraw/DX8-era games, DirectX 12, low-memory devices), each with a plain-language description and an explicit list of changed settings; presets edit the in-memory configuration and remain fully tunable in the other tabs.
- [x] Added reduced-motion support: a Reduce motion accessibility toggle (Appearance settings), a `LocalReduceMotion` composition local provided by `PluviaTheme` (also honoring the system animator-duration scale), a shared `motionSpec` helper applied to the main panel/tab/overlay transitions, and static fallbacks for the infinite skeleton, shimmer, drift, spin, and launch-progress animations.
- [x] Added actionable recovery for failed installer executable discovery: the failure dialog now offers "Browse all executables", which rescans drive C: without the uninstaller/updater name filter and re-enters candidate selection (`FAILED → CANDIDATE_SELECTION` state edge, relaxed scanner variant, coordinator/ViewModel/UI wiring).
- [x] Ported upstream PR #1464: Steam InstallScript VDF execution during container setup — depot-manifest script discovery via `EDepotFileFlag.InstallScript`, VDF parsing with env-var expansion and language overrides, pre-Wine registry writes to `system.reg`/`user.reg`, chained prerequisite run-processes with exit-code tracking, and a unified `PreLaunchSetup` command chain replacing the pre-install-only flow; includes the upstream review fixes (effective HasRunKey, hive-aware registry files, case-insensitive prefixes) plus ROOTDRIVE colon and process-quoting corrections, and the UbisoftConnect step now skips Steam games.
- [x] Ported the useful parts of upstream PR #392: an adapted `baseline-prof.txt` covering the Gameplay console shell, Compose lazy/pager/animation paths, Coil, and startup classes, with the `profileinstaller` dependency. The XML theme engine itself was deliberately not ported — it duplicates and conflicts with Gameplay's native console shell and semantic theme system.
- [x] Changed the application ID to `app.gameplay` so Gameplay installs side-by-side with GameNative; the `LAUNCH_GAME` intent action now derives from `BuildConfig.APPLICATION_ID` and the manifest placeholder. The Kotlin namespace remains `app.gamenative`, keeping activity-alias and provider wiring intact.

## 1. Product direction and constraints

- [x] Rename the user-facing application from GameNative to Gameplay.
- [x] Preserve GPL-3.0 licensing, copyright notices, source availability, and attribution to upstream GameNative contributors.
- [x] Define the product as a calm, controller-first Android shell for locally owned Windows games and installers.
- [x] Treat touch, Xbox-compatible controllers, and DualSense/PS5-compatible controllers as equal primary input methods.
- [x] Keep Steam connectivity while making local files and installed games first-class library sources.
- [x] Remove donation prompts tied to the original application from Gameplay settings.
- [ ] Finish a repository-wide audit of legacy GameNative naming in user-visible strings, notifications, shortcuts, and generated metadata; retain upstream attribution where legally required.

## 2. Local Windows games and installer workflow

- [x] Import local Windows executables from Android storage.
- [x] Import installers and create a dedicated container before running them.
- [x] Continue from installation into the existing container, compatibility, controls, and launch workflow.
- [x] Add manual/custom games to the unified library.
- [x] Expose installation as a real contextual action from the library quick-actions menu.
- [x] Cache reusable Wine/Proton/container runtime payloads so every installation does not redownload the same runtime.
- [x] Integrate the selected upstream fixes for local game/container flows and correct issues found during porting.
- [x] Fix the application-level crash observed when launching an installed game.
- [!] Verify on physical devices: installer completion, shortcut/executable discovery, relaunch after process death, and games installed outside the default path.
- [-] Add explicit install progress stages and actionable recovery for storage, runtime extraction, and executable discovery failures; installer exit detection, interrupted-session recovery, executable discovery, candidate selection, and container finalization are implemented.
- [ ] Add ISO/disc-image handling as a separate mounted-media workflow; do not treat an ISO as an executable.

## 3. Storefronts and session lifecycle

- [x] Preserve the standard Steam connection and Steam library support.
- [x] Hide storefront navigation entries when the user is not authenticated to that service.
- [x] Replace recommendations with the installed-games library as the default home content.
- [x] Reduce repeated Steam reconnection work during tab changes by retaining session/library state.
- [ ] Make storefront session state observable: connected, reconnecting, offline cache, and authentication required.
- [ ] Verify that navigation never triggers duplicate Steam initialization, network requests, or library refresh jobs.
- [ ] Define a consistent offline-library policy for Steam metadata and artwork.

## 4. Library and console shell

- [x] Move the main library toward a Steam Deck/Xbox-style landscape shell.
- [x] Remove the old left drawer as a primary navigation surface.
- [x] Move sorting, filtering, collections, and view selection into focused library options.
- [x] Separate the START/system-menu action from B/Circle contextual quick actions.
- [x] Add contextual quick actions: play/install, details, library options, search, and add game.
- [x] Track focused games so controller actions operate on the visible selection.
- [x] Redesign game details with a restrained hero layout and flatter information surfaces.
- [x] Redesign per-game options as a category rail with an adaptive settings grid and L1/R1 navigation.
- [x] Unify library system, import, quick-action, sorting/filtering, and game-option overlays under the same header, scrim, restrained motion, spacing, and focus conventions.
- [x] Extract a shared console side-panel shell after the remaining overlays use identical behavior.
- [x] Add a persistent, context-aware controller hint strip with Xbox and PlayStation glyph variants.
- [ ] Finish empty, loading, offline, error, and no-results library states.
- [ ] Verify complete D-pad/analog navigation, focus restoration, and Back/B/Circle behavior without touch.

## 5. Settings and configuration

- [x] Redesign the main application settings into a console category rail plus content area.
- [x] Make nested settings visually replace the settings screen as full-screen console pages.
- [x] Redesign container settings with compact categories and adaptive multi-column content.
- [x] Convert language, region, orientation, frontend sync, runtime managers, driver managers, and content managers to console-oriented full-screen pages.
- [x] Add delayed initial focus and safe focus requests to converted pages.
- [x] Convert Box64 and FEX preset editors to the shared full-screen console settings page.
- [x] Convert the remaining specialized configuration dialogs to the shared full-screen console page; touch gestures, shooter mode, controller binding, radial menu, Box64, and FEX are converted.
- [x] Replace long mobile-form layouts in touch/controller, radial-menu, shooter-mode, and screen-effect settings with category-based console screens.
- [ ] Reserve modal dialogs for confirmation, destructive actions, authentication, or short blocking decisions only.
- [ ] Add search across application, container, and per-game settings.
- [ ] Audit focus order and scrolling at narrow landscape heights.

## 6. Theme system and customization

- [x] Establish a restrained visual direction: low saturation, tinted dark surfaces, minimal decoration, no acidic gamer styling.
- [x] Support built-in theme selection.
- [x] Support importing themes from files.
- [x] Expose broad theme editing instead of limiting users to an accent-color picker.
- [x] Document a stable semantic theme schema with versioning and a defined migration boundary.
- [x] Validate imported themes for required tokens, readable contrast, file size, and recoverability.
- [ ] Provide theme duplication (rename, visual editing, live preview, export, and reset-to-safe-theme are complete).
- [x] Add high-contrast and reduced-motion built-in themes; high contrast ships as the Monochrome palette profile with a dedicated contrast pass, and reduced motion is delivered as an accessibility toggle with theme-level propagation to transitions and ambient animations.

## 7. Controller and input parity

- [x] Support touch controls as a first-class input method.
- [x] Accept Xbox-compatible and DualSense/PS5-compatible physical controller input.
- [x] Add console navigation semantics to the redesigned library and settings surfaces.
- [x] Centralize controller button mapping and glyph selection by detected controller family.
- [ ] Add controller hot-plug and disconnect recovery without losing focus.
- [ ] Ensure every primary workflow is completable using only a controller.
- [ ] Add automated focus-navigation tests for major screens and manual device test scripts.

## 8. Compatibility and runtime scope

- [x] Limit the product scope to Windows software running through Wine/Proton-compatible layers.
- [x] Support modern 32-bit and 64-bit Windows executables where the selected runtime and device permit it.
- [x] Define the 16-bit strategy explicitly: Win16 through compatible Wine paths where possible; DOS games through a future DOSBox integration rather than promising universal native execution. (Decision recorded here; DOSBox integration remains unscheduled.)
- [x] Add compatibility presets and plain-language explanations for legacy DirectDraw/Direct3D, DXVK, VKD3D, CPU translation, and memory settings; delivered as the container Presets tab with per-preset explanations and change summaries.
- [ ] Maintain tested game profiles, including early-2000s titles such as Mafia, with device/GPU/runtime caveats.

## 9. Imported upstream work

- [x] Review and port the requested upstream pull requests: #1651, #1695, #1709, #1721, #1730, #1759, #1760, #1776, #1782, and #1784.
- [x] Adapt imported changes to Gameplay instead of applying them blindly.
- [x] Address compilation, state-management, and integration issues discovered during the ports.
- [-] Record each upstream PR mapping and resulting Gameplay commit in a dedicated provenance table; requested PR numbers are now recorded here, while per-commit mappings remain to be added.
- [ ] Recheck open upstream changes before release candidates and cherry-pick only compatible fixes.

## 10. Stability, testing, and release engineering

- [x] Establish a reproducible `modernDebug` Android build in the current workspace.
- [x] Use Wi-Fi ADB and device crash logs to diagnose the launch crash.
- [x] Produce a verified debug APK after the controller-navigation and quick-action changes.
- [ ] Add regression tests around container creation, cached runtime reuse, install completion, and launch intent/state construction.
- [ ] Add Compose UI tests for library focus, system menu, quick actions, settings navigation, and game options.
- [ ] Add structured crash reporting that identifies the game, container, runtime, GPU driver, and launch stage without exposing secrets.
- [ ] Create release signing, versioning, changelog, and reproducible artifact instructions.
- [ ] Run license/compliance checks for GPL-3.0 source distribution and bundled third-party components before release.

## Current execution order

1. Complete focus restoration, controller hints, and narrow-landscape audits.
2. Harden installer/runtime-cache progress, error recovery, and device tests.
3. Add theme duplication and the remaining accessibility-oriented built-in themes.
4. Add automated regressions and prepare a GPL-compliant Gameplay release process.

## Definition of done for a converted console screen

- One obvious primary action and no duplicated navigation actions.
- Full operation through touch, Xbox-compatible controller, and DualSense-compatible controller.
- Predictable initial focus, focus restoration, Back/B/Circle behavior, and scroll-following focus.
- No nested mobile dialog for ordinary navigation; deep settings replace the current page.
- Consistent semantic colors, type scale, spacing, corner radii, scrim, and transition duration.
- Usable at narrow landscape heights without a long undifferentiated vertical form.
- Loading, empty, disabled, error, and destructive states are explicit.
- Existing functionality and stored configuration remain compatible.

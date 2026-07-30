# GameNative Upstream Architecture Map

## Status

Phase 0 working map based on GameNative `upstream/master` sparse import. Paths are verified against the imported upstream tree. Expand this document as each subsystem is traced in detail.

## Local game extension points

| Concern | Verified upstream path | Role in the local-Windows implementation |
|---|---|---|
| Local-game discovery | `app/src/main/java/app/gamenative/utils/CustomGameScanner.kt` | Primary adapter point for local game IDs, folder metadata, executable discovery, icon extraction, and cache invalidation. Do not fork a parallel scanner. |
| Custom-game cache | `app/src/main/java/app/gamenative/utils/CustomGameCache.kt` | Cache/invalidation path that must be extended when a local installer completes. |
| Custom-game folder selection | `app/src/main/java/app/gamenative/ui/components/CustomGameFolderPicker.kt` | Existing UI reference for Android storage access. Replace/extend with SAF-first executable and installer selection rather than assuming raw storage paths. |
| Custom-game library workflow | `app/src/main/java/app/gamenative/ui/model/LibraryViewModel.kt` | Existing add/scan/filter flow; local installer sessions must appear here without needing Steam authentication. |
| Custom-game details UI | `app/src/main/java/app/gamenative/ui/screen/library/appscreen/CustomGameAppScreen.kt` | Existing game-screen entry point for local-game launch/configuration actions. |
| Main launch dispatch | `app/src/main/java/app/gamenative/ui/PluviaMain.kt` | Resolves a `CUSTOM_GAME` launch executable and dispatches the container. Unified launch work must preserve current Steam branches. |
| Container orchestration | `app/src/main/java/app/gamenative/utils/ContainerUtils.kt` | Existing higher-level container creation/configuration logic. The installer workflow must call this path or a narrow extracted service. |
| Container storage and recovery | `app/src/main/java/app/gamenative/utils/ContainerStorageManager.kt` | Storage accounting and container management integration point for import, installer cleanup, and orphan handling. |
| Native container manager | `app/src/main/java/com/winlator/container/ContainerManager.java` | Underlying container lifecycle. Treat as an infrastructure dependency; avoid UI-driven direct calls. |
| Container state model | `app/src/main/java/com/winlator/container/ContainerData.kt` | Existing configuration model to inspect before adding import/profile metadata. |
| Wine Start Menu support | `app/src/main/java/com/winlator/core/WineStartMenuCreator.java` | Candidate source after installer completion. |
| Process diagnostics | `app/src/main/java/app/gamenative/utils/WineProcessSnapshotHelper.kt` | Basis for installer process monitoring and actionable diagnostics. |
| Launch dependencies | `app/src/main/java/app/gamenative/utils/LaunchDependencies.kt` and `app/src/main/java/app/gamenative/utils/launchdependencies/` | Runtime prerequisites must be preserved and made source-independent. |

## Steam preservation boundary

| Concern | Verified upstream path | Constraint |
|---|---|---|
| Steam service | `app/src/main/java/app/gamenative/service/SteamService.kt` | Preserve authentication, library, container and platform behavior. Local games must not modify Steam protocol internals. |
| Steam bootstrap | `app/src/main/java/app/gamenative/SteamBootstrap.kt` | Preserve bootstrap/login behavior. |
| Steam metadata | `app/src/main/java/app/gamenative/data/SteamApp.kt` and related DAOs | Store-specific metadata remains separate from local-game metadata. |
| Steam persistence | `app/src/main/java/app/gamenative/db/PluviaDatabase.kt`, `db/dao/`, `db/migration/RoomMigration.kt` | Any unified library migration must retain existing rows and container IDs. |
| Cloud saves | `app/src/main/java/app/gamenative/service/SteamAutoCloud.kt` and `ui/util/SteamSaveTransfer.kt` | Existing Steam save behavior is a mandatory regression target. |
| Steam app UI | `app/src/main/java/app/gamenative/ui/screen/library/appscreen/SteamAppScreen.kt` | Redesign can share components but must preserve Steam-only actions. |

## Input and appearance extension points

| Concern | Verified upstream path | Role |
|---|---|---|
| Launcher controller configuration | `app/src/main/java/app/gamenative/ui/component/dialog/ControllerBindingDialog.kt`, `PhysicalControllerConfigSection.kt` | Baseline for semantic action mapping and Xbox/DualSense presentation. |
| In-game physical controller handling | `app/src/main/java/app/gamenative/ui/screen/xserver/PhysicalControllerHandler.kt` | Preserve in-game forwarding while adding launcher-navigation parity. |
| Low-level controller support | `app/src/main/java/com/winlator/inputcontrols/ControllerManager.java`, `ExternalController.java`, `GamepadState.java` | Input normalization integration boundary. |
| Touch controls | `app/src/main/java/com/winlator/inputcontrols/InputControlsManager.java`, `app/src/main/java/com/winlator/widget/InputControlsView.java` | Existing touch editor/runtime support must remain available. |
| Compose theme | `app/src/main/java/app/gamenative/ui/theme/Color.kt`, `Theme.kt`, `Typography.kt` | Starting point for semantic tokens and importable theme packages. |
| Theme preference/DI | `app/src/main/java/app/gamenative/enums/AppTheme.kt`, `app/src/main/java/app/gamenative/di/AppThemeModule.kt` | Existing built-in-theme persistence adapter. |
| External-display input | `app/src/main/java/app/gamenative/externaldisplay/ExternalDisplayInputController.kt` | Must be included in controller parity testing. |

## Initial implementation order

1. Trace `CustomGameScanner`, `ContainerUtils`, `ContainerManager`, and `PluviaMain` end-to-end for the current Custom Game launch contract.
2. Extract or introduce a narrow source-independent container/launch facade without modifying `SteamService` behavior.
3. Add a persisted local import/installation-session model through Room migrations.
4. Implement executable inspection and SAF-managed source storage.
5. Route portable-game import through the new facade before implementing installer process monitoring.

## Rules derived from the source map

- Do not create a second container format for local installers.
- Do not store local games as synthetic Steam applications.
- Do not put installer lifecycle state in Compose-only state or preferences; it must survive process death.
- Do not redesign controller behavior only at the UI level: distinguish launcher actions from the existing in-game input pipeline.
- Keep upstream Steam APIs behind their current service boundary until a regression suite exists.

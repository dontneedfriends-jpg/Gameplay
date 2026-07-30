# GameNative Local Windows Launcher — Implementation Plan

## 0. Document status

- Status: planning baseline
- Intended audience: implementation, design, QA, release, and documentation agents
- Product context: [`PRODUCT.md`](../PRODUCT.md)
- Upstream: `utkarshdalal/GameNative`, branch `master`
- Target platform: Android
- Primary UI technology expected from upstream: Kotlin and Jetpack Compose
- License constraint: preserve GPL-3.0 compliance and all applicable third-party notices

This document is the source of truth for scope, sequencing, component boundaries, acceptance criteria, and agent hand-offs. It deliberately does not invent current upstream class names: the local repository does not yet contain GameNative source code. Phase 0 maps these conceptual components to real files and records the result before feature work starts.

---

## 1. Product objective

Extend GameNative into a unified Android launcher for legally owned Windows games that:

1. retains the standard GameNative Steam login, library, installation, launch, cloud-save, DLC, Workshop, branch, and offline capabilities;
2. imports and launches an already installed or portable Windows game from local storage;
3. launches a Windows installer in a newly created GameNative container, detects the installed executable, and turns the result into a normal library entry;
4. supports modern Win32/Win64 games through the existing Wine/Proton-based stack;
5. introduces an explicit runtime path for 16-bit Windows software rather than pretending standard Proton can handle all Win16 cases;
6. provides equal task completion through touch, Xbox-compatible controllers, and DualSense/PS5-compatible controllers;
7. replaces the visually cluttered, acidic interface with a calm, controller-fluent product UI inspired by the usability of Steam Deck and Xbox One;
8. supports importable, exportable, deeply customizable themes with a safe recovery path.

The application is a launcher and compatibility orchestrator, not a promise that every Windows game will run. Compatibility failures must be diagnosed and explained rather than hidden.

---

## 2. Scope boundaries

### 2.1 In scope

- Steam integration already present in GameNative.
- Local portable game import from an executable or game directory.
- Windows installer execution (`.exe`, `.msi`) in a fresh container.
- Detection and user confirmation of the installed game's primary executable.
- Resume/recovery for interrupted installations.
- One unified library for Steam and local games.
- Per-game containers and compatibility configuration.
- PE architecture/type inspection: Win16 NE, Win32 PE32, Win64 PE32+.
- Touch controls and physical gamepad input.
- Xbox-style and PlayStation-style glyphs.
- Fully controller-operable launcher UI.
- Built-in themes plus import/export of theme packages.
- Accessible safe mode and built-in fallback theme.
- External frontend/deep-link compatibility where upstream already supports it.
- ISO and BIN/CUE handling for legally owned Windows installation media as a post-MVP capability.

### 2.2 Explicitly out of scope

- Piracy features or bypassing DRM, copy protection, anti-cheat, licensing, or platform ownership checks.
- Kernel-level Windows anti-cheat emulation.
- General-purpose Android virtualization of arbitrary operating systems.
- DOS-only games and console ROM emulation.
- Installing or launching Android APKs.
- Executable code inside theme packages.
- Guaranteed compatibility with every Windows title.
- Silent modification of original files outside app-controlled storage.

### 2.3 Compatibility promise

The product promise is:

> Select a supported Windows game or installer, let the app create and configure an appropriate environment, and receive either a playable library entry or a clear, actionable explanation of the failure.

It is not:

> Every Windows executable will run without intervention.

---

## 3. Target devices and interaction modes

### 3.1 Device classes

- Android phone, portrait and landscape.
- Android tablet.
- Android handheld gaming device with integrated controls.
- Android device connected to a television or monitor.

### 3.2 Required input modes

- Capacitive touch.
- Xbox-compatible XInput-style controller exposed through Android input APIs.
- DualSense/PS5-compatible controller exposed through Android input APIs.
- D-pad and analog-stick navigation.
- Hardware keyboard and mouse where supported by upstream.

### 3.3 Input parity rule

Every primary workflow must be completable without switching input mode:

- sign in to Steam;
- browse/search/filter library;
- import a game;
- run an installer;
- confirm the detected executable;
- edit basic container settings;
- launch/stop a game;
- import, preview, apply, export, and recover a theme;
- access error details and recovery actions.

Text entry may invoke an appropriate on-screen keyboard, but the flow must remain navigable and dismissible from a controller.

---

## 4. Functional requirements

Requirement IDs are stable and must be referenced by implementation tasks, tests, and pull requests.

### 4.1 Unified library

- **LIB-001** The home library shall show Steam and local games in one collection.
- **LIB-002** Each entry shall retain its source: Steam, local portable game, local installer, or imported installation media.
- **LIB-003** Users shall filter by source, installed state, compatibility state, favorites, and recent use.
- **LIB-004** Local use shall not require Steam authentication.
- **LIB-005** Logging out of Steam shall not delete local games or their containers.
- **LIB-006** A game shall have a stable internal ID independent of Steam App ID or filesystem path.
- **LIB-007** Duplicate imports shall be detected and resolved explicitly, not silently duplicated.
- **LIB-008** Library entries shall survive app updates and schema migrations.

### 4.2 Portable game import

- **IMP-001** Users shall be able to select a Windows executable through Android's system file picker.
- **IMP-002** Users shall be able to select a directory when the Android provider supports directory selection.
- **IMP-003** The app shall persist valid SAF permissions where possible.
- **IMP-004** The app shall inspect the executable before creating a container.
- **IMP-005** The inspection result shall distinguish Win16 NE, Win32 PE32, Win64 PE32+, malformed, and unsupported files.
- **IMP-006** The app shall suggest a compatible runtime profile based on executable type and device capability.
- **IMP-007** The user shall choose between copying files into app storage and referencing an accessible source when both are safe.
- **IMP-008** Copy progress, required space, cancellation, and cleanup shall be visible.
- **IMP-009** The imported executable's working directory, arguments, and environment shall be editable.
- **IMP-010** A successful import shall create a library entry linked to one container and one primary executable.
- **IMP-011** Failed import shall not leave an invisible orphan container.
- **IMP-012** Retry shall reuse a valid partially created container when safe.

### 4.3 Installer workflow

- **INS-001** Users shall be able to select `.exe` and `.msi` installers.
- **INS-002** A new container shall be created before the installer launches.
- **INS-003** Container creation shall use the same underlying GameNative mechanisms used for store games.
- **INS-004** Installation shall have an explicit persisted state machine.
- **INS-005** The app shall record a pre-install filesystem snapshot or equivalent journal.
- **INS-006** The installer shall run in the newly created container.
- **INS-007** The user shall be able to reopen an interrupted installation session.
- **INS-008** The app shall detect new launch candidates using filesystem changes, Wine Start Menu entries, `.lnk` files, and executable metadata.
- **INS-009** The app shall rank candidates and explain why each candidate is suggested.
- **INS-010** The user shall confirm or manually browse to the primary game executable.
- **INS-011** Uninstallers, redistributable installers, crash reporters, launchers, and configuration utilities shall be down-ranked.
- **INS-012** Completing the wizard shall convert the installation session into a normal local library entry.
- **INS-013** The original installer location shall be retained only as metadata unless the user requests a managed copy.
- **INS-014** Installation cancellation shall offer: keep for later, delete incomplete installation, or return to installer.
- **INS-015** MSI execution shall use the container's supported `msiexec` path and expose actionable logs on failure.
- **INS-016** Installers requesting restart shall transition to a resumable `RestartRequired` state rather than losing the session.

### 4.4 Windows runtime selection

- **RUN-001** Runtime selection shall be independent of game source.
- **RUN-002** Win32 and Win64 shall use upstream GameNative Wine/Proton and translation facilities.
- **RUN-003** Win16 shall be represented as a distinct runtime capability.
- **RUN-004** A runtime provider shall declare supported executable types and device requirements.
- **RUN-005** Runtime availability shall be checked before destructive or long-running import steps.
- **RUN-006** Runtime components shall be versioned and attributable in diagnostics.
- **RUN-007** Per-game runtime overrides shall be supported.
- **RUN-008** Known-good upstream/community configurations shall remain applicable where identifiers and metadata permit.
- **RUN-009** Failure shall identify the layer: file access, container, translator, Wine/Proton, graphics, dependency, DRM/anti-cheat, or game process.
- **RUN-010** Users shall be able to export a redacted diagnostic bundle.

### 4.5 Windows installation media

- **MED-001** Post-MVP, the app shall recognize `.iso` and valid `.bin/.cue` pairs as Windows installation media.
- **MED-002** Media import shall inspect contents before choosing a runtime.
- **MED-003** The app shall support an installer requiring the disc to remain present through a virtual media mapping or an equivalent Wine-visible path.
- **MED-004** Multi-track BIN/CUE media shall not be treated as a flat archive.
- **MED-005** The app shall not claim to bypass SafeDisc, SecuROM, or other copy protection.
- **MED-006** Unsupported or protected media shall produce a clear explanation and suggest a legal DRM-free release where appropriate.

### 4.6 Steam preservation

- **STM-001** Existing Steam sign-in shall remain available.
- **STM-002** Existing Steam library discovery and refresh shall remain functional.
- **STM-003** Existing Steam game installation/update behavior shall remain functional.
- **STM-004** Steam Cloud, DLC, Workshop, branches, achievements where supported, and offline launch shall remain functional.
- **STM-005** Steam-specific identifiers and authentication data shall remain isolated from local-game domain models.
- **STM-006** Steam logout shall preserve installed Steam content according to upstream behavior and shall always preserve local content.
- **STM-007** The redesign shall not replace authenticated web flows with a custom credential collector.
- **STM-008** Upstream Steam regression smoke tests shall run before every release candidate.

### 4.7 Controller support

- **CTL-001** Launcher actions shall be defined semantically (`Confirm`, `Back`, `Menu`, `Search`, `Context`, `TabLeft`, `TabRight`) rather than by physical key code.
- **CTL-002** Android key/motion events shall map to a normalized controller model.
- **CTL-003** Xbox and DualSense glyph packs shall change dynamically based on the active device.
- **CTL-004** Users shall be able to override mappings per controller and per game.
- **CTL-005** Compose focus order shall be explicit on nontrivial screens.
- **CTL-006** Focus shall be visible at WCAG-compliant contrast and not rely on color alone.
- **CTL-007** Closing a dialog, sheet, or child screen shall restore focus to the invoking control.
- **CTL-008** No modal or list shall trap controller focus.
- **CTL-009** Hot-plug, disconnect, reconnect, and active-controller switching shall be handled without restarting the app.
- **CTL-010** Controller hints shall never use Xbox labels while a DualSense-compatible controller is active, or vice versa.
- **CTL-011** Touch use shall temporarily suppress focus ornamentation without destroying focus state.
- **CTL-012** The in-game control editor shall preserve upstream capability and support the normalized controller model.

### 4.8 Themes and customization

- **THM-001** The application shall include at least one accessible built-in light theme and one accessible built-in dark theme.
- **THM-002** Users shall import themes from a versioned theme package file.
- **THM-003** Users shall export their customized theme to the same package format.
- **THM-004** Themes may change semantic colors, typography, density, component shapes, focus visuals, icons, backgrounds, navigation sounds, motion parameters, cover treatments, and supported layout presets.
- **THM-005** Theme packages shall not execute code.
- **THM-006** Every imported archive path shall be normalized and protected against zip-slip/path traversal.
- **THM-007** Import shall enforce total uncompressed size, file count, image dimensions, and allowed MIME/type limits.
- **THM-008** Imported fonts and assets shall be scoped to the theme and shall not modify system files.
- **THM-009** Required semantic tokens shall have fallbacks.
- **THM-010** A theme preview shall be isolated and revertible before application.
- **THM-011** Invalid contrast and accessibility violations shall be reported before application.
- **THM-012** Users may explicitly apply a noncompliant custom theme after warning, except when it would make recovery controls unusable.
- **THM-013** A built-in safe theme shall always remain installed and immutable.
- **THM-014** A launch-time safe-mode controller chord and touch recovery path shall reset the active theme.
- **THM-015** Theme schema migration shall be versioned and tested.
- **THM-016** Missing assets shall degrade to semantic defaults without crashing the launcher.
- **THM-017** User-created themes shall be editable without writing JSON manually.

### 4.9 Accessibility

- **A11Y-001** Target WCAG 2.2 AA where applicable to native Android UI.
- **A11Y-002** Text shall respect Android font scaling without clipping essential actions.
- **A11Y-003** Touch targets shall meet Android accessibility guidance.
- **A11Y-004** TalkBack semantics, roles, names, values, and traversal order shall be defined.
- **A11Y-005** Reduced motion shall replace nonessential transitions with instant changes or short crossfades.
- **A11Y-006** Status and validation shall not rely solely on color.
- **A11Y-007** Controller focus and keyboard focus shall be visible.
- **A11Y-008** High-contrast built-in themes shall remain available regardless of imported themes.
- **A11Y-009** Progress and long-running tasks shall expose accessible status updates without excessive announcements.

---

## 5. Non-functional requirements

### 5.1 Reliability

- No imported game, container, or theme may disappear because a Steam session expires.
- Long operations must persist enough state to recover after process death.
- Database migrations must be forward-tested from every supported released schema.
- All temporary files must have explicit ownership and cleanup rules.
- Cancellation must be cooperative and leave a known state.

### 5.2 Performance

- Library navigation should maintain frame pacing suitable for 60 Hz displays on supported baseline devices.
- Do not decode full-resolution cover/background images on the UI thread.
- Large libraries must use paging/lazy composition and stable item keys.
- Theme changes must not rebuild game/container data.
- File copy and hashing must be streamed and cancellable.
- Startup must not block on Steam network refresh.

### 5.3 Security

- Never collect Steam credentials in custom app-owned text fields when upstream uses an authenticated provider/web flow.
- Treat installers, executables, archives, themes, fonts, images, and shortcuts as untrusted input.
- Do not execute imported theme content.
- Sanitize command-line presentation and logging; avoid exposing tokens or credentials.
- Validate archive paths and symlinks before extraction.
- Keep container processes within existing GameNative/Android sandbox boundaries.
- Verify downloadable runtime components using the upstream integrity mechanism or introduce signed/checksummed manifests.

### 5.4 Privacy

- Preserve upstream privacy controls.
- Local file names and paths must not be sent to analytics.
- Diagnostics must be previewable and redacted before sharing.
- Analytics events must use internal/source identifiers rather than personally revealing filesystem paths.

### 5.5 Maintainability

- Store-specific logic, local import logic, runtime logic, UI, and themes must not depend cyclically on one another.
- New services require interfaces and deterministic tests at filesystem/runtime boundaries.
- Upstream merge conflicts should be minimized by wrapping existing capabilities before rewriting them.
- Temporary compatibility adapters must include removal criteria.

---

## 6. Proposed architecture

```text
UI / Compose shell
├── Home and unified library
├── Add/import wizard
├── Installation session UI
├── Game details and compatibility settings
├── Steam account UI
├── Theme studio
└── Diagnostics and recovery
            │
Application layer
├── LibraryCoordinator
├── ImportGameUseCase
├── StartInstallationUseCase
├── FinalizeInstallationUseCase
├── LaunchGameUseCase
├── SteamFacade
├── ThemeManager
└── InputModeCoordinator
            │
Domain layer
├── Game / GameSource / LaunchTarget
├── ContainerProfile / RuntimeProvider
├── InstallationSession state machine
├── CompatibilityReport
├── ControllerProfile
└── ThemeManifest / semantic tokens
            │
Infrastructure
├── Existing GameNative Steam implementation
├── Existing container and launch implementation
├── Android SAF and managed file storage
├── PE/NE executable inspector
├── Installer change journal and candidate scanner
├── Wine/Proton runtime adapter
├── Win16 runtime adapter
├── Room repositories and migrations
├── Theme package validator/loader
└── Logs and redacted diagnostics
```

### Architectural rule

UI code asks application services to perform a task. It must not directly construct Wine commands, manipulate container paths, query Steam protocols, or extract theme archives.

---

## 7. Component catalogue

The names below describe responsibilities. Phase 0 may map them onto existing upstream classes or choose repository-conformant names.

### 7.1 `GameRepository`

Responsibilities:

- persist unified game records;
- query by source, state, favorites, recency, and installation status;
- preserve stable internal IDs;
- expose observable library data to UI;
- migrate existing Steam/custom-game records.

Must not:

- authenticate with Steam;
- launch processes;
- perform file imports.

### 7.2 `SteamFacade`

Responsibilities:

- adapt the existing upstream Steam feature set to the unified library;
- keep existing auth/session behavior intact;
- translate Steam game metadata into source-specific records;
- expose install, update, cloud, DLC, Workshop, branch, achievement, and offline operations already supported upstream.

Implementation strategy:

- prefer an adapter around upstream services;
- avoid invasive rewrites until regression coverage exists;
- document all upstream touchpoints in Phase 0.

### 7.3 `LocalImportCoordinator`

Responsibilities:

- orchestrate SAF selection results;
- inspect executable/media type;
- estimate space;
- choose copy/reference strategy;
- create or request a container;
- persist progress and retry state;
- produce a local game or installation session.

### 7.4 `ExecutableInspector`

Input:

- seekable stream or managed local file.

Output:

```kotlin
sealed interface ExecutableKind {
    data object Win16NE : ExecutableKind
    data object Win32PE : ExecutableKind
    data object Win64PE : ExecutableKind
    data object DosOnly : ExecutableKind
    data object MsiPackage : ExecutableKind
    data class Unsupported(val reason: String) : ExecutableKind
}
```

Responsibilities:

- parse signatures defensively;
- report type and confidence;
- never execute a file during inspection;
- provide metadata for runtime selection and diagnostics.

### 7.5 `ManagedFileStore`

Responsibilities:

- wrap Android SAF and app-managed storage;
- maintain persisted URI permissions;
- stream copy with progress and cancellation;
- calculate required/free space;
- enforce ownership and cleanup policies;
- handle providers that do not expose normal filesystem paths.

### 7.6 `ContainerService`

Responsibilities:

- adapt existing GameNative container creation/edit/delete mechanisms;
- create containers from versioned profiles;
- expose lifecycle and readiness state;
- attach storage/media mappings;
- provide transactional cleanup of failed creation;
- preserve existing installed Steam containers.

### 7.7 `RuntimeRegistry`

Responsibilities:

- register available runtime providers;
- choose candidates based on executable type, device, GPU, and user preference;
- report missing components;
- version runtime selections.

Provider contract:

```kotlin
interface RuntimeProvider {
    val id: RuntimeId
    fun supports(kind: ExecutableKind, device: DeviceCapabilities): SupportResult
    suspend fun prepare(container: ContainerRef): PrepareResult
    suspend fun launch(request: LaunchRequest): LaunchResult
}
```

Initial providers:

- upstream Wine/Proton/FEX-compatible provider for Win32/Win64;
- placeholder capability entry followed by an implemented Win16 provider;
- no DOS provider in current product scope.

### 7.8 `InstallationSessionRepository`

Persists the installer state machine and recovery information across process death.

Required states:

```text
Draft
→ Inspecting
→ AwaitingOptions
→ CreatingContainer
→ ReadyToInstall
→ Installing
→ RestartRequired
→ DetectingCandidates
→ AwaitingExecutableChoice
→ Finalizing
→ Completed

Any active state → Paused | Failed | Cancelled
Cancelled → Retained | Cleaning → Deleted
Failed → Retrying | Cleaning
```

Each transition must be explicit, idempotent where feasible, logged, and tested.

### 7.9 `InstallChangeJournal`

Responsibilities:

- capture a lightweight pre-install baseline;
- record relevant filesystem changes;
- avoid expensive full hashing unless necessary;
- supply candidate generation with new/changed paths;
- persist enough information for recovery.

### 7.10 `LaunchCandidateDetector`

Candidate inputs:

- new `.exe` files;
- Wine Start Menu shortcuts;
- desktop shortcuts;
- `.lnk` target resolution;
- uninstall metadata;
- file metadata and names;
- install directory structure.

Ranking signals:

- executable created during installation;
- non-installer GUI subsystem binary;
- filename resembles product/directory name;
- target appears in a Start Menu shortcut;
- executable is not in redistributable, temporary, uninstall, crash-report, or updater locations;
- executable has a plausible icon/version resource;
- user previously selected a similar path for a known game.

The detector must return multiple candidates and confidence, never silently finalize a low-confidence result.

### 7.11 `GameLaunchCoordinator`

Responsibilities:

- resolve game, container, launch target, runtime, and compatibility profile;
- synchronize cloud saves where applicable;
- prepare runtime dependencies;
- start the process through existing GameNative launch facilities;
- manage pause/resume/stop and metrics without leaking local paths;
- report layered failures.

### 7.12 `ControllerInputRouter`

Responsibilities:

- normalize Android device/key/axis events;
- distinguish navigation mode from in-game forwarding;
- track active input modality;
- map semantic launcher actions;
- expose controller family for glyph selection;
- handle dead zones, repeat behavior, hot-plug, and multiple devices.

### 7.13 `FocusCoordinator`

Responsibilities:

- define predictable initial focus;
- store/restore focus per route;
- manage focus across lazy lists and dynamic content;
- prevent focus loss during asynchronous library updates;
- provide reusable Compose modifiers/components.

### 7.14 `ThemeManager`

Responsibilities:

- enumerate built-in and imported themes;
- validate, install, preview, apply, migrate, export, and remove themes;
- guarantee immutable safe themes;
- apply semantic tokens without restarting where possible;
- detect boot failures and recover.

### 7.15 `ThemePackageValidator`

Validates:

- archive safety;
- manifest/schema version;
- required fields and semantic tokens;
- file types and resource budgets;
- asset references;
- color parsing and contrast;
- typography fallback;
- layout preset compatibility;
- motion/reduced-motion alternatives.

### 7.16 `CompatibilityDiagnostics`

Responsibilities:

- produce structured errors with layer, code, user message, technical detail, and recovery actions;
- collect redacted logs;
- identify DRM/anti-cheat limitations without offering bypass instructions;
- export a support bundle after user preview.

---

## 8. Domain model

Exact Room entities depend on the upstream audit. The domain must nonetheless support these concepts.

### 8.1 Game

```kotlin
data class Game(
    val id: GameId,
    val source: GameSource,
    val title: String,
    val artwork: ArtworkSet,
    val containerId: ContainerId?,
    val launchTargetId: LaunchTargetId?,
    val installState: GameInstallState,
    val compatibilityProfileId: CompatibilityProfileId?,
    val controllerProfileId: ControllerProfileId?,
    val favorite: Boolean,
    val lastPlayedAt: Instant?
)
```

### 8.2 Game source

```kotlin
sealed interface GameSource {
    data class Steam(val appId: Long) : GameSource
    data class LocalPortable(val importId: ImportId) : GameSource
    data class LocalInstalled(val sessionId: InstallationSessionId) : GameSource
    data class LocalMedia(val mediaId: MediaId) : GameSource
}
```

### 8.3 Launch target

Fields:

- stable ID;
- container-visible executable path;
- working directory;
- arguments as an ordered structured list;
- environment overrides as structured key/value data;
- executable kind;
- runtime provider and version;
- optional Steam/store launch context;
- last validation result.

Never store a shell-concatenated command as the canonical model.

### 8.4 Container profile

Fields:

- runtime family and version;
- Windows version mode;
- architecture mode;
- graphics driver/backend;
- DX wrapper;
- audio configuration;
- resolution/scaling;
- CPU/translation preset;
- environment overrides;
- mounted drives/media;
- installed components;
- provenance: default, known config, user-customized.

### 8.5 Installation session

Fields:

- state and previous state;
- source URI/managed file reference;
- detected installer type;
- container ID;
- created/updated timestamps;
- progress summary;
- pre-install journal reference;
- candidate list and selected target;
- retained cleanup policy;
- last structured error.

---

## 9. Key workflows

### 9.1 Import portable game

```text
User selects Add → Game
→ SAF picker
→ persist permission
→ inspect executable
→ show compatibility summary and storage choice
→ create container
→ copy/map files
→ validate target
→ create unified Game record
→ optional artwork lookup/manual artwork
→ launch or return to library
```

Acceptance behavior:

- Back never discards completed copy work without confirmation.
- Process death resumes at the last durable step.
- Unsupported DOS-only executable is explained before a container is created.
- A malformed executable cannot reach launch command construction.

### 9.2 Run installer

```text
User selects Add → Installer
→ SAF picker
→ inspect and estimate storage
→ choose/suggest runtime profile
→ persist InstallationSession
→ create container
→ capture baseline
→ launch installer
→ monitor process tree
→ installer exits
→ detect candidates
→ user confirms executable
→ finalize Game + LaunchTarget
→ optional first-launch configuration
```

Important edge cases:

- installer launches child process and parent exits;
- installer remains in background;
- multiple games/components are installed;
- only a launcher is installed;
- runtime prerequisite asks for restart;
- user closes GameNative during installation;
- installer writes outside expected Program Files directories;
- source SAF permission disappears;
- storage fills during installation;
- no plausible executable is detected.

### 9.3 Steam game

```text
Steam sign-in
→ upstream library sync
→ adapt metadata into unified library projection
→ install/update through upstream path
→ assign/reuse container through upstream behavior
→ launch through GameLaunchCoordinator adapter
→ preserve upstream cloud and platform hooks
```

Local features must not be inserted into Steam authentication or depot download internals.

### 9.4 Theme import

```text
User selects .gntheme
→ copy to quarantine/temp area
→ validate archive and manifest
→ validate assets and budgets
→ compute accessibility report
→ isolated preview
→ user applies or cancels
→ atomically install theme
→ persist previous safe theme for rollback
```

### 9.5 Theme recovery

Triggers:

- launch-time recovery chord;
- accessible recovery action in settings;
- repeated startup failure after applying a theme;
- theme schema becomes unreadable after update.

Result:

- activate immutable safe theme;
- keep the failing theme disabled for inspection/export;
- never delete user content automatically.

---

## 10. UX and information architecture

### 10.1 Design direction

- Product UI, not a marketing surface.
- Calm, restrained palette by default; no acidic neon styling.
- Familiar console behavior inspired by Steam Deck and Xbox One.
- No decorative glassmorphism, gradient text, giant rounded cards, or gratuitous page choreography.
- One coherent component language across touch and controller modes.
- Technical settings use progressive disclosure.

### 10.2 Primary navigation

Recommended top-level destinations:

1. **Home** — continue playing, recent installations, active tasks.
2. **Library** — all games, filters, sorting, search.
3. **Downloads & installs** — Steam downloads and local installation sessions.
4. **Add** — game, installer, later disc image.
5. **Settings** — accounts, runtimes, storage, controls, appearance, diagnostics.

On phone portrait, use a compact bottom destination bar where space permits. On landscape/handheld/TV, use a controller-friendly rail or top-level console navigation. Do not maintain unrelated navigation trees for each form factor.

### 10.3 Home screen

Required content:

- continue/recently played;
- current download/install status;
- recent library additions;
- clear Add action;
- Steam connection status that is informative but not dominant.

Avoid:

- a dashboard of unrelated metric cards;
- forcing login before local use;
- autoplay video backgrounds;
- large decorative panels that reduce visible library density.

### 10.4 Library

- Grid/list density adapts structurally to form factor.
- Stable focus under filtering and async updates.
- Source appears as metadata, not as separate product silos.
- Installed/readiness status must be readable without color.
- Context actions: Play, Resume installation, Configure, Manage files, Artwork, Remove.

### 10.5 Add flow

First choice uses plain language:

- **Game already installed**
- **Install a game**
- **Disc image** (when implemented)

Do not lead with emulator/container terminology. Advanced users may open profile details before proceeding.

### 10.6 Installation screen

- Show current lifecycle stage, not a fake single percentage across unrelated phases.
- Keep installer window primary while GameNative provides an accessible side/overlay status.
- On installer exit, clearly distinguish success detection from installer process completion.
- If multiple executable candidates exist, show filename, location, icon, version metadata, and confidence reason.

### 10.7 Game details

Primary actions:

- Play/Resume setup;
- compatibility summary;
- controls;
- manage installation;
- artwork/details.

Advanced container and runtime settings live under a clearly named compatibility section, not on the first screen.

### 10.8 Motion

- Most UI state transitions: 150–250 ms.
- Motion communicates focus, selection, navigation, progress, or completion.
- No staged page-load animations.
- Every nonessential animation has a reduced-motion alternative.

### 10.9 Responsive behavior

Test at minimum:

- compact portrait phone;
- large portrait phone;
- landscape phone;
- 7-inch Android handheld;
- tablet;
- 16:9 external display/TV.

Account for cutouts, gesture insets, system bars, IME, and overscan-safe controller UI where relevant.

---

## 11. Theme package specification baseline

Working extension: `.gntheme`.

### 11.1 Archive layout

```text
theme.gntheme
├── manifest.json
├── tokens.json
├── typography.json
├── motion.json
├── layouts.json
├── icons/
├── fonts/
├── backgrounds/
├── sounds/
└── previews/
```

Only `manifest.json` and the required semantic subset of `tokens.json` are mandatory. Other files are optional and fall back to built-in behavior.

### 11.2 Manifest minimum

```json
{
  "schemaVersion": 1,
  "id": "author.theme-name",
  "name": "Theme Name",
  "version": "1.0.0",
  "author": "Author",
  "minimumAppVersion": "0.1.0",
  "entrypoints": {
    "tokens": "tokens.json",
    "typography": "typography.json",
    "motion": "motion.json",
    "layouts": "layouts.json"
  }
}
```

### 11.3 Semantic tokens

At minimum:

- background and elevated surface roles;
- primary/secondary text;
- accent and on-accent;
- focus ring plus non-color focus shape/weight;
- selected, disabled, loading;
- success, warning, error, info and corresponding content colors;
- scrim;
- game-cover placeholder;
- navigation surface;
- button roles;
- input-control roles.

Themes customize roles, not arbitrary individual screen colors. This allows deep restyling while preserving state semantics.

### 11.4 Layout customization boundary

To permit major visual change without executable themes:

- expose a finite set of versioned layout primitives/presets;
- allow ordering, visibility, density, alignment, image treatment, and navigation style within constraints;
- validate that required actions remain reachable;
- do not deserialize arbitrary Compose trees or reflection-based component names.

### 11.5 Theme editor

Required later-stage capabilities:

- duplicate a built-in or imported theme;
- edit semantic colors with contrast feedback;
- select/import fonts and assets;
- adjust density, shape scale, focus style, sound, and motion energy;
- choose supported layout presets;
- preview phone portrait, handheld landscape, and TV/controller states;
- export `.gntheme`.

---

## 12. Win16 strategy

### 12.1 Discovery spike

Before committing to an implementation, build a time-boxed technical spike comparing:

- WineVDM/OTVDM-style integration feasibility on Android ARM through the existing environment;
- a Wine configuration capable of Win16 where available;
- a Windows 3.x/9x emulated environment exposed through a runtime-provider boundary.

Evaluate:

- license compatibility;
- ARM/x86 translation interaction;
- installer behavior;
- graphics/DirectDraw and audio;
- filesystem integration;
- startup time and storage cost;
- controller/touch injection;
- maintenance burden.

### 12.2 Win16 acceptance corpus

Use legally obtainable samples covering:

- simple NE executable;
- Win16 installer for a Win32 game;
- Windows 3.1-era game;
- early DirectDraw/WinG title;
- title requiring CD presence without protected-media bypass;
- malformed and DOS-only files for correct rejection.

### 12.3 Product behavior

- Auto-detection may recommend a Win16 runtime.
- The UI clearly labels the runtime as experimental until the corpus passes.
- A failed Win16 launch must not fall through to random Proton profiles without explanation.
- Users can override the runtime when multiple providers declare support.

---

## 13. Migration and upstream strategy

### 13.1 Repository setup

- Import upstream GameNative with history if practical.
- Add `upstream` remote pointing to the original repository.
- Use a project branch under `codex/` for implementation unless the owner chooses another workflow.
- Record upstream commit SHA used as the baseline.
- Preserve submodules and large/bundled runtime asset instructions.

### 13.2 Phase 0 architecture map

Create `docs/UPSTREAM_ARCHITECTURE.md` containing real paths and responsibilities for:

- app entry/navigation;
- Compose theme/tokens/components;
- database/entities/DAO/migrations;
- game abstraction and Custom Games;
- Steam authentication/library/install/cloud/launch;
- container creation/configuration/deletion;
- Wine/Proton/FEX runtime handling;
- file/storage management;
- controller/touch handling;
- downloads/install state;
- analytics and diagnostics;
- tests/build variants.

### 13.3 Compatibility adapter policy

- Wrap upstream services behind local interfaces first.
- Avoid renaming/moving large upstream areas during functional work.
- Separate mechanical refactors from behavior changes.
- Track upstream divergences in `docs/UPSTREAM_DIVERGENCES.md`.
- Rebase/merge upstream at controlled checkpoints, not midway through database migrations or runtime changes.

### 13.4 Data migration

Required migration properties:

- existing Steam and Custom Game records remain visible;
- existing container IDs/paths remain valid;
- stable internal IDs are backfilled deterministically;
- source-specific metadata moves without loss;
- migration is idempotently tested against copied production-like databases;
- rollback limitations are documented before release.

---

## 14. Delivery phases

Each phase ends with buildable software, tests, documentation, and a go/no-go review.

### Phase 0 — Upstream import and baseline

Tasks:

- import current upstream source and submodules;
- reproduce the documented Android Studio/Gradle build;
- record toolchain versions;
- build debug APK without feature changes;
- map upstream architecture;
- capture baseline screenshots and navigation video;
- run Steam login/library/install/launch smoke tests on a test account;
- run existing Custom Game launch smoke test;
- inventory existing controller behavior and Compose focus problems;
- inventory design tokens and reusable components;
- create `DESIGN.md` from the actual code, then define the redesign delta.

Exit criteria:

- reproducible debug build;
- baseline test evidence;
- architecture map with real file paths;
- no unresolved uncertainty about where Steam, containers, Custom Games, and launch commands live;
- approved data migration approach.

### Phase 1 — Domain decoupling and persistence

Tasks:

- introduce stable internal `GameId`;
- define `GameSource`, `LaunchTarget`, `InstallationSession`, and structured error models;
- create/adapt repositories;
- implement Room migration;
- create Steam adapter without behavior change;
- adapt existing Custom Games into unified model;
- add migration and repository tests.

Exit criteria:

- current Steam and Custom Game libraries render through the unified model;
- no Steam feature regression in smoke tests;
- migration preserves all baseline records and containers;
- source-independent launch coordinator exists behind tests.

### Phase 2 — Local portable game MVP

Tasks:

- implement SAF executable/directory selection;
- implement `ExecutableInspector`;
- implement storage estimate/copy/reference workflow;
- adapt `ContainerService`;
- create import state persistence;
- build Add Game wizard;
- create local game record and launch target;
- launch through unified coordinator;
- implement cleanup and retry.

Exit criteria:

- a representative DRM-free Win32 game imports and launches;
- a representative Win64 game imports and reaches launch on supported hardware;
- malformed/DOS-only inputs fail before container creation;
- process death during copy can recover or cleanly retry;
- Steam regression suite remains green.

### Phase 3 — Installer MVP

Tasks:

- implement persisted installation state machine;
- create container before installer launch;
- implement pre-install journal;
- run `.exe` and `.msi` installers;
- monitor installer process tree;
- implement candidate scanning/ranking;
- implement executable confirmation UI;
- support restart-required and resume;
- finalize local library entry;
- implement retained/delete cleanup choices.

Exit criteria:

- at least three installer patterns complete end-to-end;
- parent-exits/child-continues case works;
- no-candidate case allows manual selection;
- interrupted installation resumes after app process death;
- cancellation never produces an invisible orphan container;
- GameNative's normal post-container launch/configuration path is reused.

### Phase 4 — New navigation shell and design system

Tasks:

- establish semantic design tokens and base components;
- implement responsive navigation structure;
- rebuild Home, Library, Add, Installs, Game Details, Settings, and Steam account surfaces;
- implement loading, empty, error, disabled, and offline states;
- apply restrained built-in themes;
- support font scaling, TalkBack, reduced motion, and high contrast;
- preserve all required upstream settings.

Exit criteria:

- all primary tasks work in phone portrait and handheld landscape;
- no acidic/glass/oversized-card visual patterns remain in rebuilt surfaces;
- component states are consistent;
- accessibility audit has no unresolved critical issue;
- baseline Steam features remain discoverable.

### Phase 5 — Controller parity

Tasks:

- implement semantic action router;
- implement controller family detection and glyph packs;
- establish focus coordinators and restoration;
- test D-pad, sticks, buttons, triggers, reconnect, and multiple devices;
- adapt dialogs, lazy grids, menus, text entry, and theme UI;
- integrate with existing in-game input/control editor without regression.

Exit criteria:

- entire acceptance journey completes using only an Xbox-compatible controller;
- the same journey completes using only a DualSense-compatible controller;
- no focus traps or invisible focus in required screens;
- glyphs change correctly when active controller changes;
- touch switching does not break focus recovery.

### Phase 6 — Theme packages and editor foundation

Tasks:

- finalize schema version 1;
- implement archive validation and resource budgets;
- implement semantic token loader;
- implement preview/apply/rollback;
- implement import/export;
- provide built-in safe light/dark/high-contrast themes;
- implement recovery chord/path;
- expose typography, shape, icon, background, sound, motion, and layout presets;
- implement first editor surface.

Exit criteria:

- valid `.gntheme` imports and applies without restart where supported;
- malicious traversal/oversized/invalid packages are rejected;
- broken theme cannot prevent recovery;
- export/import round-trip preserves supported customization;
- theme preview covers touch and controller focus states;
- safe themes remain immutable.

### Phase 7 — Win16 runtime

Tasks:

- complete feasibility spike and ADR;
- implement selected runtime provider;
- integrate dependency delivery and integrity checks;
- implement Win16 profiles and UI labeling;
- support Win16 installer flow;
- run acceptance corpus and document limitations.

Exit criteria:

- agreed Win16 corpus meets its pass threshold;
- Win16 is auto-detected and routed correctly;
- DOS-only binaries remain clearly out of scope;
- failure and fallback behavior are deterministic.

### Phase 8 — Installation media

Tasks:

- implement ISO inspection;
- implement BIN/CUE pair validation;
- map/mount media into a container using a supported approach;
- retain media mapping for games requiring disc presence;
- integrate media installer workflow;
- detect protected/unsupported cases without bypass behavior.

Exit criteria:

- ordinary Windows ISO installer completes end-to-end;
- valid multi-track metadata is preserved or explicitly rejected if unsupported;
- removing/moving source media produces a recoverable error;
- DRM limitations are accurately explained.

### Phase 9 — Hardening and release

Tasks:

- full migration matrix;
- device/GPU/input/accessibility matrix;
- performance and memory profiling;
- crash/process-death testing during every durable state;
- security review of imported content;
- privacy/analytics review;
- license and third-party notice review;
- localization readiness and pseudolocalization;
- support diagnostics and user documentation;
- release candidate regression against upstream features.

Exit criteria:

- all P0/P1 defects closed;
- no known data-loss defect;
- release checklist complete;
- documented compatibility limitations;
- signed release artifact built through a reproducible process.

---

## 15. Testing strategy

### 15.1 Unit tests

- PE/NE/MSI signature parsing, including malformed/fuzzed headers.
- Runtime provider selection.
- Installation state transitions and invalid transitions.
- Launch-candidate ranking.
- Command/argument serialization.
- Theme manifest/schema parsing.
- Archive traversal and resource-budget rejection.
- Contrast and semantic-token validation.
- Controller event normalization and glyph selection.
- Redaction of logs and analytics payloads.

### 15.2 Integration tests

- Room migrations from all supported versions.
- SAF providers with file, document, and directory semantics.
- Container create/retain/delete behavior.
- Installer journal plus candidate detector on fixture filesystems.
- Game launch request construction using fake runtime providers.
- Steam adapter behavior with recorded/fake responses where legally and technically feasible.
- Theme import/preview/apply/rollback round trips.

### 15.3 Compose/UI tests

- Touch navigation for all primary routes.
- D-pad/analog focus movement.
- Focus restoration after dialog/navigation.
- Large text and long localized strings.
- Empty/loading/error/offline states.
- Controller glyph switching.
- Theme recovery.
- Installation process-death restoration.

### 15.4 Device matrix

At minimum:

| Dimension | Coverage |
|---|---|
| Android | Minimum supported, one mid version, current target |
| GPU | Adreno/Turnip path; Mali-supported path |
| CPU class | Supported midrange and high-end ARM64 |
| Form factor | Phone portrait, phone landscape, handheld, tablet/TV-like display |
| Input | Touch, Xbox-compatible, DualSense-compatible, keyboard/mouse smoke |
| Display | 60 Hz, high-refresh where available, cutout/insets |
| Accessibility | 200% font scale where feasible, TalkBack, high contrast, reduced motion |

### 15.5 Compatibility corpus

Maintain a legal internal matrix rather than relying on anecdotes:

- small DRM-free Win32 DirectX 8/9 title;
- Win32 DirectDraw-era title;
- representative Win64 DX11 title;
- representative DX12 title within device capacity;
- EXE installer;
- MSI installer;
- parent/child bootstrap installer;
- installer requiring redistributables;
- Win16 samples from Section 12;
- unsupported kernel anti-cheat title for correct diagnosis;
- ordinary ISO installer when Phase 8 begins.

For each entry record device, runtime, container profile, result, FPS where meaningful, known issues, and reproducible steps.

---

## 16. Observability and errors

### 16.1 Structured error shape

Every actionable failure should contain:

- stable error code;
- affected layer;
- concise user-facing summary;
- optional technical details;
- recoverability classification;
- one or more supported actions;
- correlation/session ID without credentials or personal paths.

Example layers:

- `SOURCE_ACCESS`
- `STORAGE`
- `EXECUTABLE_INSPECTION`
- `CONTAINER`
- `RUNTIME_PREPARE`
- `TRANSLATION`
- `GRAPHICS`
- `DEPENDENCY`
- `INSTALLER`
- `GAME_PROCESS`
- `STEAM_AUTH`
- `STEAM_DOWNLOAD`
- `DRM_OR_ANTICHEAT`
- `THEME_VALIDATION`

### 16.2 User-facing recovery

Prefer concrete actions:

- grant access again;
- free storage;
- retry from saved state;
- choose another executable;
- change runtime/profile;
- restore default theme;
- view/export redacted logs.

Avoid generic “Something went wrong” when the failing layer is known.

---

## 17. Risks and mitigations

### 17.1 Upstream churn

Risk: GameNative changes rapidly and feature work may conflict with upstream.

Mitigation:

- adapters around upstream services;
- recorded baseline SHA;
- small focused commits;
- scheduled upstream integration checkpoints;
- divergence documentation.

### 17.2 Steam regression

Risk: unified models or new navigation break authentication, installs, cloud saves, or offline launch.

Mitigation:

- do not rewrite Steam internals initially;
- preserve upstream flows behind `SteamFacade`;
- mandatory smoke suite at every phase exit.

### 17.3 Android storage providers

Risk: not every SAF provider supports paths, directory enumeration, random access, or persistent permissions.

Mitigation:

- operate on streams/document APIs;
- managed-copy fallback;
- provider capability checks;
- explicit missing-permission recovery.

### 17.4 Installer ambiguity

Risk: installer exit does not imply success and candidate detection may pick an updater/uninstaller.

Mitigation:

- state machine plus process-tree monitoring;
- ranked candidates with user confirmation;
- manual browse fallback;
- never silently finalize low confidence.

### 17.5 Win16 feasibility

Risk: runtime may be legally, technically, or operationally unsuitable.

Mitigation:

- provider abstraction;
- time-boxed spike and ADR;
- experimental label;
- acceptance corpus before product commitment.

### 17.6 Unlimited themes

Risk: deep customization can destroy accessibility, focus behavior, performance, or startup.

Mitigation:

- semantic/versioned schema;
- finite layout primitives;
- no executable code;
- validation and budgets;
- safe theme and recovery chord;
- quarantined preview.

### 17.7 DRM and anti-cheat expectations

Risk: users interpret local-file support as universal compatibility or DRM bypass.

Mitigation:

- compatibility promise in product copy;
- structured limitation diagnostics;
- no bypass implementation;
- verified compatibility database and per-device reporting.

---

## 18. Agent work protocol

### 18.1 Before starting a task

Each agent must:

1. read `PRODUCT.md` and this plan;
2. read `docs/UPSTREAM_ARCHITECTURE.md` once Phase 0 creates it;
3. identify requirement IDs covered;
4. inspect current upstream-conformant implementation before editing;
5. record dependencies on unfinished components;
6. avoid modifying unrelated upstream code.

### 18.2 Task proposal format

Every implementation task should state:

- requirement IDs;
- affected components/files;
- data/schema impact;
- user-visible behavior;
- error and recovery behavior;
- controller/touch/accessibility behavior;
- security/privacy considerations;
- tests to add;
- upstream regression checks;
- definition of done.

### 18.3 Ownership boundaries

Parallel work is safe only across well-defined boundaries. Suggested tracks after Phase 1 contracts stabilize:

- **Runtime/storage track:** inspector, SAF, managed storage, containers, runtime providers.
- **Install track:** session state machine, journaling, process monitoring, candidate detection.
- **Product UI track:** design system, navigation, library, add/install/detail/settings screens.
- **Input/accessibility track:** normalized actions, focus, glyphs, controller test harness, TalkBack.
- **Theme track:** schema, validator, manager, preview/editor/recovery.
- **Steam regression track:** facade, baseline tests, upstream merge verification.

Agents must not independently invent duplicate domain models. Shared interfaces and migrations land before dependent parallel work.

### 18.4 Change discipline

- One behavior change per focused commit where practical.
- Schema migration commits include migration tests.
- UI commits include touch and controller states.
- New theme tokens include both built-in themes and contrast verification.
- New errors use structured codes and recovery actions.
- Any change to upstream Steam/container internals requires a regression note.
- No destructive cleanup without an explicit retained/deleted user choice where user game data is involved.

---

## 19. Definition of done

A feature is done only when:

- mapped requirement IDs are satisfied;
- implementation follows real upstream architecture;
- unit/integration/UI tests pass;
- touch, Xbox-compatible, and DualSense-compatible paths are verified where applicable;
- accessibility states are implemented;
- process death, cancellation, retry, offline, empty, loading, and error cases are handled where applicable;
- no credentials, tokens, or local paths leak into analytics/log exports;
- migration/cleanup behavior is tested if persistent data changes;
- Steam regression checks pass if shared code was touched;
- user documentation and diagnostics are updated;
- build succeeds on the supported toolchain;
- no P0/P1 defect remains.

---

## 20. Milestone acceptance journeys

### Journey A — Local portable game

Using touch only, import a DRM-free Win32 game, create a container, launch it, quit, find it in Recent, change controls, and relaunch it.

Repeat the complete launcher flow with an Xbox-compatible controller and a DualSense-compatible controller.

### Journey B — Installer

Select an installer, observe container creation, complete installation, choose the detected game executable, launch the game, reboot the Android app, and launch the persisted library entry again.

Repeat with the app process killed during container creation and during candidate detection.

### Journey C — Steam preservation

Sign in, refresh library, install/update a game, launch it, synchronize saves where supported, switch offline, relaunch where upstream supports it, log out, and verify local games remain intact.

### Journey D — Theme customization

Import a valid theme, preview it on phone and handheld layouts, apply it, use the launcher with both controller glyph families, export it, restore a safe theme, and re-import the exported file.

Attempt malicious, oversized, incomplete, low-contrast, and incompatible theme packages and verify safe failure/recovery.

### Journey E — Win16

Select a known Win16 executable or installer, receive the correct runtime recommendation, create the appropriate environment, launch/install it, and receive a deterministic compatibility result with actionable diagnostics.

---

## 21. Decisions that must be recorded as ADRs

Create Architecture Decision Records under `docs/adr/` for:

1. unified game identity and source modeling;
2. local-file copy versus reference policy;
3. installation journal implementation;
4. runtime-provider boundary;
5. selected Win16 approach;
6. theme schema and non-executable customization boundary;
7. controller normalization and glyph-family detection;
8. upstream synchronization strategy;
9. installation-media mapping approach;
10. analytics/privacy treatment of local imports.

---

## 22. Immediate next actions

1. Import current GameNative upstream source into this repository with required submodules/history.
2. Record baseline upstream SHA and build instructions.
3. Build the unchanged debug APK.
4. Create `docs/UPSTREAM_ARCHITECTURE.md` with real file/class mappings.
5. Capture Steam and Custom Game baseline regression tests.
6. Generate `DESIGN.md` from the actual Compose theme/components and approve the redesign delta.
7. Convert Phase 1 into small tracked tasks referencing requirement IDs.

Feature implementation must not begin before actions 1–5 are complete. Otherwise agents will design against assumed classes, risk corrupting existing data, and unnecessarily rewrite working Steam/container behavior.

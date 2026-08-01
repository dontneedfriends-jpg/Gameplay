# Gameplay

Gameplay is an open-source Android gaming shell for installing, organizing, configuring, and running legally owned Windows games directly on ARM64 phones and handhelds.

It combines a controller-first console interface with isolated Wine/Proton containers. Games can come from a connected storefront or from files on the device: Gameplay treats local executables and installers as first-class library content rather than requiring every title to belong to Steam.

Gameplay is a modified fork of [GameNative](https://github.com/utkarshdalal/GameNative). It remains licensed under GPL-3.0, preserves upstream attribution, and develops a separate product direction focused on a unified console shell, local installation workflows, extensive theming, and equal support for touch and physical controllers.

> Gameplay is under active development. Compatibility varies by device, GPU driver, Android version, CPU architecture, Windows runtime, graphics translation layer, and the game itself. It is not a cloud-streaming service and does not include games, licenses, product keys, or DRM circumvention.

## What Gameplay is building

The goal is a complete Android console environment for Windows games—not a collection of disconnected emulator dialogs.

- A unified library for Steam, GOG, Epic, Amazon, imported games, and locally installed titles.
- A landscape shell designed for handheld consoles and controller navigation.
- Direct import of Windows executables, MSI packages, and EXE installers from Android storage.
- Automatic creation of a dedicated container when an installer is selected.
- Post-install executable discovery so an installed game can be added to the library and launched again.
- Reusable runtime caches so Wine/Proton components are not downloaded separately for every container.
- Per-game configuration for Wine/Proton, DXVK/VKD3D, GPU drivers, CPU translation, environment variables, display, and controls.
- Touch controls and physical Xbox-compatible and DualSense/PS5-compatible controllers as equal input methods.
- A semantic theme system with built-in themes, full-screen editing, import/export, validation, and recovery.

The detailed implementation state and acceptance criteria live in [DEVELOPMENT_PLAN.md](DEVELOPMENT_PLAN.md).

## Current capabilities

### Games and installation

- Import local Windows executables into the library.
- Select an installer, create its container, run the installation, and continue into the normal Gameplay launch flow.
- Reopen installed games without repeating the installer process.
- Reuse cached runtime payloads across containers.
- Configure each title independently without changing the defaults for the rest of the library.
- View important runtime choices—graphics driver, Wine/Proton, and DXVK/VKD3D—close to the game configuration surface.

ISO and other disc images are not currently treated as executables. They require a dedicated mounted-media workflow and remain planned work.

### Storefronts and library

- Connect to Steam using the inherited GameNative integration.
- Retain installed library data and session state while navigating instead of reconnecting on every tab change.
- Hide storefront navigation when its service is not authenticated.
- Use installed games as the primary home library instead of a recommendations feed.
- Display Steam achievements in Gameplay's console-oriented game screen.
- Preserve GOG, Epic, and Amazon library support from the upstream codebase.
- Sort, filter, search, and select views through controller-friendly library options.

### Console interface

- Full-screen landscape navigation inspired by dedicated handhelds and living-room consoles without copying their visual identity.
- Separate system-menu and context-action behavior.
- Quick actions for play/install, details, search, library options, and adding games.
- Category-based application, container, and per-game settings.
- Full-screen nested settings instead of deep stacks of mobile dialogs.
- Restrained surfaces, low-saturation themes, predictable focus, and narrow-landscape layouts.

### Themes

Gameplay themes are semantic documents rather than simple accent colors. A theme may redefine surfaces, text roles, focus states, status colors, shape, density, and related presentation tokens while retaining a safe recovery path.

Included profiles cover dark, OLED, light, forest, copper, wine, and arctic directions. Custom themes can be created in the application or imported from a file. The versioned format is documented in [docs/THEMES.md](docs/THEMES.md).

## Compatibility scope

Gameplay targets Windows software that can run through the bundled Wine/Proton-compatible stack and Android-native translation components.

- Modern 32-bit and 64-bit Windows games are supported when the selected runtime and hardware permit it.
- Vulkan-capable hardware is strongly recommended for modern DirectX games.
- Older DirectDraw/Direct3D titles may need game-specific graphics and Wine settings.
- Win16 support depends on the chosen Wine path and game architecture; it is not universal.
- DOS games need a future DOSBox-style integration and are not currently a promised compatibility target.
- Anti-cheat, kernel drivers, unusual DRM, launchers, codecs, and device-specific GPU issues may prevent a game from running.

There is no meaningful single “supported up to year X” cutoff. A demanding old title may fail while a newer title works well. Compatibility should be evaluated per game, runtime, driver, and device.

## Requirements

- ARM64 Android device.
- Android 10 or newer for the recommended `modern` build (`minSdk 29`, `targetSdk 36`).
- Android 8 or newer for the compatibility-oriented `legacy` build (`minSdk 26`, `targetSdk 28`).
- Sufficient free storage for the Windows runtime, container, installer, extracted files, and game data.
- Vulkan support for DXVK/VKD3D-based games.
- Legally obtained game files and any licenses required by their publishers.

The XR flavors are experimental and are not the primary Gameplay target.

## Building from source

Use a current Android Studio installation with its bundled JDK. Clone submodules when obtaining a fresh checkout:

```sh
git clone --recurse-submodules https://github.com/dontneedfriends-jpg/Gameplay.git
cd Gameplay
```

### Windows

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :app:assembleModernDebug --no-daemon
```

### Linux and macOS

```sh
./gradlew :app:assembleModernDebug --no-daemon
```

The resulting APK is written to:

```text
app/build/outputs/apk/modern/debug/app-modern-debug.apk
```

Other available flavors include `legacy`, `legacyXr`, `modern`, and `modernXr`. Gameplay development primarily validates `modernDebug`.

### Updates and releases

Gameplay checks the latest non-prerelease GitHub Release for `dontneedfriends-jpg/Gameplay` at most once per day. An update is never downloaded without user approval. The release must contain these assets:

- `Gameplay-modern-release.apk`
- `Gameplay-modern-release.json`
- `SHA256SUMS.txt`

The APK is verified for package name, newer version code, size, and SHA-256 before Android's package installer is opened. The release workflow is [`.github/workflows/release.yml`](.github/workflows/release.yml). Production releases must use the same signing key as the installed `app.gameplay` package; losing that key makes in-place updates impossible.

On Windows, run `powershell -ExecutionPolicy Bypass -File .\scripts\release-preflight.ps1 -Tag v1.1.2` locally before publishing. It requires local `app/keystores/keystore.properties`, which must never be committed.

### Optional artwork integration

Automatic artwork lookup for imported games can use a SteamGridDB API key placed in `local.properties`:

```properties
STEAMGRIDDB_API_KEY=your_api_key_here
```

Never commit `local.properties`, API keys, signing credentials, keystores, or generated APKs.

## Repository layout

- `app/` — Android application, Jetpack Compose interface, storefront integrations, game library, and container orchestration.
- `ubuntufs/` — filesystem/runtime packaging module.
- `app/src/main/assets/` — manifests and bundled runtime-support assets.
- `docs/THEMES.md` — versioned Gameplay theme format and authoring notes.
- `DEVELOPMENT_PLAN.md` — completed work, active priorities, planned work, and definitions of done.
- `PRODUCT.md` — product principles and design constraints.
- `NOTICE` — Gameplay fork attribution and naming information.
- `THIRD_PARTY_NOTICES` — component licenses, notices, and source information.

The Kotlin namespace and Android application ID remain `app.gamenative`. This is an internal compatibility identifier used by existing data, migrations, JNI bindings, and native components; it is not the displayed product name.

## Development expectations

Gameplay is controller-first but not controller-only. Changes should preserve touch behavior and existing user data while moving ordinary navigation toward the shared console shell.

For interface changes, verify:

- narrow landscape screens;
- D-pad and analog focus traversal;
- Xbox A/B and PlayStation Cross/Circle semantics;
- Back behavior and focus restoration;
- touch targets, scrolling, empty states, and long localized strings.

For runtime changes, verify both a storefront title and a locally imported executable or installer where applicable. Container and database changes must preserve existing installations.

Before submitting code, build at least the primary variant:

```sh
./gradlew :app:assembleModernDebug --no-daemon
```

New bundled binaries must include their applicable license, notice, and reproducible source or source-offer information. Do not remove upstream copyright statements.

## Contributing

Issues and focused pull requests are welcome. Describe the device, Android version, SoC/GPU, selected driver, Wine/Proton version, graphics layer, and the exact stage that fails when reporting compatibility problems. Do not attach copyrighted games, credentials, authentication tokens, or private logs.

Large interface changes should follow [PRODUCT.md](PRODUCT.md) and update [DEVELOPMENT_PLAN.md](DEVELOPMENT_PLAN.md) when they complete or materially change a tracked requirement.

## License, attribution, and trademarks

Gameplay is distributed under the [GNU General Public License version 3](LICENSE). If you distribute a modified APK or other binary, you must satisfy the GPL-3.0 requirements, including providing the corresponding source under the same license.

GameNative and its contributors retain copyright in their original work. Gameplay's fork attribution is recorded in [NOTICE](NOTICE), and licenses or source information for bundled components are recorded in [THIRD_PARTY_NOTICES](THIRD_PARTY_NOTICES) and beside relevant assets.

Gameplay is not affiliated with or endorsed by Valve, Microsoft, Sony, Epic Games, GOG, Amazon, CodeWeavers, or the Wine project. Steam, Xbox, PlayStation, Windows, and other names and marks belong to their respective owners.

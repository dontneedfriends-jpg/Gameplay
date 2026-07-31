# Contributing to Gameplay

Gameplay welcomes focused fixes, compatibility improvements, interface work, documentation, and carefully integrated upstream changes.

## Before opening a change

1. Check the current scope and status in [DEVELOPMENT_PLAN.md](DEVELOPMENT_PLAN.md).
2. Follow the product and interaction principles in [PRODUCT.md](PRODUCT.md) for interface work.
3. Preserve existing installations, containers, preferences, and database migrations.
4. Keep touch, Xbox-compatible controllers, and DualSense-compatible controllers functional across primary workflows.
5. Do not include proprietary games, credentials, API keys, signing material, generated APKs, or user logs containing private data.

## Building

Use a current Android Studio installation and clone the repository with its submodules. The primary development build is:

```sh
./gradlew :app:assembleModernDebug --no-daemon
```

On Windows, use `gradlew.bat` and Android Studio's bundled JDK. More details are available in [README.md](README.md).

## Pull requests

Keep a pull request limited to one coherent change. Explain:

- what behavior changed;
- why the change is needed;
- which devices, Android versions, GPUs, runtimes, and input methods were tested;
- whether stored data, containers, bundled components, or licensing notices are affected.

For visual changes, include screenshots or a short recording of touch and controller navigation when practical. For runtime bugs, include sanitized logs and the affected launch stage.

## Code and interface expectations

- Use the existing architecture and shared console components before adding another one-off dialog or visual system.
- Ordinary navigation should use full-screen console pages; reserve modal dialogs for short blocking decisions, authentication, confirmation, and destructive actions.
- Verify focus order, focus restoration, Back/B/Circle behavior, scrolling, narrow landscape heights, and localized text.
- Keep network, database, and filesystem work off the UI thread.
- Avoid unnecessary reconnects, repeated downloads, and per-item database queries in large libraries.
- Add or update tests when a change has a stable automated test boundary.

## Licensing and bundled components

Gameplay is GPL-3.0 and contains work inherited from GameNative. Do not remove upstream copyright or license notices.

Any new bundled binary, library, runtime component, artwork, or patch set must include the applicable license and attribution. Update [THIRD_PARTY_NOTICES](THIRD_PARTY_NOTICES) when required and provide corresponding source or a valid source offer for GPL-covered binaries.

By contributing, you agree that your contribution may be distributed under the repository's GPL-3.0 license.

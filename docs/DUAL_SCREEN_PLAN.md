# Dual-screen + Library views â€” implementation plan

Branch: `feature/dual-screen`

## Phase 0. Foundation
- [x] `utils/DualScreenDevice.kt`: AYN Thor detection (build props) + `DisplayManager.displays.size > 1`
- [x] `rememberIsDualScreenDevice()`, `rememberHasExternalDisplay()` composables
- [x] Unit tests for model parsing

## Phase 1. External display
- [ ] Audit `externaldisplay/` (ExternalDisplayInputController, SwapInputOverlayView, externalDisplayMode, externalDisplaySwap) on Thor
- [ ] Per-device container defaults for Thor (externalDisplayMode=touchpad)
- [ ] Quick Menu portrait/second-screen layout variant
- [ ] ContainerConfigDialog: category rail on top when width < 600dp
- [ ] Tests: device classification, container defaults

## Phase 2. Remove LIST collection mode
- [x] Remove `PaneType.LIST` from layout picker; migrate stored `LIST -> GRID_CAPSULE` in `PrefManager.libraryLayout`
- [x] Delete `LibraryListCard.kt` and LIST branches (`LibraryListPane` kept — it hosts grids)
- [x] Clean string resources (no LIST-specific strings found), filter tests

## Phase 3. Installed view: small cards + blurred hero backdrop
- [x] `LibraryDynamicBackdrop` reused as-is (blur 7dp, desaturate, scrim)
- [x] `PaneType.INSTALLED_COMPACT` + small cards (96-120dp) over backdrop, transparent ListPane scaffold
- [x] Backdrop tracks focused grid index live
- [x] Tests: existing suite green

## Phase 4. Nintendo 3DS home mode (Thor)
- [x] `PaneType.DS_HOME` + `LibraryDsHomePane.kt`: square icon grid below, hero card above
- [ ] Thor: bottom zone on second display (Presentation API) — needs device
- [x] Icon scale steps S/M/L (Y button), `PrefManager.dsHomeIconScale`
- [x] D-pad model: default focus search grid <-> hero (focusGroup semantics)
- [x] DS_HOME enters view cycle only on dual-screen devices
- [ ] Tests: scale steps, focus model, visibility gating

## Phase 5. Polish
- [ ] GamepadActionBar hints for new modes (Y = scale, X = details)
- [ ] All flavors build (legacy/modern/Xr), Robolectric pass, on-device Thor pass

Order: 0 -> 2 -> 3 -> 1 -> 4 -> 5

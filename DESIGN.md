# Design

## Surfaces & chrome

### Bottom hint bars (gamepad button hints)

All controller hints ("which buttons do what") live in **one canonical place**:
a bottom strip at the screen edge. Every hint is a button glyph + short label.

- **Fill**: translucent vertical gradient, never a flat opaque band.
  From `background.copy(alpha = 0f)` at the top to `~0.72–0.92` at the bottom
  (see `GamepadActionBar`). The strip reads as content fading out, not as a
  separate panel.
- **Focus**: the strip is never focusable (`focusProperties { canFocus = false }`).
- Filled backgrounds are reserved for the interactive elements themselves
  (chips, buttons), not for the strip.
- Top overlays (tab bars) mirror the same idea with an inverted gradient
  (`background 0.88 → 0`).

### Console focus vocabulary

- `focusRing` (2dp, tertiary) + `surfaceContainerHighest` on focus.
- 44dp minimum touch targets.
- `motionSpec` for every animation; reduced-motion falls back to snap/crossfade.

## Color

Restrained: tinted neutrals + accent for primary actions, selection, and
state. Semantic statuses come from `PluviaTheme.colors`
(statusInstalled/statusAvailable/statusDownloading, accentDanger, accentPurple).

## Motion

150–250ms, ease-out, crossfade for content swaps (hero, backdrop).
No bounce, no decorative animation; motion conveys state only.

# Gameplay Theme format

Gameplay themes are UTF-8 JSON documents. Use the `.gameplay-theme.json` suffix so exported files are easy to identify.

## Schema version 1

```json
{
    "schemaVersion": 1,
    "name": "Gameplay Slate",
    "dark": {
        "primary": "#6F91AA",
        "onPrimary": "#FFFFFF",
        "background": "#101419",
        "onBackground": "#F2F5F7",
        "surface": "#171D24",
        "surfaceElevated": "#202831",
        "onSurface": "#F2F5F7",
        "textMuted": "#B4BEC8",
        "border": "#3A4651",
        "success": "#5B9B78",
        "warning": "#C39A5C",
        "danger": "#C56D6C"
    },
    "light": {
        "primary": "#315E78",
        "onPrimary": "#FFFFFF",
        "background": "#F7F9FA",
        "onBackground": "#182026",
        "surface": "#F0F3F5",
        "surfaceElevated": "#E4E9ED",
        "onSurface": "#182026",
        "textMuted": "#4E5B66",
        "border": "#69757D",
        "success": "#397253",
        "warning": "#805E22",
        "danger": "#913F42"
    }
}
```

Both `dark` and `light` palettes are required. The selected screen mode decides which palette Gameplay uses. OLED mode uses the dark palette but forces the lowest background surfaces to black.

## Tokens

| Token | Used for |
| --- | --- |
| `primary` | Focus rings, progress, selected controls and the primary action |
| `onPrimary` | Text and icons drawn directly on `primary` |
| `background` | Full-screen application background |
| `onBackground` | Primary content drawn on the application background |
| `surface` | Rails, settings rows and content panels |
| `surfaceElevated` | Focused, selected or layered surfaces |
| `onSurface` | Primary content drawn on surfaces |
| `textMuted` | Secondary labels, hints and metadata |
| `border` | Dividers, progress tracks and quiet outlines |
| `success` | Installed, ready and successful states |
| `warning` | Partial compatibility and warning states |
| `danger` | Errors and destructive actions |

Derived Material colors are generated from these semantic tokens. This keeps existing screens coherent while allowing a theme to change the complete visual character of the application.

## Validation and recovery

- `schemaVersion` must be `1`.
- `name` must contain 1–48 characters.
- Every color must use opaque `#RRGGBB` notation.
- Body text/background and body text/surface contrast must be at least 4.5:1.
- Muted text and primary controls must have at least 3:1 contrast.
- Files larger than 128 KiB are rejected.
- Unknown or missing fields are rejected so spelling mistakes cannot silently produce partial themes.
- Gameplay validates the complete document before saving it. A failed import leaves the active theme unchanged.
- Disabling a custom theme retains the imported document. Removing it requires confirmation and restores the selected built-in profile.

Open **Settings → Interface → Edit or create theme** to change every token in the built-in console editor and preview both palettes. Use **Export theme** to save either the imported theme or Gameplay's safe template for editing in any text editor, then use **Import theme** to validate and apply it.

# Bank Highlight Search

Search your bank without losing sight of it. Press a hotkey (default **Ctrl+Shift+F**),
type a query, and matching items get a blinking highlight — instead of the native
search behavior of hiding everything else. Press **Enter** to jump to the ALL tab and
scroll the matches into view.

## Features

- Bindable hotkey opens a search prompt (only while the bank is open)
- Live highlighting as you type — items blink briefly, then stay outlined
- Feathered pulse style: the outline glow breathes instead of blinking
- Press Enter to keep highlights (until bank close or a new search); Esc cancels them instantly
- Enter switches to the ALL tab and scrolls to where the most matches are visible
- Matches charge/dose variations (Prayer potion(1–4), jewellery charges) — toggleable
- Highlights placeholders too — toggleable
- Configurable highlight style, colors, and blink duration

## Settings

| Setting | Default | Description |
|---|---|---|
| Search hotkey | Ctrl+Shift+F | Opens the highlight search prompt |
| Highlight style | Item outline | Outline, feathered pulse, outline+fill, box, underline, or filled box |
| Highlight color | Orange | Outline color for matches |
| Fill color | Orange, alpha 40 | Translucent fill for the fill styles (alpha 0 disables) |
| Blink duration | 3 s | Blink time before the outline turns solid (0 = blink forever) |
| Pulse min thickness | 1 px | Feathered pulse: thickness at the low point |
| Pulse max thickness | 4 px | Feathered pulse: thickness at the peak |
| Highlight placeholders | On | Match placeholders by name |
| Match variations | On | Highlight dose/charge variants of matches |

This plugin never modifies the native bank search or filters items — it only draws on top.

# Bank Highlight Search

Search your bank without losing sight of it. Press a hotkey (default **Ctrl+Shift+F**),
type a query, and matching items get a blinking highlight — instead of the native
search behavior of hiding everything else. Press **Enter** to jump to the ALL tab and
scroll the matches into view.

## Features

- Bindable hotkey opens a search prompt (only while the bank is open)
- Live highlighting as you type — items blink briefly, then stay outlined
- Highlights persist until you close the bank or run a new search
- Enter switches to the ALL tab and scrolls to the matches
- Matches charge/dose variations (Prayer potion(1–4), jewellery charges) — toggleable
- Highlights placeholders too — toggleable
- Configurable colors, fill opacity, and blink duration

## Settings

| Setting | Default | Description |
|---|---|---|
| Search hotkey | Ctrl+Shift+F | Opens the highlight search prompt |
| Highlight color | Orange | Outline color for matches |
| Fill color | Orange, alpha 40 | Translucent fill (alpha 0 disables) |
| Blink duration | 3 s | Blink time before the outline turns solid (0 = never blink) |
| Highlight placeholders | On | Match placeholders by name |
| Match variations | On | Highlight dose/charge variants of matches |

This plugin never modifies the native bank search or filters items — it only draws on top.

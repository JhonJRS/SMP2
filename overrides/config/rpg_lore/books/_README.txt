=== RPG Lore — Book Definition Format ===

Each .json file in this folder defines one lore book.
The filename (without .json) becomes the book's internal ID
(prefixed with rpg_lore:).

--- Fields ---

title             (string)  Book title displayed in-game
author            (string)  Author name shown on the book. Default: "Unknown"
generation        (int)     0 = Original, 1 = Copy, 2 = Copy of Copy, 3 = Tattered
weight            (number)  Selection weight when multiple books match. Higher = more likely. Default: 1.0
pages             (array)   List of page strings. Each string is a JSON text component
                            (e.g. {"text":"Hello"}) or a plain string that will be auto-wrapped.

--- Appearance (all optional) ---

title_color       (string)  Hex color for the title in the tooltip, e.g. "FFD700" for gold. Default: yellow
author_color      (string)  Hex color for the author in the tooltip. Default: gray
description       (string)  Brief description shown below the author in the tooltip
description_color (string)  Hex color for the description text. Default: aqua
hide_generation   (bool)    If true, the generation label (Original, Copy, etc.) is hidden. Default: false

--- Drop Conditions (all optional) ---

Nested inside a "drop_conditions" object. Omitted fields match everything.

require_player_kill   (bool)    Must the mob be killed by a player? Default: true
base_chance           (number)  Override the global drop chance (0.0 to 1.0) for this book
max_copies_per_player (int)     Max times a player can receive this book. -1 = unlimited. Default: -1
mob_types             (array)   Entity type IDs that can drop this book, e.g. ["minecraft:zombie"]
mob_tags              (array)   Entity type tags, e.g. ["minecraft:undead"]
biomes                (array)   Biome IDs, e.g. ["minecraft:plains"]
biome_tags            (array)   Biome tags, e.g. ["minecraft:is_overworld"]
dimensions            (array)   Dimension IDs, e.g. ["minecraft:overworld"]
min_y                 (int)     Minimum Y coordinate for the kill location
max_y                 (int)     Maximum Y coordinate for the kill location
time                  (string)  "ANY", "DAY_ONLY", or "NIGHT_ONLY"
weather               (string)  "ANY", "CLEAR_ONLY", "RAIN_ONLY", or "THUNDER_ONLY"

--- Example ---

{
  "title": "The Ancient Battle",
  "author": "Unknown Chronicler",
  "generation": 0,
  "weight": 5,
  "title_color": "FFD700",
  "description": "A faded account of a great battle fought long ago.",
  "drop_conditions": {
    "mob_types": ["minecraft:zombie", "minecraft:skeleton"],
    "biome_tags": ["minecraft:is_overworld"],
    "time": "NIGHT_ONLY"
  },
  "pages": [
    "{\"text\":\"Long ago, in these very fields...\"}",
    "{\"text\":\"The battle raged for three days...\"}"
  ]
}

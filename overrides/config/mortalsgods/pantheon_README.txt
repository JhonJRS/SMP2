================================================================
  MORTALS & GODS  -  Building Your Own Pantheon
================================================================

  >>> NOTHING TO SET UP. Out of the box this IS the WARBORN REALMS
  >>> pantheon, art, and lore. Just play. You only need this folder
  >>> if you WANT to build your own gods. If in doubt, leave it be.

This folder controls EVERY god in the mod: their names, lore,
powers, AND their pictures. Out of the box you get the WARBORN
REALMS pantheon (Luminara, Mortanis, Forgrimm, Xerion, Imperius,
Sanguinar, and the two Crown stances). Edit pantheon.json, restart
the game (or run /mortalsgods reload), and YOUR gods replace the
defaults. You cannot break the game by editing here: if you mistype
something, that one god is skipped and a warning is printed to the
log. The rest keep working. Delete pantheon.json to get the Warborn
Realms defaults back.

You write DATA, never code. You pick from a fixed menu of "boon"
and "currency" types and just fill in the numbers.

What's in this folder:
  pantheon.json        <- the gods (edit this)
  pantheon_README.txt  <- this file
  images/              <- drop your own god pictures here (see
                          "CUSTOM GOD PICTURES" near the bottom)

================================================================
  QUICK START
================================================================
* Just want different ART on the existing gods? You don't even
  touch pantheon.json. Put a PNG named after the god into the
  images/ folder: images/luminara.png, images/mortanis.png, etc.
  (ids are listed below). Size 200 x 380 pixels, PNG. Done.
* Want to rename / re-lore / re-power a god, or build a whole new
  pantheon? Edit pantheon.json as described below.

----------------------------------------------------------------
  THE SHAPE OF THE FILE
----------------------------------------------------------------

{
  "groups": [
    {
      "id": "primarch",          <- unique short name, no spaces
      "title": "Primarch Gods",  <- shown above the pair in the UI
      "tab": "gods",             <- ANY bookmark id (see TABS below)
      "demigod": false,          <- true only for living-ruler groups
      "require": { ... },        <- OPTIONAL access gate (see ACCESS GATES)
      "gods": [ ... god objects ... ]
    }
  ]
}

A "group" is a pairing shown together in the worship screen (the
duality). You normally put TWO gods in a group so they face each
other, but you may add a whole new group with your own pair — or a
group with MANY gods; the screen wraps them and scrolls, so nothing
runs off-screen no matter how many groups or gods you add.

"tab" can be ANY id you like, not just "gods"/"demigods". Each
distinct tab id becomes its own bookmark at the top of the worship
screen (see TABS). Groups with the same tab id share a bookmark.

----------------------------------------------------------------
  A GOD
----------------------------------------------------------------

{
  "id": "luminara",            <- unique short name (also the art filename)
  "name": "Luminara",          <- display name
  "lore": "Shown in the info panel.",
  "currency": { ... },         <- what an offering costs (below)
  "boons": [ { ... } ],        <- one or more boons (below)
  "color": "#E8C46E",          <- accent color for this god's UI button
  "art": "luminara.png"        <- OPTIONAL custom picture (see below)
}

The "art" field is optional. Leave it out and the god shows its
built-in card (or images/<id>.png if you dropped one there). Set it
to point at any file in the images/ folder, e.g. "art": "my_sun.png".

The "color" tints the god's button (its border, art backing, and devotion
bar). Within a pair the two gods look best as OPPOSITES: give them
complementary colors. Shortcut: set ONE color on the first god of a pair and
leave the partner's out; the mod auto-fills the partner with the exact
opposite hue (180 degrees around the color wheel).

Optional "verb": the label on the god's choose button in the UI, e.g.
  "verb": "Renounce the Crown"
Set this so a "reject" god doesn't read as "Follow This God". Leave it out
for the default ("Choose" / "Follow This God").

----------------------------------------------------------------
  CURRENCIES  (what you give / how you earn devotion)
----------------------------------------------------------------

Item  -> hand over an item:
  { "type": "item", "item": "minecraft:totem_of_undying", "amount": 1 }
  Point "item" at ANYTHING to invent a new currency, e.g.
  { "type": "item", "item": "minecraft:diamond", "amount": 3 }

  Start the id with # for an item TAG, so a god can accept a whole
  family of items rather than one exact id:
  { "type": "item", "item": "#c:ingots/gold", "amount": 3 }
  { "type": "item", "item": "#minecraft:swords", "amount": 1 }

  Offerings are taken from your main inventory and your off hand.
  Worn armour is never taken: a tag currency like "any gold item"
  should not strip the helmet off your head to pay for itself.

XP    -> spend experience levels:
  { "type": "xp", "levels": 5 }

Kill  -> earned in the field rather than handed over at the altar.
  Either hunt PLAYERS:
  { "type": "kill", "target": "rival_followers" }

    rival_followers  the victim follows the OPPOSING god in this same
                     group (the default, and the only rule that
                     existed before 1.1.0)
    non_believers    the victim follows no god anywhere
    any_player       any player kill counts
    tab:<id>         the victim follows any god on that tab
    group:<id>       the victim follows any god in that group
    god:<id>         the victim follows that exact god

  ...or hunt CREATURES, by exact type or by entity tag:
  { "type": "kill", "entity": "minecraft:zombie" }
  { "type": "kill", "entity": "#minecraft:undead" }

  Player kills are counted ONCE per victim (you cannot farm one
  rival). Mob kills are not deduplicated.

MORE THAN ONE CURRENCY

"currency" may be an ARRAY, with "currencyMode" beside it:

  "currency": [
    { "type": "item", "item": "#c:ingots/gold", "amount": 3 },
    { "type": "xp", "levels": 2 }
  ],
  "currencyMode": "any"      <- "all" (default) or "any"

  all  -> every cost must be paid together (gold AND levels)
  any  -> the first cost you can afford is taken (gold OR levels),
          in the order you list them

FAVOR MULTIPLIER

Any currency may carry "favorMultiplier" to scale the devotion it
grants. 1.0 = unchanged, 1.3 = +30%, 0.5 = half.

  { "type": "kill", "entity": "#minecraft:undead",
    "favorMultiplier": 0.25 }     <- mobs are cheap, so worth less

With "currencyMode": "all" the multipliers of everything paid are
multiplied together; with "any" only the one you actually paid counts.

----------------------------------------------------------------
  BOONS  (what you get, and how it scales)
----------------------------------------------------------------

Every boon uses the same curve:

    strength = cap * (1 - e^(-devotion / tau))

"cap"  = the most you can ever get (the ceiling).
"tau"  = how slow the climb is. Bigger tau = grindier.
         (~63% of cap at devotion=tau, ~95% at devotion=3*tau.)

Pick a "type" from this fixed list and set cap/tau:

  attribute      params.attribute = any vanilla or modded attribute
                 id, cap = the bonus amount. This is the general
                 case, and these three names are just shorthands
                 for it (all still valid, nothing to rewrite):
                   max_health  = attribute minecraft:max_health
                                 (cap 40 = +20 hearts)
                   reach       = attribute
                                 minecraft:block_interaction_range
                                 (set params.attribute to
                                  minecraft:entity_interaction_range
                                  for attack reach instead)
                   mana        = attribute irons_spellbooks:max_mana
                                 (needs Iron's Spells)
  auto_revive    cap = max stored totem-style revives
  durability     cap = bonus fraction on crafted gear (1.0 = +100%)
  war_stat       cap = max potion amplifier; needs params.effect
                 (e.g. minecraft:strength, minecraft:resistance)
  favor_theft    cap = fraction of a victim's devotion stolen per
                 kill (Demi-God war)

REMOVED IN 1.1.0: pehkui_scale. It parsed, printed text in the
worship screen, and never had any effect at all. A god carrying one
is now reported in the log and skipped. The Emperor's scaling is,
and always was, driven by the emperor*ScaleCap values in
mortalsgods-common.toml. Just delete the boon.
  scoreboard     writes the current curve value into a scoreboard
                 objective every second, so datapack functions or
                 KubeJS can react to worship. Needs params.objective.
                 The objective is auto-created; it drops back to 0
                 when you stop following the god. Example:
                   { "type": "scoreboard", "cap": 100, "tau": 8,
                     "params": { "objective": "luminara_favor" } }
                 A function can then test  luminara_favor  matches N.

A god may stack several boons. Example (Forgrimm):

  "boons": [
    { "type": "durability", "cap": 1.0, "tau": 7 },
    { "type": "reach", "cap": 3.0, "tau": 7,
      "params": { "attribute": "minecraft:block_interaction_range" } }
  ]

----------------------------------------------------------------
  TABS  (bookmarks at the top of the worship screen)
----------------------------------------------------------------

Every group sits on a "tab" bookmark. Just set "tab" on a group to
any id and that bookmark appears automatically — great for one tab
per culture (greek, egyptian, norse...). LORE is always the last
bookmark.

You normally don't need anything else. But you CAN declare tabs at
the TOP of the file to control their ORDER, their LABEL, whether
they're EXCLUSIVE, and gate them:

  {
    "tabs": [
      { "id": "greek",   "label": "Greek Pantheon", "exclusive": true },
      { "id": "egyptian","label": "Egyptian Gods",  "exclusive": true },
      { "id": "gods" },
      { "id": "demigods" }
    ],
    "groups": [ ... ]
  }

  id         <- matches the "tab" on your groups
  label      <- text on the bookmark (defaults to a tidy version of id)
  exclusive  <- optional, see ONE-PANTHEON TABS below
  require    <- optional access gate, see ACCESS GATES below

Tabs appear in the order you list them here; any tab a group uses
but you didn't list is added after these, in group order. A tab
with no visible groups (all gated off) is hidden.

----------------------------------------------------------------
  ONE-PANTHEON TABS  ("exclusive": true)
----------------------------------------------------------------

Mark a tab "exclusive": true and a player may worship several gods
inside it, but NOT gods in any OTHER exclusive tab at the same time.
Make each culture its own exclusive tab and a player commits to a
SINGLE pantheon (Greek OR Egyptian, not both), while still following
multiple gods within it.

Nothing is destroyed: trying to worship a second pantheon is simply
refused with a message. To switch, the player runs
  /mortalsgods renounce
which releases their pledges in exclusive tabs so they can pledge
elsewhere. (Non-exclusive tabs like the default gods/demigods are
never affected.)

The wider forms, all open to any player without op:

  /mortalsgods renounce            exclusive tabs only (as above)
  /mortalsgods renounce all        turn from EVERY god, everywhere
  /mortalsgods renounce tab <id>   turn from one named tab

Before 1.1.0 only the first form existed, so on a pantheon with no
exclusive tab there was no way to stop worshipping at all. Operators
can run the wider forms on someone else:

  /mortalsgods renounce all <targets>
  /mortalsgods renounce tab <id> <targets>

which is the tidy way to pair a gate with its cleanup:

  /tag @p remove greek
  /execute as @p run mortalsgods renounce tab greek

Renouncing clears devotion and the offering cooldown. It does NOT
clear which rivals you have already killed, so renouncing and
re-pledging cannot make old victims worth credit a second time.

----------------------------------------------------------------
  ACCESS GATES  ("require": { ... })
----------------------------------------------------------------

Put a "require" block on a TAB, a GROUP, or a single GOD to control
who may see and worship it. It's checked against the player's
scoreboard TAGS (the /tag command) and scoreboard OBJECTIVE scores.
Anything a player fails is hidden from them entirely and can't be
worshipped even by a hacked client. All listed conditions must pass.

  "require": {
    "tag":     "greek",                 <- must have this /tag tag
    "allTags": ["greek", "initiated"],  <- must have EVERY tag
    "anyTags": ["hero", "chosen"],      <- must have AT LEAST ONE
    "noneTags":["cursed"],              <- must have NONE of these
    "scores": [
      { "objective": "faith", "min": 1, "max": 100 },
      { "objective": "rank",  "equals": 3 }
    ]
  }

Every field is optional; use only what you need. A missing "min" or
"max" means that side is unbounded; "equals" is shorthand for
min=max. A player with no score on an objective counts as 0.

Examples:
* Lock a whole culture tab behind a tag the player earns in a quest:
    "tabs": [ { "id": "greek", "require": { "tag": "unlocked_greek" } } ]
* Hide one forbidden god until a scoreboard score is high enough:
    "require": { "scores": [ { "objective": "sins", "min": 10 } ] }

Hand out access from a datapack/function/KubeJS with /tag or
/scoreboard players set, and the worship screen updates the next
time the player opens an altar.

TAKING ACCESS BACK

When a player STOPS passing a gate (you remove the tag, their score
drops), that god's boons are SUSPENDED immediately: the attribute
bonuses, potion effects and scoreboard writes all stop, and they are
told once in chat that the god has turned away. Their pledge and
their devotion are kept, so restoring the tag restores the god at
full standing with no grinding again.

Before 1.1.0 the gate was only checked when DISPLAYING the pantheon
and when choosing, never when applying, so revoking a tag hid the
god and blocked re-pledging while its boons kept applying every
second, forever, with no way to remove them. If you built a pack
around that, note that those buffs now stop.

To take the pledge away as well as the boons, pair the tag removal
with a renounce (see ONE-PANTHEON TABS above).

----------------------------------------------------------------
  PER-GOD CADENCE  (optional)
----------------------------------------------------------------

By default every god shares the worship cadence set in
config/mortalsgods-common.toml. Any god may override it:

  {
    "id": "sanguinar",
    "cooldownTicks": 6000,     <- min ticks between offerings
    "graceTicks": 144000,      <- neglect allowed before decay
    "decayPerPeriod": 0.05,    <- fraction lost per decay period
    ...
  }

Leave a field out to inherit the global value. 20 ticks = 1 second,
24000 ticks = 20 minutes. A jealous war god can demand attention
every few minutes while a patient harvest god forgives a week.

NOTE ON TIME: these are ticks of SERVER UPTIME, read from the world's
game-time counter. They are not world days. Turning off the daylight
cycle does not pause them, /time set cannot move them, and sleeping
through a night does not advance them. That is deliberate, because
game time only ever moves forward, so worship cadence cannot be
rewound or skipped by changing the clock.

----------------------------------------------------------------
  TIPS
----------------------------------------------------------------

* Want a softer climb? Raise tau. Want a stronger god? Raise cap.
* Want a third pair? Copy a group, give it a new "id", swap the
  gods. The UI lays out groups automatically.
* Demi-God groups (tab "demigods", "demigod": true): the FIRST god
  listed is the WORSHIP/support side, the rest are REJECT sides.
  Net favor (support minus reject) scales the crowned ruler via
  Pehkui. Crown a player with /mortalsgods emperor set <name>.
* /mortalsgods reload re-reads this file without a restart.
* Keep a backup of your edited pantheon.json. Deleting the file
  regenerates the defaults.
* Watch the log on startup: it tells you how many gods loaded and
  names anything it had to skip.

----------------------------------------------------------------
  CUSTOM GOD PICTURES  (the images/ folder)
----------------------------------------------------------------

Every god shows a tall picture in the worship screen. You can swap
any of them for your own.

HOW (two ways):
  1. EASY, no JSON: drop a PNG into the images/ folder named after
     the god's id:
         images/luminara.png
         images/mortanis.png   ... etc.
     It replaces that god's picture automatically.
  2. BY NAME: set the god's "art" field in pantheon.json to any
     filename in images/:
         "art": "my_custom_sun.png"
     This is what you use for brand-new gods you invented.

The id-named file (way 1) and a custom "art" name (way 2) both live
in the images/ folder. If both exist, "art" wins.

IMAGE SIZE:
  * 200 x 380 pixels (width x height). This is the exact slot the
    game draws, a tall ~10:19 "tarot card" rectangle.
  * Other sizes still work but are scaled to fit that slot, so an
    image with a very different shape will look squished. Match
    200 x 380 (or any 10:19 multiple, e.g. 400 x 760) for a crisp,
    undistorted result.

IMAGE FORMAT:
  * PNG only. (.jpg / .jpeg / .webp are NOT loaded.)
  * Transparency is supported and encouraged: a transparent
    background lets the card's shape show cleanly over the plate,
    and the mod tints the unpicked partner darker on its own.
  * The mod prints the god's NAME on a strip along the bottom of
    the card, so don't bake a title into your image, leave the
    lower edge clean.

WHERE / WHO SEES IT:
  * Pictures are read from each player's OWN
    config/mortalsgods/images/ folder. On a server, the pantheon
    (names, powers) comes from the server, but the IMAGES are
    local: give every player who wants the custom look a copy of
    the images/ folder. Players without the files just see the
    built-in cards, no errors.
  * Added or renamed an image file? Restart the game (or rejoin)
    to load it. Changing which file a god points at in
    pantheon.json takes effect on /mortalsgods reload.

The eight built-in ids: luminara, mortanis, forgrimm, xerion,
imperius, sanguinar, loyalist, regicide.

----------------------------------------------------------------
  DEMI-GODS  (who can be the living Emperor)
----------------------------------------------------------------

The Demi-Gods tab worships a LIVING ruler (the "Emperor"), whose
body is empowered by the faithful and weakened as the realm turns
on him. WHO that ruler is, is configurable. In the mod's server
config file (config/mortalsgods-common.toml, section [demigod]):

  demigodPlayers = ["Steve", "Alex"]
        Player names who are ALWAYS Demi-Gods. The first one online
        becomes the worshipped Emperor.

  enableDemigodWar = true
        Master switch for the whole Demi-God system.

Or set one by command (an op can do this live):
  /mortalsgods emperor set <player>
  /mortalsgods emperor clear
  /mortalsgods emperor info

Priority: a command-set Emperor wins; otherwise the first online
player from demigodPlayers is used. Warborn Realms ships with the
list EMPTY, so its only Demi-God is the server's Emperor (set by
command, or a future War 'n Nobility hook). Other servers can just
fix their Demi-Gods in the list. The Demi-Gods tab always shows who
the current Emperor is (or that the throne sits empty).

----------------------------------------------------------------
  THE LORE TAB  (optional lore.txt)
----------------------------------------------------------------

The worship screen has a LORE bookmark beside GODS and DEMI-GODS.
By default it shows a short synopsis of the Warborn Realm saga.

Telling your OWN world's story instead? Drop a plain-text file at
config/mortalsgods/lore.txt and it replaces the built-in synopsis.
Format:
    # A Heading
    A paragraph on its own line.
    Another paragraph. Blank lines are ignored.

    # The Next Heading
    ...and so on.

Lines beginning with # are gold section headings; every other
non-empty line is a paragraph. The tab scrolls with the mouse
wheel, so write as much as you like. Like the images, lore.txt is
read from each player's own config folder; restart (or reopen the
screen after a /mortalsgods reload) to pick up edits.
================================================================

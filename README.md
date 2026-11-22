<div align="center">

<img src="src/main/resources/assets/hopperfilter/icon.png" alt="Hopper Filter Setup icon" width="96">

# Hopper Filter Setup

**Set up item-sorter hoppers in one click.**

[![Build](https://img.shields.io/github/actions/workflow/status/SobujRoySunday/hopperfilter/build.yml?branch=main&label=build)](https://github.com/SobujRoySunday/hopperfilter/actions/workflows/build.yml)
[![Latest release](https://img.shields.io/github/v/release/SobujRoySunday/hopperfilter?label=release)](https://github.com/SobujRoySunday/hopperfilter/releases/latest)
[![Minecraft](https://img.shields.io/badge/minecraft-26.1%20%7C%2026.2%20%7C%2026.3-62b47a)](#compatibility)
[![Fabric](https://img.shields.io/badge/loader-Fabric-dbd0b4)](https://fabricmc.net/)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

</div>

Building an item sorter means filling dozens of hoppers with the same pattern: a stack of the item to
sort, then a few renamed filler items. Hopper Filter Setup does that for you. Hold the item you want to
sort, right-click an empty hopper, and the whole filter is placed from your inventory.

## Features

- **One-click filters:** right-click an empty hopper while holding an item and its filter slots are filled.
- **Your own layout:** set up one hopper by hand, sneak + right-click it, and that layout is used from then on.
- **Never takes half:** if you're short on items, you get a message saying what's missing and nothing is taken.
- **Stays out of the way:** hoppers that already hold items behave exactly like vanilla.
- **Creative friendly:** in creative mode the items are created for free (configurable).
- **Server ready:** every player can save their own layout, and the config reloads live.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for your Minecraft version.
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) for the same Minecraft version.
3. Download Hopper Filter Setup from the [latest release](https://github.com/SobujRoySunday/hopperfilter/releases/latest).
   Pick the file whose name ends in your Minecraft version, for example `hopperfilter-1.0.0-mc26.3.jar` for Minecraft 26.3.
4. Put both jars into your `.minecraft/mods` folder and start the game with the Fabric profile.

On a multiplayer server, install the mod (and Fabric API) **on the server**. Installing it on clients is
optional. Without it, a player may briefly see a "ghost" block when right-clicking a hopper while holding a block.

## How to use

### 1. Save your layout (once)

Set up one hopper by hand the way your sorter needs it. For example, put 18 of the item to sort in
slot 1 and one stick renamed "Filter" in each of slots 2 to 5. Then **sneak + right-click the hopper with an empty hand**.

You'll see `Hopper filter layout saved` in the action bar. By default slot 1 is saved as "whatever I'm holding",
so one layout works for every item you sort.

### 2. Fill hoppers

Hold the item you want to sort (say, iron ingots) and **right-click an empty hopper**. Slot 1 gets 18 iron ingots
and slots 2 to 5 get your filler items, all taken from your inventory. Move to the next hopper with gold ingots
in hand, and so on.

| Situation | What happens |
|---|---|
| You have everything the layout needs | The hopper is filled and the items are taken from your inventory |
| You're missing something | The action bar lists what's short, the hopper opens normally and nothing is taken |
| The hopper already contains items | Vanilla behaviour, the hopper just opens |
| Empty hand and the layout uses the held item | Vanilla behaviour, the hopper just opens |
| Creative mode | Items are created for free (see `consumeInCreative`) |

If you haven't saved a layout yet, the default is: held item ×18, then 4 sticks named "Filter".

> [!TIP]
> If right-click filling gets in the way while you're building, set `requireSneakToFill` to `true`.
> You'll then need to sneak to fill a hopper.

## Configuration

The config lives at `config/hopperfilter.json` and is created on first launch. Edits are picked up
live, with no restart needed.

```json
{
  "requireSneakToFill": false,
  "firstSlotIsHeldItem": true,
  "consumeInCreative": false,
  "defaultLayout": {
    "slots": [
      { "item": "$held", "name": "", "count": 18 },
      { "item": "minecraft:stick", "name": "Filter", "count": 1 },
      { "item": "minecraft:stick", "name": "Filter", "count": 1 },
      { "item": "minecraft:stick", "name": "Filter", "count": 1 },
      { "item": "minecraft:stick", "name": "Filter", "count": 1 }
    ]
  },
  "playerLayouts": {}
}
```

| Option | Default | Description |
|---|---|---|
| `requireSneakToFill` | `false` | Only fill hoppers while sneaking. |
| `firstSlotIsHeldItem` | `true` | When saving a layout in game, store slot 1 as "held item" instead of the exact item. Set to `false` for a fixed layout. |
| `consumeInCreative` | `false` | Take items from your inventory even in creative mode. |
| `defaultLayout` | see above | Layout for players who haven't saved their own. |
| `playerLayouts` | `{}` | Layouts saved in game, keyed by player UUID. Managed by the mod, but you can edit it. |

Each slot in a layout has these fields:

- `item`: `"$held"` for whatever is in your main hand, `""` to leave the slot empty, or any item id such as `minecraft:stick`.
- `name`: the custom (anvil) name the item must have. `""` matches any name.
- `count`: how many to place. It's capped at the item's stack size.

## Compatibility

| Minecraft | Download |
|---|---|
| 26.3 | `hopperfilter-<version>-mc26.3.jar` |
| 26.2 | `hopperfilter-<version>-mc26.2.jar` |
| 26.1, 26.1.1, 26.1.2 | `hopperfilter-<version>-mc26.1.jar` |

All builds need Fabric Loader 0.19.3+, Fabric API and Java 25 (bundled with the official launcher).
Versions before 26.1 aren't supported.

## Building from source

You need **JDK 25**.

```sh
./gradlew build              # builds for the default Minecraft version (see gradle.properties)
./gradlew build -Pmc=26.2    # builds for a specific Minecraft line
./gradlew runClient          # starts a development client with the mod loaded
```

On Windows, use `gradlew.bat` instead of `./gradlew`. Jars are written to `build/libs/`.

Each supported Minecraft line has a file in [`versions/`](versions) with its exact Minecraft and Fabric API versions.
CI builds all of them on every push.

## Contributing

Bug reports, ideas and pull requests are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) to get started.

## License

Released under the [MIT License](LICENSE).

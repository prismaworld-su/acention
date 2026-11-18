# Acention

A Paper plugin that makes progression in Minecraft harder.

The first feature is a **locked End portal**. The portal does not let anyone into the End until players throw the required items into it. Once activated, it stays open for a limited time.

## Features

- **Locked End portal.** While the portal is closed, nothing gets into the End: not players, mobs, items or ender pearls. The return portal in the End is not affected.
- **Item activation.** Throw the required items into the portal to open it (one Nether Star by default). Several item types can be required at once. Items can be brought in parts by different players, and progress is saved. Extra or unneeded items are thrown back.
- **Safe for players.** A player who steps into a closed portal is pushed back onto the frame instead of falling into the lava below, and is told what items are still missing.
- **Timed opening.** The portal stays open for 10 minutes by default. While it is open, items and players pass through as in vanilla.
- **Announcements.** When the portal opens, every player gets a chat message and hears the ender dragon growl. A message is also sent when it closes.
- **Status menu.** `/end` shows whether the portal is open, who opened it, when, the time left and the item progress. Admins can open the portal for free or close it early from the menu.
- **Persistent state.** The portal state is kept across server restarts. The timer runs on real time.
- **Fully configurable.** Required items, open duration, sound, date format and all texts ([MiniMessage](https://docs.advntr.dev/minimessage/format.html)).

## Requirements

- [Paper](https://papermc.io/) 26.3
- Java 25

## Installation

1. Download or build `acention-<version>-all.jar`.
2. Put it into the server's `plugins` folder.
3. Restart the server.
4. Adjust `plugins/acention/config.yml` and run `/end reload`.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/end` | Open the End portal status menu | `acention.end` |
| `/end reload` | Reload `config.yml` | `acention.reload` |

## Permissions

| Permission | Description | Default |
|---|---|---|
| `acention.end` | Open the `/end` menu | op |
| `acention.end.manage` | Open and close the portal with the menu buttons (includes `acention.end`) | op |
| `acention.reload` | Reload the configuration | op |
| `acention.portal.bypass` | Pass through a closed portal | nobody |

## Configuration

Main settings in `config.yml`:

```yaml
portal:
  # How long the portal stays open, in seconds (600 = 10 minutes).
  open-duration: 600

  # Items to throw into the portal: MATERIAL: amount.
  # All of them are required. An empty list means only admins can open the portal.
  required-items:
    NETHER_STAR: 1

  # Sound played to every online player when the portal opens.
  open-sound:
    enabled: true
    key: "minecraft:entity.ender_dragon.growl"
    volume: 1.0
    pitch: 1.0

time:
  format: "dd.MM.yyyy HH:mm:ss"
  # Empty means the server time zone, for example "Europe/Moscow".
  zone: ""
```

Material names are listed in the [Paper Javadocs](https://jd.papermc.io/paper/org/bukkit/Material.html), sound keys on the [Minecraft Wiki](https://minecraft.wiki/w/Sounds.json).

The `messages` and `gui` sections hold every text shown to players. The default texts are in Russian. Setting a message to `""` turns it off.

Invalid values do not break the plugin: a warning is logged and the default value is used.

## Data

The portal state (last opening and collected items) is stored in `plugins/acention/data.yml`. If the file gets corrupted, it is renamed to `data.yml.broken` and the portal starts closed.

## Building

The project uses Kotlin and Gradle with the Shadow plugin:

```powershell
gradle build
```

The plugin jar is written to `build/libs/acention-<version>-all.jar`. To start a local test server with the plugin, run `gradle runServer`.

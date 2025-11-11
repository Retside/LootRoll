# LootRoll

A Minecraft plugin that implements a fair rolling system for loot distribution in parties. When mobs die, players in a party can roll for dropped items.

## Features

- Fair roll system for loot distribution
- Party integration (Parties and MMOCore)
- Support for Vanilla items, MMOItems, and MythicMobs items
- Economy support with automatic party splitting
- Event API for plugin integration

## Requirements

- Minecraft 1.19.2+
- [PacketEvents](https://github.com/retrooper/packetevents) (required)
- Party system: [Parties](https://www.spigotmc.org/resources/parties-an-advanced-parties-manager.3709/) or [MMOCore](https://www.spigotmc.org/resources/mmocore.72310/)

Optional:

- [MythicMobs](https://www.spigotmc.org/resources/mythicmobs.5702/)
- [MMOItems](https://www.spigotmc.org/resources/mmoitems.39267/)
- [Vault](https://www.spigotmc.org/resources/vault.34315/)

## Quick Start

1. Install PacketEvents and a party system
2. Place LootRoll in your `plugins/` folder
3. Configure `plugins/LootRoll/config.yml`
4. Create loot configurations in `plugins/LootRoll/drops/`
5. Run `/lootroll reload`

## Documentation

Full documentation: [https://ney.gitbook.io/docs/lootroll](https://ney.gitbook.io/docs/lootroll)

## Commands

- `/roll [item]` - Roll for an item (priority)
- `/greed [item]` - Greed roll (lower priority)
- `/pass [item]` - Pass on an item
- `/lootroll reload` - Reload configuration
- `/lootroll info` - Show plugin info
- `/lootroll list` - List configured mobs

## Example Configuration

```yaml
zombie:
  min-drops: 1
  max-drops: 2
  loot:
    - vanilla{item=rotten_flesh} 2-5 .9
    - vanilla{item=iron_sword} 1 .1
    - money{split=true} 50-100 .5
```

## Building

```bash
./gradlew build
```

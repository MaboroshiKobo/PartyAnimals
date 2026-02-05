<div align="center">
  <img src="https://raw.githubusercontent.com/MaboroshiKobo/branding/refs/heads/main/projects/partyanimals/partyanimals.avif" width="180" alt="PartyAnimals Logo">
  <h1>PartyAnimals</h1>
  <p>A modular player engagement plugin for Paper servers designed to reward your community through voting and interactive pinatas.</p>
</div>

### Features

PartyAnimals strives to increase player retention with a focus on these core functions:

* It spawns interactive **pinata** entities that players can hit to receive configurable rewards. Supports standard vanilla mobs or custom models via **ModelEngine** or **BetterModel**!
* You can configure their behaviors, allowing pinatas to roam, flee from players, or defend themselves with **reflexes** like *Shockwave* (knockback), *Blink* (teleportation), *Morph* (shapeshifting), and more.
* It features a complete voting module that integrates with **NuVotifier** to track votes, handle offline queuing, and manage community goals.
* It includes extensive customization for rewards, messages, and interaction rules, with optional **PlaceholderAPI** support.

### Prerequisites

To use this plugin, your server must be running **Paper** on `1.21` or higher, and Java 21 or higher.

#### Dependencies

These dependencies are optional but highly recommended to unlock full functionality:

* [NuVotifier](https://github.com/NuVotifier/NuVotifier) (Required for voting features)
* [PlaceholderAPI](https://placeholderapi.com/)
* [ModelEngine](https://github.com/Ticxo/ModelEngine) / [BetterModel](https://github.com/Toxicity188/BetterModel) (For custom entity models)

### Documentation & Support

For a complete guide on features, commands, and configuration, please visit our [wiki](https://docs.maboroshi.org/). If you have questions or need to report a bug, join our [Discord server](https://discord.maboroshi.org).

### Statistics

This plugin utilizes [bStats](https://bstats.org/plugin/bukkit/PartyAnimals/28389) to collect anonymous usage metrics.

![bStats Metrics](https://bstats.org/signatures/bukkit/PartyAnimals.svg)

## Building

If you wish to build the project from source, ensure you have a Java 21 environment configured.

```bash
./gradlew build
```

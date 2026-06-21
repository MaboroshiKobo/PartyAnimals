<div align="center">
  <img src="https://raw.githubusercontent.com/MaboroshiKobo/branding/refs/heads/main/projects/partyanimals/partyanimals.avif" width="180" alt="PartyAnimals Logo" />
  <h1>PartyAnimals</h1>
  <p>A fun pinata plugin for custom server events and automated vote rewards. Supports custom entity models and unique escape mechanisms. The superior alternative to PinataParty.</p>

  <p>
    <img alt="paper" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/paper_vector.svg">
    <img alt="purpur" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/purpur_vector.svg">
    <img alt="spigot" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/unsupported/spigot_vector.svg">
  </p>

  <p>
    <a href="https://github.com/MaboroshiKobo/PartyAnimals"><img alt="github" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/github_vector.svg"></a>
    <a href="https://hangar.papermc.io/Maboroshi/PartyAnimals"><img alt="hangar" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/hangar_vector.svg"></a>
    <a href="https://modrinth.com/plugin/partyanimals"><img alt="modrinth" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg"></a>
  </p>

  <p>
    <a href="https://docs.maboroshi.org"><img alt="generic" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/documentation/generic_vector.svg"></a>
    <a href="https://discord.maboroshi.org"><img alt="discord-singular" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/social/discord-singular_vector.svg"></a>
  </p>
</div>

### Features

* Spawn pinatas that players can track down and smack for loot. It works with standard vanilla mobs right out of the box, or you can drop in custom 3D models using ModelEngine or BetterModel.
* They don't just sit there like target dummies; you can configure them to wander around, run away from players, or defend themselves with 'abilities' like Shockwave knockbacks, Blink teleports, shapeshifting Morphs and a couple more!
* There is a built-in voting module that utilizes NuVotifier. It handles offline reward queuing seamlessly and lets you set up global community goals for the whole server.
* You get total control over reward tables, chat messages, and interaction rules, with full MiniMessage and PlaceholderAPI support to tie it all into your server's look.

### Prerequisites

This plugin is designed and officially tested for **Paper** `26.1`+ using **Java 25**. While it might technically run on slightly older Minecraft or Java versions, those aren't officially supported; so if something breaks, you're on your own!

#### Compatibility

PartyAnimals supports integration with the following plugins to enhance functionality:

* [NuVotifier](https://github.com/NuVotifier/NuVotifier) (Required for voting features)
* [ModelEngine](https://github.com/Ticxo/ModelEngine) / [BetterModel](https://github.com/Toxicity188/BetterModel) (Custom entity models)
* [PlaceholderAPI](https://placeholderapi.com/)

### Documentation & Support

For a complete guide on features, commands, and configuration, please visit our [wiki](https://docs.maboroshi.org). If you have questions or need to report a bug, join our [Discord server](https://discord.maboroshi.org).

### Statistics

This plugin utilizes [bStats](https://bstats.org/plugin/bukkit/PartyAnimals/28389) to collect anonymous usage metrics.

![bStats Metrics](https://bstats.org/signatures/bukkit/PartyAnimals.svg)

## Building

If you wish to build the project from source, ensure you have a Java 25 environment configured.

```bash
./gradlew build
```

[![PartyAnimals Banner](https://raw.githubusercontent.com/MaboroshiKobo/branding/refs/heads/main/projects/partyanimals/banners/partyanimals_2048.png)](https://docs.maboroshi.org/projects/partyanimals)

<div align="center">
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
    <a href="https://docs.maboroshi.org/projects/partyanimals"><img alt="generic" height="56" src="https://raw.githubusercontent.com/MaboroshiKobo/branding/refs/heads/main/socials/128x/domain_icon_bg.png"></a>
    <a href="https://discord.maboroshi.org"><img alt="discord-singular" height="56" src="https://raw.githubusercontent.com/MaboroshiKobo/branding/refs/heads/main/socials/128x/discord_icon_bg.png"></a>
  </p>
</div>

## Custom pinata events, unique abilities, and community vote goals

PartyAnimals is a feature-packed event and rewards plugin that lets you create highly interactive pinata events on your server. Spawn pinatas that run away, defend themselves with custom abilities, and drop customizable rewards when hit, all fully integrated with your voting system to keep players active and engaged.

## Features

* Spawn pinatas using standard vanilla mobs or custom 3D models via ModelEngine and BetterModel.
* Give pinatas dynamic behaviors like sprinting away, teleporting, morphing size, or blasting players back with shockwaves.
* Set up daily voting milestones, reminder alerts, and global community goals using NuVotifier.
* Fine-tune custom hit cooldowns, health scaling based on player count, and custom boss bars.
* Easily customize reward tables, custom particle effects, and sound effects with MiniMessage and PlaceholderAPI support.

## Prerequisites

PartyAnimals is compatible with the following plugins:

* [NuVotifier](https://github.com/NuVotifier/NuVotifier) (Required for voting features)
* [ModelEngine](https://github.com/Ticxo/ModelEngine) / [BetterModel](https://github.com/Toxicity188/BetterModel) (Optional for custom models)
* [PlaceholderAPI](https://placeholderapi.com/) (Optional)

## Documentation & Support

For configurations, commands, and rewards, check out our [wiki](https://docs.maboroshi.org/projects/partyanimals). For bugs, questions, or updates, visit our [Discord server](https://discord.maboroshi.org) or open a [GitHub Issue](https://github.com/MaboroshiKobo/PartyAnimals/issues).

## Statistics

This plugin utilizes [bStats](https://bstats.org/plugin/bukkit/PartyAnimals/28389) to collect anonymous usage metrics.

![bStats Metrics](https://bstats.org/signatures/bukkit/PartyAnimals.svg)

## Building

To build the project from source, ensure you have a Java 25 environment configured.

```bash
./gradlew build
```

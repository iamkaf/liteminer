# Liteminer

[![Amber](https://img.shields.io/badge/Amber-iamkaf?style=for-the-badge&label=Requires&color=%23ebb134)](https://modrinth.com/mod/amber)
[![Issues](https://img.shields.io/github/issues/iamkaf/mod-issues?style=for-the-badge&color=%23eee)](https://github.com/iamkaf/mod-issues)
[![Discord](https://img.shields.io/discord/1207469438719492176?style=for-the-badge&logo=discord&label=DISCORD&color=%235865F2)](https://discord.gg/HV5WgTksaB)
[![KoFi](https://img.shields.io/badge/KoFi-iamkaf?style=for-the-badge&logo=kofi&logoColor=%2330d1e3&label=Support%20Me&color=%2330d1e3)](https://ko-fi.com/iamkaffe)

A veinmining mod for Fabric and NeoForge available on CurseForge and Modrinth.

## Getting started

1. Clone this repository.
2. Run `python scripts/moddy.py setup` and answer the prompts. The script will
   ask for your base package, mod id, name, author and initial version then
   update packages, class names and identifiers accordingly, and insert the
   version into `changelog.md`.
3. When bumping to a new Minecraft version, run `python scripts/moddy.py set-minecraft-version <version>` to pull matching dependency versions.
4. Replace the placeholder code in `TemplateMod` with your own logic.
5. Run the Gradle `build` task to produce jars for each loader.

## Directory layout

- `common/` contains code shared between all loaders.
- `fabric/`, `forge/` and `neoforge/` contain loader specific entry points and build logic.
- `buildSrc/` holds the Gradle scripts that wire everything together.

Feel free to expand upon this structure to suit the needs of your own mods.

## Adding a new service

Add a new platform service with `python scripts/moddy.py add-service <ServiceName>`.

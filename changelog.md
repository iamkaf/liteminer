# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 2.0.0

### Fixed
- Vein mining crops now distinguishes **grown** vs **growing** crops, so harvesting mature crops won't break nearby immature crops.
- **NeoForge:** Experience orbs now drop from all blocks when vein mining, not just the first block.
- **Block mining statistics** now track all blocks broken during vein mining.

### Added
- New config option: `distinguish_grown_crops` (default: enabled) to toggle the grown-vs-growing crop selection behavior.
- New config option: `match_deepslate_ore_variants` (default: enabled) to treat `<name>_ore` and `deepslate_<name>_ore` as the same block for vein mining.
- New client config options to customize highlight line colors (foreground + see-through) using the 16 Minecraft wool colors.
- New client config option: `show_highlights` (default: enabled) to completely disable block highlights if desired.

### Changed
- Improved performance of block highlights, especially when using high block break limits

## 1.5.0

### Added
- Ported to Minecraft **1.21.11**.

## 1.4.1

### Fixed
- Fixed a startup crash on Fabric.

## 1.4.0

### Added
- Ported to Minecraft **1.21.10**.

### Fixed
- Fixed a bug that would prevent you from changing shapes on NeoForge.

### Changed
- Note: remember to update **Amber**.

## 1.3.0

### Added
- Ported to Minecraft **1.21.9**.

### Fixed
- Reduced lines squiggliness.

## 1.2.1

### Added
- Ported to Minecraft **1.21.7** and **1.21.8**.

## 1.2.0

### Added
- Ported to Minecraft **1.21.6**.

## 1.1.0

### Added
- Ported to Minecraft **1.21.5**.

## 1.0.0

### Added
- First stable release of Liteminer.
- Added tags for blacklisting blocks and tools:
  - Item tags:
    - `liteminer:excluded_tools`
    - `liteminer:included_tools`
  - Block tags:
    - `liteminer:excluded_blocks`
    - `liteminer:block_whitelist`
  - Compatibility note: tags are compatible with FTB Ultimine tags.

## 0.5.2-beta.9

### Fixed
- Added a missing mod to the dependency list.

## 0.5.1-beta.8

### Fixed
- Fixed a mixin failing to apply.

## 0.5.0-beta.7

### Added
- Ported to Minecraft **1.21.4**.

## 0.4.0-beta.6

### Added
- Ported to Fabric **1.20.1**.
- Ported to Forge **1.20.1**.
- Slightly changed the outlines.

### Changed
- Added description for `require_correct_tool_enabled` config option.

## 0.3.2-beta.5

### Added
- Added a HUD scale in the client config.

### Fixed
- Fixed the text in the HUD when selecting 1 block (singular vs plural).
- Stopped the hotbar from scrolling when changing shapes on NeoForge.

## 0.3.1-beta.4

### Fixed
- Liteminer no longer breaks bedrock. ([#3](https://github.com/iamkaf/mod-issues/issues/3))
- Ice now melts correctly.
- Fixed a bug where player heads didn't retain their skin.
- Added missing Show HUD translation.

## 0.3.0-hotfix-beta.3

### Fixed
- Added a missing translation.

## 0.3.0-beta.2

### Added
- Added a client config section with **Key Mode** and **Show HUD** options.
- Added shapes feature: Shapeless, Small Tunnel, 3x3, Staircase Up, Staircase Down.
- Added a HUD to show selected blocks count and current mining shape.
- Added feature to right-click blocks with tools (stripping, pathing, hoeing).

### Fixed
- Fixed a bug where sometimes air would be highlighted.
- Player heads will not be mined since their textures are not retained.

## 0.2.2-beta.1

### Fixed
- Resolved a server-side crash.

## 0.2.1-beta.1

### Fixed
- The mod wasn't loading in 1.21 even though it was compatible.

## 0.2.0-beta.1

### Added
- Added a configuration file.
- Added config for key mode (HOLD/TOGGLE).
- Added config for preventing tool breaking.
- Added config for block break limit.
- Added config for increased harvest time.
- Added config for food exhaustion.

## 0.1.0-alpha.2

### Added
- Initial release.

## Types of changes

- `Added` for new features.
- `Changed` for changes in existing functionality.
- `Deprecated` for soon-to-be removed features.
- `Removed` for now removed features.
- `Fixed` for any bug fixes.
- `Security` in case of vulnerabilities.

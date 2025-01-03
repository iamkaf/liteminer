# Liteminer Changelog

See the full changelog at https://github.com/iamkaf/liteminer/commits/main

## 1.0.0

The first stable release of Liteminer!

I've completed all the features I wanted to add to the mod when I started the project, and I'm happy with how it turned out. The next steps are fixing bugs that might come up and adding new features that I think of or that are requested by the community starting with the Forge port.

- feat: added tags for blacklisting blocks and tools
    - Item Tags:
        - `liteminer:excluded_tools` - items in this tag can't be used for litemining
        - `liteminer:included_tools` - if `require_correct_tool_enabled` is true in common config, by default only "tool" items can be used (tiered items with durability); this can be used to allow extra items
    - Block Tags
        - `liteminer:excluded_blocks` - blocks in this tag may never be litemined
        - `liteminer:block_whitelist` - if this tag is non-empty, then _only_ blocks in this tag may be litemined
    - Note: these tags are compatible with the FTB Ultimine tags, so you can use the same tags for both mods if you already have a setup you like.

## 0.5.2-beta.9

- fix: added a missing mod to the dependency list

## 0.5.1-beta.8

- fix: fixed a mixin failing to apply

## 0.5.0-beta.7

- feat: ported to minecraft 1.21.4

## 1.0.0

The first stable release of Liteminer!

I've completed all the features I wanted to add to the mod when I started the project, and I'm happy with how it turned out. The next steps are fixing bugs that might come up and adding new features that I think of or that are requested by the community starting with the Forge port.

- feat: added tags for blacklisting blocks and tools
    - Item Tags:
        - `liteminer:excluded_tools` - items in this tag can't be used for litemining
        - `liteminer:included_tools` - if `require_correct_tool_enabled` is true in common config, by default only "tool" items can be used (tiered items with durability); this can be used to allow extra items
    - Block Tags
        - `liteminer:excluded_blocks` - blocks in this tag may never be litemined
        - `liteminer:block_whitelist` - if this tag is non-empty, then _only_ blocks in this tag may be litemined
    - Note: these tags are compatible with the FTB Ultimine tags, so you can use the same tags for both mods if you already have a setup you like.

## 0.4.0-beta.6

- feat: ported to fabric 1.20.1
- feat: ported to forge 1.20.1
- feat: slightly changed the outlines
- chore: add description for require_correct_tool_enabled config option

## 0.3.2-beta.5

- feat: added a hud scale in the client config
- fix: fixed the text in the hud when selecting 1 block, it is no longer plural
- fix: stopped the hotbar from scrolling when changing shapes on NeoForge

## 0.3.1-beta.4

- fix: liteminer no longer breaks bedrock 😂 [#3](https://github.com/iamkaf/mod-issues/issues/3)
- fix: ice now melts correctly
- fix: fixed a bug where player heads didn't retain their skin (and quite possibly other block entity related bugs)
- fix: added missing Show HUD translation

## 0.3.0-hotfix-beta.3

- fix: added a missing translation

## 0.3.0-beta.2

- feat: added a client config section with __Key Mode__ and __Show HUD__ options.
- feat: added shapes feature, the current modes are: Shapeless, Small Tunnel, 3x3, Staircase Up, Staircase Down.
- feat: added a HUD to show how many blocks you're breaking and in what shape.
- feat: added feature to right-click the blocks with your tools (useful for stripping, pathing and hoeing).
- fix: fixed a bug where sometimes air would be highlighted
- fix: player heads will not be mined since their textures are not retained (might be fixed later)

![Mining Shapes](https://raw.githubusercontent.com/iamkaf/modresources/refs/heads/main/pages/liteminer/screenshot1.png)

![Mining Shapes](https://raw.githubusercontent.com/iamkaf/modresources/refs/heads/main/pages/liteminer/screenshot2.png)

![Mining Shapes](https://raw.githubusercontent.com/iamkaf/modresources/refs/heads/main/pages/liteminer/screenshot3.png)

![Mining Shapes](https://raw.githubusercontent.com/iamkaf/modresources/refs/heads/main/pages/liteminer/screenshot4.png)

## 0.2.2-beta.1

- fix: resolved a server side crash

## 0.2.1-beta.1

- fix: the mod wasn't loading in 1.21 even though it was compatible

## 0.2.0-beta.1

- feat: added a configuration file.
- feat: added a config for key mode (HOLD/TOGGLE).
- feat: added a config for preventing tool breaking.
- feat: added a config for the block break limit.
- feat: added a config for increased harvest time.
- feat: added a config for food exhaustion.

## 0.1.0-alpha.2

- initial release

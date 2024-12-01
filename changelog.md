# Liteminer Changelog

See the full changelog at https://github.com/iamkaf/liteminer/commits/main

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

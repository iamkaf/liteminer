# Liteminer

## Porting Notes

- `26.1` Gradle wiring changed and should be upstreamed back into the multiloader template stack if it proves stable:
- `forgeconfigapiport-fabric` for `26.1` must resolve to `fuzs.forgeconfigapiport:forgeconfigapiport-fabric`, not the Modrinth compatibility jar.
- `26.1` catalog needs `amber-neoforge` restored so `libs.amber.neoforge` resolves for NeoForge modules.
- `26.1` catalog also needs `amber-forge` and `forgeconfigapiport-forge` restored for Forge modules.
- `26.1` version ports must explicitly opt back into `neoforge` in `project.enabled-loaders` before the loader can even be built.
- Amber `26.1` also had to opt back into `forge` in `project.enabled-loaders` before Forge could be exercised.
- The `26.1` line is now following the ForgeGradle 7 path from `MDKExamples`, using the current stable plugin-portal release (`7.0.16` during this port) with the normal Gradle 9.4 wrapper.
- On the FG7 path, Forge dependencies should be declared as `implementation minecraft.dependency("net.minecraftforge:forge:<mc>-<forge>")`, not through the old `minecraft` dependency bucket.
- On the FG7 path, modern Forge runs use `workingDir` plus `runs.register(...)`, and project repositories should include `minecraft.mavenizer(it)`, `fg.forgeMaven`, and `fg.minecraftLibsMaven`.
- For FG7 plugin resolution, match `MDKExamples`: `gradlePluginPortal()` first, then Forge Maven, then `mavenLocal()`. Repository order appears to matter.
- When `settings.gradle` is made loader-conditional, keep Fabric's plugin repository (`https://maven.fabricmc.net/`) in `pluginManagement.repositories`; otherwise disabling Forge can still leave Fabric unresolvable.
- For `26.1`, keep the FG7 port flexible around `minecraft.mappings(...)`; if explicit official mappings drive mavenizer into the broken `mcp_config` path, prefer the loader defaults until Forge’s 26.1 toolchain settles.
- Amber `26.1` has Forge disabled again for now; Fabric and NeoForge remain the supported working loaders while the Forge 26.1 toolchain settles.
- `settings.gradle` plugin declarations may still need to be conditional on `project.enabled-loaders`; otherwise Forge runs can try to resolve unrelated loader plugins even when the loader is disabled for the invocation.
- Root `build.gradle` does not need to predeclare every loader plugin with `apply false`; on mixed Gradle 8/9 toolchains, those declarations can force resolution of incompatible loader plugins even when the loader is disabled for the invocation.
- On Gradle 9, the old Sponge Mixin Gradle plugin can fail before configuration with `org/gradle/util/VersionNumber`; for `26.1` Forge ports, keep the mixins themselves but avoid depending on that Gradle plugin layer if the build already carries manifest/AP wiring.
- On ForgeGradle 7, old per-project settings like `minecraft.reobf = false` no longer exist on the modern `minecraft` extension and should be removed from `26.1` Forge builds.
- Fabric loader projects that compile shared common sources may still need explicit compile/runtime wiring for shared loader libs like Amber until the convention plugin bridges those dependencies automatically.

A veinmining mod for Minecraft.

![License](https://img.shields.io/badge/license-MIT-blue.svg)

## ⛏️ About

Liteminer adds configurable vein mining with multiple mining shapes, a HUD, and cross-loader support.
Built using a multi-loader architecture supporting Fabric, Forge (scaffolded), and NeoForge.

## 📦 Features

- Vein mining with multiple shapes (Shapeless, Tunnel, 3×3, Staircase Up/Down)
- Configurable behavior (block limit, tool checks, exhaustion, etc.)
- HUD + keybind workflow
- Tag-based block/tool allow/deny lists (compatible with FTB Ultimine tags)

## 🗂️ Monorepo Structure

This repository contains all Minecraft versions of Liteminer:

```
liteminer/
├── 1.21.11/          # Minecraft 1.21.11 (mod-template format)
│   ├── common/       # Shared code across loaders
│   ├── fabric/       # Fabric-specific implementation
│   ├── forge/        # Forge scaffold (not enabled in settings yet)
│   └── neoforge/     # NeoForge-specific implementation
└── README.md         # This file
```

## 🚀 Supported Versions

- 1.21.11 — ✅ Active (`1.21.11/`)

## 🛠️ Building

Use `just` from the repo root as the command runner.

```bash
# Build all loaders for a specific version
just build 1.21.11

# Build a specific task in a specific version
just run 1.21.11 :fabric:build
just run 1.21.11 :neoforge:build

# Run the game for development
just run 1.21.11 fabric:runClient
just run 1.21.11 neoforge:runClient

# Run tests
just test 1.21.11
```

Built jars will be in `<version>/<loader>/build/libs/`

## 💻 Development

### Prerequisites

- Java 21 or higher
- Git
- just (install: `https://github.com/casey/just`)

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/iamkaf/liteminer.git
   cd liteminer
   ```

2. Open the specific version directory in your IDE:
   ```bash
   # Open 1.21.11 in IntelliJ IDEA, for example
   idea 1.21.11
   ```

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🔗 Links

- **CurseForge**: https://www.curseforge.com/minecraft/mc-mods/liteminer
- **Modrinth**: https://modrinth.com/mod/liteminer
- **Issues**: https://github.com/iamkaf/mod-issues/issues

## 👤 Author

**iamkaf**

- GitHub: [@iamkaf](https://github.com/iamkaf)

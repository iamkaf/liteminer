# Liteminer

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

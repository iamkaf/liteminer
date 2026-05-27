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
├── 1.20.1/           # Minecraft 1.20.1
├── 1.21.1/           # Minecraft 1.21.1
├── 1.21.11/          # Minecraft 1.21.11
├── 26.1/             # Minecraft 26.1
├── 26.1.2/             # Minecraft 26.1.2
│   ├── common/       # Shared code across loaders
│   ├── fabric/       # Fabric-specific implementation
│   ├── forge/        # Forge scaffold (not enabled in settings yet)
│   └── neoforge/     # NeoForge-specific implementation
└── README.md         # This file
```

## 🚀 Supported Versions

- 26.1.2 - ✅ Active (`26.1.2/`)
- 26.1 - ✅ Active (`26.1/`)
- 1.21.11 — ✅ Active (`1.21.11/`)
- 1.21.1 — 🤔 Maintenance, pending refactor (`1.21.1/`)
- 1.20.1 — 🤔 Maintenance, pending refactor (`1.20.1/`)

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

## 🧩 Addon API

Liteminer exposes a public addon API under `com.iamkaf.liteminer.api` on the active `26.1.2` line.
The API is intended for mods that need to inspect player state, register custom mining shapes, react to
veinmine operations, or adjust the client HUD.

### Reading Liteminer State

Use `LiteminerApi` for server-side player state:

```java
import com.iamkaf.liteminer.api.LiteminerApi;

boolean active = LiteminerApi.isVeinmining(player);
int shapeIndex = LiteminerApi.getSelectedShapeIndex(player);
var selectedShape = LiteminerApi.getSelectedShape(player);
int blockLimit = LiteminerApi.getBlockLimit();
```

You can also set a player's selected shape by id:

```java
import net.minecraft.resources.Identifier;

LiteminerApi.setSelectedShape(player, Identifier.fromNamespaceAndPath("liteminer", "three_by_three"));
```

### Veinmine Events

Server-side lifecycle events live in `com.iamkaf.liteminer.api.event.LiteminerEvents`.

Available events:

- `BEFORE_VEINMINE`: fired before Liteminer processes secondary blocks. Return anything other than `InteractionResult.PASS` to cancel the operation.
- `ALLOW_BLOCK`: fired for each secondary block candidate. Return anything other than `InteractionResult.PASS` to skip that block.
- `AFTER_VEINMINE`: fired after Liteminer finishes processing secondary blocks.

Example:

```java
import com.iamkaf.liteminer.api.event.LiteminerEvents;
import net.minecraft.world.InteractionResult;

LiteminerEvents.ALLOW_BLOCK.register(context -> {
    if (isProtected(context.level(), context.pos(), context.player())) {
        return InteractionResult.FAIL;
    }

    return InteractionResult.PASS;
});
```

Each event context includes the operation type (`BREAK` or `INTERACT`), level, player, origin block,
tool, selected shape, shape index, and block limit. Per-block contexts also include the candidate block.
The after-event context includes the full candidate list, processed blocks, and skipped blocks.

### Custom Shapes

Register custom shapes through `LiteminerShapes`:

```java
import com.iamkaf.liteminer.api.shape.LiteminerShapes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

LiteminerShapes.register(
        Identifier.fromNamespaceAndPath("examplemod", "vertical_column"),
        Component.literal("Vertical Column"),
        (level, player, origin) -> {
            var blocks = new java.util.HashSet<net.minecraft.core.BlockPos>();
            blocks.add(origin);
            blocks.add(origin.above());
            blocks.add(origin.below());
            return blocks;
        }
);
```

Registered shapes participate in Liteminer's shape cycling, HUD text, block highlighting, and server-side
veinmine logic. Shape walkers should usually include the origin in the returned set; Liteminer skips the
origin when processing secondary blocks.

Built-in shape ids are exposed on `LiteminerShapes`:

- `SHAPELESS`
- `SMALL_TUNNEL`
- `STAIRCASE_UP`
- `STAIRCASE_DOWN`
- `THREE_BY_THREE`

### Client HUD Event

Client-side presentation events live in `com.iamkaf.liteminer.api.event.LiteminerClientEvents`.

Use `MODIFY_HUD` to change or hide Liteminer's default HUD:

```java
import com.iamkaf.liteminer.api.event.LiteminerClientEvents;
import net.minecraft.network.chat.Component;

LiteminerClientEvents.MODIFY_HUD.register(context -> {
    context.lines().add(Component.literal("Addon active"));
    context.setTextColor(0xFF55FF55);
});
```

`LiteminerHudContext` exposes the selected block count, selected shape, mutable HUD lines, visibility,
text color, line height, and screen-center offsets.

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🔗 Links

- **CurseForge**: https://www.curseforge.com/minecraft/mc-mods/liteminer
- **Modrinth**: https://modrinth.com/mod/liteminer
- **Issues**: https://github.com/iamkaf/liteminer/issues

## 👤 Author

**iamkaf**

- GitHub: [@iamkaf](https://github.com/iamkaf)

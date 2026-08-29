# World Slice

**A configurable, block-accurate "world slice" mod for Minecraft.**

World Slice reinterprets the Minecraft world as a narrow, vertical cross-section: the X axis becomes a configurable depth, while the Z axis remains open-ended in both directions. The full vanilla build height — terrain, caves, ores, structures, biomes and even the End dragon fight — is preserved inside the slice.

<div align="center">

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=for-the-badge)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.249-f16436?style=for-the-badge)](https://neoforged.net/)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)](LICENSE)

**English** · [简体中文](README.zh-CN.md)

</div>

---

## Table of Contents

1. [Overview](#overview)
2. [Features](#features)
3. [Concept](#concept)
4. [Requirements](#requirements)
5. [Installation](#installation)
6. [Getting Started](#getting-started)
7. [Configuration](#configuration)
8. [World Boundary Rules](#world-boundary-rules)
9. [Dimension-Specific Behaviour](#dimension-specific-behaviour)
10. [Portal Behaviour](#portal-behaviour)
11. [Implementation Notes](#implementation-notes)
12. [Known Limitations](#known-limitations)
13. [Building from Source](#building-from-source)
14. [Credits](#credits)
15. [License](#license)

---

## Overview

World Slice wraps the vanilla Overworld, Nether and End chunk generators so that only a single, contiguous band of X columns is ever generated. The result is a world that behaves like a 2D slice viewed side-on — a Terraria-like perspective — while remaining fully faithful to vanilla Minecraft generation within the playable band.

Key properties:

- **One shared thickness** across all three vanilla dimensions.
- **Vertical fidelity** — the normal build height, terrain shape and cave systems are unchanged.
- **Infinite Z** — the world extends indefinitely in the forward/backward direction.
- **No world deletion** — existing chunk and region files are never bulk-deleted or rewritten.

## Features

- **Configurable world width** on the X axis, from 1 to 4096 blocks (default 16).
- **All three vanilla dimensions** are sliced and share one thickness.
- **Vanilla generation** — biomes, terrain, caves, aquifers, ores, vegetation, structures and mob spawning remain intact inside the slice.
- **Virtual fluid boundary** that stops water, lava and other `FlowingFluid` implementations from escaping the slice.
- **Invisible entity boundary** that blocks players and ordinary living entities while letting the Ender Dragon and non-living entities pass.
- **Terraria-style side camera** with smooth entity interpolation, toggled with `V`.
- **NeoForge configuration screen** accessible from the mod list and in-game.

## Concept

World Slice redefines the meaning of the two horizontal axes:

| Axis | Role |
|:----:|------|
| **X** | Slice depth — bounded by the configured world thickness. |
| **Z** | Primary travel direction — unbounded in both directions. |
| **Y** | Height — fully preserved, from bedrock to build limit. |

The playable block range is:

- **Overworld / Nether:** `X = 0 … thickness - 1`
- **The End:** `X = -(thickness / 2) … minX + thickness - 1` (centred on `X = 0`)

For example, at `thickness = 16`, the Overworld spans `X = 0 … 15` and the End spans `X = -8 … 7`. At `thickness = 1`, the world is a single column, `X = 0`.

## Requirements

- **Minecraft** 1.21.1
- **NeoForge** 21.1.249 or a compatible NeoForge 21.1 release
- **Java** 21

## Installation

1. Install NeoForge for Minecraft 1.21.1.
2. Download the World Slice release JAR.
3. Place the JAR in the instance's `mods` directory.
4. Launch the game and create or open a world.

The world begins sliced immediately; no additional setup is required.

## Getting Started

- Press **`V`** to toggle the side camera.
- Press **`O`** to open the settings screen while in a world.
- Open **Mods → World Slice → Config** to change defaults from the main menu.
- Walk along **Z** to travel; the **X** axis is the narrow slice depth.

## Configuration

All three entry points open the **same** settings screen design:

| Entry point | Context |
|-------------|---------|
| **Mods → World Slice → Config** | Main menu (no world loaded) |
| **`O`** key | In-world |
| **Pause menu → "World Slice Settings"** | In-world |

### World Thickness (in-world)

- **Type:** per-world server setting.
- **Storage:** the Overworld's `SavedData` (`worldslice_settings`), shared by the Overworld, Nether and End.
- **Range:** 1–4096 blocks.
- **Permission:** in multiplayer, only operators with permission level 2 or higher may change it.

### New World Default Thickness (main menu)

- **Type:** client-editable default.
- **Storage:** `config/worldslice-common.toml` (`defaultWorldThickness`).
- **Semantics:** read **only once**, when a brand-new World Slice world initializes its per-world settings. Existing worlds keep their own saved thickness and are never affected by editing this value.

### Side Camera Distance

- **Type:** per-client setting.
- **Storage:** `config/worldslice-client.toml` (`cameraDistance`).
- **Range:** 8–64 blocks (default 28).
- **Semantics:** affects only the local client; it does not touch the server or other players, and changes preview immediately while the side camera is active.

Thickness changes apply immediately to boundary, collision, fluid and rendering logic in all three dimensions. Newly generated chunks use the current setting; existing chunk and region files are never deleted or bulk-rewritten.

## World Boundary Rules

World Slice enforces two **independent** boundaries:

1. **Fluid boundary** — stops water and lava (and any other `FlowingFluid` implementation) from spreading outside the slice.
2. **Entity boundary** — two invisible virtual collision walls at the outside faces of the slice.

### Entity Boundary Matrix

| Entity type | Behaviour |
|-------------|-----------|
| Player (Survival / Creative / Adventure) | **BLOCKED** |
| Player (Spectator) | Passes |
| Ordinary living entity — mobs, animals, villagers, golems | **BLOCKED** |
| Bosses — Wither, Warden, Elder Guardian, … | **BLOCKED** |
| **Ender Dragon** | **Passes** (explicitly excluded) |
| Item Entity | Passes |
| Experience Orb | Passes |
| Projectile — arrow, snowball, trident, potion | Passes |
| Thrown Item | Passes |
| End Crystal | Passes |
| Vehicle — boat, minecart | Passes |

The entity boundary is a **safety boundary**, not a real barrier block. It relies on the vanilla collision system: when a blocked entity moves toward the edge, its X velocity component is removed while Y and Z movement continue normally, so entities slide along the wall rather than freezing in place. No `Barrier` blocks, saved entities or persistent wall chunks are ever created.

The Ender Dragon is deliberately exempt because its vanilla fight requires the full three-dimensional arena; its flight path, AI phases and velocity are never modified by World Slice.

## Dimension-Specific Behaviour

### Overworld and Nether

- Sliced from `X = 0`.
- The same thickness is shared with the End.

### The End

- Sliced **symmetrically around the vanilla dragon-fight origin** (`X = 0, Z = 0`).
- The central bedrock exit portal therefore remains at the centre of the slice.
- The dragon fight, End crystals, dragon respawn, dragon egg and End gateways continue to use vanilla progression (the exit portal stays closed until the dragon is defeated).

### Upgrading from an older version

The End slice origin changed to be centred on `(0, 0)`. To obtain a correctly centred End:

- prefer a world that has **not yet entered the End**, or
- back up and delete the old `DIM1` data so it regenerates.

World Slice never deletes player dimension data automatically.

## Portal Behaviour

### Nether portals

Nether portals keep the vanilla Z **8:1 scaling** but never scale X as a horizontal distance. X is the slice depth, so crossing between the Overworld and the Nether keeps the player at the same depth (clamped to the shared bounds). Auto-created Nether portals are oriented with their width along Z so the frame stays inside a single X column, even at `worldThickness = 1`.

### Entering the End

The arrival position and obsidian platform are relocated to the slice centre X (`X = 0` for the End) and the ~100-block distance from the dragon-fight centre is moved onto the open-ended Z axis. The player arrives at **`Z = 100`** and travels along Z toward the `(0, 0)` exit portal.

### End exit portal and gateways

The End exit portal stays at `(0, 0)` in the centre of the slice. End gateways keep their computed Z destination and have their X clamped into the slice. Returning to the Overworld after defeating the dragon continues to use the normal bed/world-spawn logic.

## Implementation Notes

### Generator wrapping

World Slice wraps the existing Overworld, Nether and End `ChunkGenerator` instances when each dimension's server chunk source is constructed, so every dimension keeps its own noise settings, biome source, height and seed. Only chunks intersecting the configured block range delegate the vanilla generation pipeline; dependency chunks continue through normal chunk-status creation without terrain, carver, feature, mob or structure placement. A boundary chunk runs vanilla generation and is then clipped to its valid columns, avoiding a large world that is generated and later deleted.

### Centralized bounds

All block, chunk, fluid and entity boundaries are centralized in `WorldSliceBounds`, which exposes a dimension-aware `SliceBounds` value (`minX`, `maxX`, `centerX`, `thickness`). Outside the slice, block and fluid reads expose void/empty state and writes are rejected before an out-of-bounds chunk is loaded.

### Collision

`EntityMixin` injects at the return of Minecraft 1.21.1's `Entity.collectColliders` and adds two finite-Z `VoxelShape` AABBs at the outside faces of the slice, spanning the level build height. `WorldSliceBounds.affectsBoundaryCollision` is the single gate that decides which entities receive the walls.

### Server-side safety net

A low-frequency, player-only safety check clamps direct teleports, portals and other out-of-band position changes back inside the player interval. Ordinary movement is never implemented as a tick teleport, and no per-tick scan over all living entities is performed.

## Known Limitations

- The side camera is perspective-based; foreground terrain between the camera and the player can still occlude the player (foreground transparency is not yet enabled).
- True orthographic projection and an optional true 2D movement mode are not included yet.
- World features and structures that cross the configured boundary are clipped at the virtual boundary.
- End gateways keep their computed Z destination and have X mapped into the slice, but no new safe-landing search is performed; if the mapped column has no terrain the player may need to fly.

## Building from Source

```bash
./gradlew build
```

The resulting JAR is produced at `build/libs/worldslice-<version>.jar`.

## Credits

Thanks to Immortius and the [Chunk By Chunk](https://github.com/immortius/chunkbychunk) project. World Slice is an independent project inspired in part by Chunk By Chunk's world-generation architecture and is not an official version of that project.

## License

World Slice is licensed under the **MIT License**. See [LICENSE](LICENSE) for the full text.

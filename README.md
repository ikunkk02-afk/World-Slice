# World Slice

World Slice turns Minecraft into a configurable, block-accurate world slice, preserving the vertical depth of vanilla terrain while allowing the world to extend infinitely sideways.

## Features

- Configurable world width on the X axis (default 16 blocks), applied to every vanilla dimension
- Overworld, Nether and End are all sliced, sharing one world thickness
- Vanilla terrain, biomes, caves, aquifers, ores, vegetation and structures in the playable slice
- Visible vertical terrain layers and underground caves
- Virtual fluid boundary for water, lava and other `FlowingFluid` implementations
- Invisible entity collision walls at the two outside faces of the slice (players and mobs; the Ender Dragon passes through)
- Terraria-style side camera with smooth entity interpolation
- NeoForge 1.21.1 support

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.249 or a compatible NeoForge 21.1 release
- Java 21

## Installation

1. Install NeoForge for Minecraft 1.21.1.
2. Download the World Slice release JAR.
3. Put the JAR in the instance's `mods` directory.
4. Create or open an Overworld and enter it.

The playable block range is X=0 through X=thickness-1 in the Overworld and Nether. The End is sliced symmetrically around the vanilla dragon-fight origin, from X=-thickness/2 through X=minX+thickness-1 (for example X=-8..7 at thickness 16), so the central bedrock exit portal at (0,0) stays in the centre of the slice. Z remains open-ended in both directions, and the normal Minecraft build height is preserved. Press `V` to toggle the side camera.

## Configuration

The settings screen can be opened three ways, and all of them share the same design:

- **Mods -> World Slice -> Config:** available from the main menu, before entering any world.
- **O:** in-world shortcut.
- **Pause menu -> "World Slice Settings"** button.

- **World Thickness (in-world):** A server/world setting stored in the Overworld's `SavedData` and shared by the Overworld, Nether and End. The supported range is 1-4096 blocks. In multiplayer, only server operators with permission level 2 or higher can change it.
- **New World Default Thickness (main menu):** A client default stored in `config/worldslice-common.toml`. It is read only when a brand new World Slice world initializes its per-world settings for the first time; existing worlds keep their own saved thickness and are never changed by editing this value.
- **Side Camera Distance:** A per-client setting stored in `config/worldslice-client.toml`. The default is 28 blocks and the supported range is 8-64 blocks. It does not affect the server or other players, and changes preview immediately while the side camera is active.
- **V:** Toggle Side Camera.

World Thickness changes apply immediately to boundary, collision, fluid and rendering logic in all three dimensions. New chunks use the current setting; existing chunk/region files are never deleted or bulk-rewritten.

## Implementation notes

World Slice wraps the existing Overworld, Nether and End `ChunkGenerator` instances when each dimension's server chunk source is constructed, so every vanilla dimension keeps its own noise settings, biome source, height and seed. Only chunks intersecting the configured block range delegate the vanilla generation pipeline; dependency chunks continue through normal chunk-status creation without terrain, carver, feature, mob or structure placement. A boundary chunk such as X=0 for width 1 or X=1 for width 17 runs vanilla generation and is then clipped to its valid columns. This avoids generating a large world and deleting it afterward.

The block, fluid and generation boundary is centralized in `WorldSliceBounds`. It distinguishes block bounds, chunk intersection, full containment and partial boundary chunks. Outside the slice, reads expose void/empty fluid and writes are rejected before an out-of-bounds chunk is loaded. The fluid guard is applied at `FlowingFluid.canSpreadTo`, so it also covers fluids implemented through the vanilla flowing-fluid hierarchy.

Entity movement uses a separate virtual boundary. `EntityMixin` injects at the return of Minecraft 1.21.1's `Entity.collectColliders` and adds two finite-Z `VoxelShape` AABBs at the outside faces of the slice, spanning the level build height. Vanilla collision resolution therefore removes only the blocked X component while preserving Z movement, jumping, falling, swimming and flying behavior. The shapes are created for non-spectator players and every ordinary living entity (mobs, animals, villagers, golems and bosses such as the Wither); the Ender Dragon is explicitly excluded so its vanilla fight can fly through the full 3D arena, while items, experience orbs, projectiles, boats and minecarts pass through the boundary. No barrier blocks, saved entities or persistent wall chunks are created.

The server also performs a low-frequency safety check for `ServerPlayer` instances in every sliced dimension. It clamps direct teleports, portals and other out-of-band position changes back inside the player bounding-box interval; ordinary movement is never implemented as a tick teleport. Spectators retain their normal no-clip behavior. The fluid boundary remains independent and continues to protect water and lava.

### Portal behavior

Nether portals keep the vanilla Z 8:1 scaling but never scale X as a horizontal distance. X is the slice depth, so crossing between the Overworld and the Nether keeps the player at the same slice depth (clamped to the shared bounds). Auto-created Nether portals are oriented with their width along Z so the frame stays inside a single X column, even at `worldThickness = 1`.

Entering the End relocates the arrival position and the obsidian platform to the slice centre X (X=0 for the End) and moves the ~100-block distance from the dragon-fight centre onto the open-ended Z axis, so the player arrives at Z=100 and travels along Z toward the (0,0) exit portal. The central bedrock exit portal stays at (0,0) in the middle of the slice. The End exit portal and End gateways keep their Z destination and have their X clamped into the slice. Returning to the Overworld after defeating the dragon continues to use the normal bed/world-spawn logic.

> Updating from an older World Slice version: the End slice origin changed to be centred on (0,0). For a correctly centred End, use a world that has not entered the End yet, or back up and delete the old `DIM1` data so it regenerates. World Slice never deletes player dimension data automatically.

## Known Issues

- The side camera is perspective-based. The foreground terrain between the camera and the player can still occlude the player; foreground transparency is not enabled yet.
- True orthographic projection and an optional true 2D movement mode are not included yet.
- World features and structures that cross the configured boundary are clipped at the virtual boundary.
- End gateways keep their computed Z destination and have X mapped into the slice, but no new safe landing search is performed; if the mapped column has no terrain the player may need to fly.

## Credits

Thanks to Immortius and the [Chunk By Chunk](https://github.com/immortius/chunkbychunk) project. World Slice is an independent project inspired in part by Chunk By Chunk's world-generation architecture and is not an official version of that project.

## License

World Slice is licensed under the MIT License. See [LICENSE](LICENSE).

# World Slice

World Slice turns Minecraft into a configurable, block-accurate world slice, preserving the vertical depth of vanilla terrain while allowing the world to extend infinitely sideways.

## Features

- Configurable world width on the X axis (default 16 blocks), applied to every vanilla dimension
- Overworld, Nether and End are all sliced, sharing one world thickness
- Vanilla terrain, biomes, caves, aquifers, ores, vegetation and structures in the playable slice
- Visible vertical terrain layers and underground caves
- Virtual fluid boundary for water, lava and other `FlowingFluid` implementations
- Invisible player collision walls at the two outside faces of the slice
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

The playable block range is X=0 through X=thickness-1 in every vanilla dimension. Z remains open-ended in both directions, and the normal Minecraft build height is preserved. Press `V` to toggle the side camera.

## Configuration

Press `O` to open the World Slice settings screen. The UI follows the vanilla Minecraft options style.

- **World Thickness:** A server/world setting stored in the Overworld's `SavedData` and shared by the Overworld, Nether and End. The default is 16 blocks and the supported range is 1-4096 blocks. The valid block range is always X=0 through X=thickness-1; it is not rounded to a multiple of 16. In multiplayer, only server operators with permission level 2 or higher can change it.
- **Side Camera Distance:** A per-client setting stored in `config/worldslice-client.toml`. The default is 28 blocks and the supported range is 8-64 blocks. It does not affect the server or other players, and changes preview immediately while the side camera is active.
- **V:** Toggle Side Camera.
- **O:** Open World Slice settings. A "World Slice Settings" button is also available in the pause menu.

World Thickness changes apply immediately to boundary, collision, fluid and rendering logic in all three dimensions. New chunks use the current setting; existing chunk/region files are never deleted or bulk-rewritten.

## Implementation notes

World Slice wraps the existing Overworld, Nether and End `ChunkGenerator` instances when each dimension's server chunk source is constructed, so every vanilla dimension keeps its own noise settings, biome source, height and seed. Only chunks intersecting the configured block range delegate the vanilla generation pipeline; dependency chunks continue through normal chunk-status creation without terrain, carver, feature, mob or structure placement. A boundary chunk such as X=0 for width 1 or X=1 for width 17 runs vanilla generation and is then clipped to its valid columns. This avoids generating a large world and deleting it afterward.

The block, fluid and generation boundary is centralized in `WorldSliceBounds`. It distinguishes block bounds, chunk intersection, full containment and partial boundary chunks. Outside the slice, reads expose void/empty fluid and writes are rejected before an out-of-bounds chunk is loaded. The fluid guard is applied at `FlowingFluid.canSpreadTo`, so it also covers fluids implemented through the vanilla flowing-fluid hierarchy.

Player movement uses a separate virtual boundary. `EntityMixin` injects at the return of Minecraft 1.21.1's `Entity.collectColliders` and adds two finite-Z `VoxelShape` AABBs at the outside faces X=0 and X=thickness, spanning the level build height. Vanilla collision resolution therefore removes only the blocked X component while preserving Z movement, jumping, falling, swimming and flying behavior. The shapes are created only for non-spectator players in a sliced dimension, and no barrier blocks, saved entities or persistent wall chunks are created.

The server also performs a low-frequency safety check for `ServerPlayer` instances in every sliced dimension. It clamps direct teleports, portals and other out-of-band position changes back inside the player bounding-box interval; ordinary movement is never implemented as a tick teleport. Spectators retain their normal no-clip behavior. The fluid boundary remains independent and continues to protect water and lava.

### Portal behavior

Nether portals keep the vanilla Z 8:1 scaling but never scale X as a horizontal distance. X is the slice depth, so crossing between the Overworld and the Nether keeps the player at the same slice depth (clamped to the shared bounds). Auto-created Nether portals are oriented with their width along Z so the frame stays inside a single X column, even at `worldThickness = 1`.

Entering the End relocates the arrival position and the obsidian platform to the slice centre X instead of the vanilla X=100, so the player never arrives over the void. The End exit portal and End gateways keep their Z destination and have their X clamped into the slice. Returning to the Overworld after defeating the dragon continues to use the normal bed/world-spawn logic.

## Known Issues

- The side camera is perspective-based. The foreground terrain between the camera and the player can still occlude the player; foreground transparency is not enabled yet.
- True orthographic projection and an optional true 2D movement mode are not included yet.
- World features and structures that cross the configured boundary are clipped at the virtual boundary.
- End gateways keep their computed Z destination and have X mapped into the slice, but no new safe landing search is performed; if the mapped column has no terrain the player may need to fly.

## Credits

Thanks to Immortius and the [Chunk By Chunk](https://github.com/immortius/chunkbychunk) project. World Slice is an independent project inspired in part by Chunk By Chunk's world-generation architecture and is not an official version of that project.

## License

World Slice is licensed under the MIT License. See [LICENSE](LICENSE).

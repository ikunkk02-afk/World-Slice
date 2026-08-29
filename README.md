# World Slice

World Slice turns Minecraft into a 16-block-thick world slice, preserving the vertical depth of vanilla terrain while allowing the world to extend infinitely sideways.

## Features

- 16-block-thick Overworld world
- Vanilla terrain, biomes, caves, aquifers, ores, vegetation and structures in the playable slice
- Visible vertical terrain layers and underground caves
- Virtual fluid boundary for water, lava and other `FlowingFluid` implementations
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
4. Create a new Overworld and enter it. A new world is recommended for a clean slice.

The playable block range is X=0 through X=15. Z remains open-ended in both directions, and the normal Minecraft build height is preserved. Press `V` to toggle the side camera.

## Implementation notes

World Slice wraps the existing Overworld `ChunkGenerator` when the server's chunk source is constructed. Chunk X=0 delegates the complete vanilla generation pipeline, while dependency chunks continue through normal chunk-status creation without terrain, carver, feature, mob or structure placement. This avoids generating a large world and deleting it afterward.

The block, fluid and generation boundary is centralized in `WorldSliceBounds`. Outside the slice, reads expose void/empty fluid and writes are rejected before an out-of-bounds chunk is loaded. The fluid guard is applied at `FlowingFluid.canSpreadTo`, so it also covers fluids implemented through the vanilla flowing-fluid hierarchy.

## Known Issues

- The first release modifies the Overworld only. Nether and End remain vanilla.
- The side camera is perspective-based. The foreground terrain between the camera and the player can still occlude the player; foreground transparency is not enabled yet.
- True orthographic projection and an optional true 2D movement mode are not included yet.
- World features and structures that cross X=0 or X=15 are clipped at the virtual boundary.
- Existing worlds are not retroactively rewritten outside the slice. Use a new world when converting an existing installation.

## Credits

Thanks to Immortius and the [Chunk By Chunk](https://github.com/immortius/chunkbychunk) project. World Slice is an independent project inspired in part by Chunk By Chunk's world-generation architecture and is not an official version of that project.

## License

World Slice is licensed under the MIT License. See [LICENSE](LICENSE).

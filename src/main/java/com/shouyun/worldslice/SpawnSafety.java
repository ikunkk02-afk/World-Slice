package com.shouyun.worldslice;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Finds a natural spawn in the playable chunk column without changing the
 * world-generation seed or making a large synchronous chunk-generation burst.
 */
public final class SpawnSafety {
    /**
     * Search radii in chunks. The third phase covers +/-4096 blocks from the
     * vanilla spawn Z. The last phase is an asynchronous emergency extension
     * used only when that whole strip contains no safe natural surface.
     */
    private static final int[] SEARCH_RADII = {16, 128, 256, 512};

    static final int INITIAL_CHUNKS_PER_START = 40;
    static final int CHUNKS_PER_TICK = 4;
    static final int CHUNKS_PER_PLAYER_EVENT = 16;

    private SpawnSafety() {
    }

    /**
     * Starts a bounded, resumable search centred on the spawn Z Minecraft
     * already selected for this world. X is deliberately not copied because
     * only chunks intersecting the configured slice are requested.
     */
    public static Search beginSearch(ServerLevel level, BlockPos vanillaSpawn) {
        return new Search(level, vanillaSpawn.getZ(), WorldSliceBounds.thickness(level));
    }

    /**
     * Checks a position using the same constraints used by the search. This is
     * also used for vanilla bed and dimension respawn positions.
     */
    public static boolean isSafeSpawnPosition(ServerLevel level, BlockPos feet) {
        if (!WorldSliceBounds.isInside(level, feet) || feet.getY() <= level.getMinBuildHeight()
            || feet.getY() >= level.getMaxBuildHeight() - 1) {
            return false;
        }

        return isSafe(level, feet, feet.below());
    }

    public static String describeBiome(ServerLevel level, BlockPos pos) {
        return biomeName(level.getBiome(pos));
    }

    /**
     * Last-resort spawn for a slice that contains no dry, sturdy natural
     * surface in the search range. The Z coordinate is derived from this
     * world's own seed, so it is neither the origin nor a shared fixed point.
     * A small stone pad is created above sea level only in this exceptional
     * case; normal worlds always use an existing generated surface.
     */
    public static BlockPos createSafeFallback(ServerLevel level, BlockPos vanillaSpawn) {
        long mixedSeed = level.getSeed() ^ (level.getSeed() >>> 33);
        int offset = Math.floorMod((int)(mixedSeed ^ (mixedSeed >>> 32)), 1025) - 512;
        int fallbackChunkZ = Math.floorDiv(vanillaSpawn.getZ(), 16) + offset;
        int worldZ = fallbackChunkZ * 16 + 8;
        int floorY = level.getSeaLevel() + 1;

        int thickness = WorldSliceBounds.thickness(level);
        int centerX = WorldSliceBounds.centerX(thickness);
        level.getChunk(Math.floorDiv(centerX, 16), fallbackChunkZ, ChunkStatus.FULL, true);
        for (int x = WorldSliceBounds.minX(); x <= WorldSliceBounds.maxX(thickness); x++) {
            for (int z = worldZ - 1; z <= worldZ + 1; z++) {
                level.setBlock(new BlockPos(x, floorY, z), Blocks.STONE.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x, floorY + 1, z), Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x, floorY + 2, z), Blocks.AIR.defaultBlockState(), 3);
            }
        }

        BlockPos spawn = new BlockPos(centerX, floorY + 1, worldZ);
        return isSafeSpawnPosition(level, spawn) ? spawn : null;
    }

    private static boolean isSafe(ServerLevel level, BlockPos feet, BlockPos floor) {
        var floorState = level.getBlockState(floor);
        var feetState = level.getBlockState(feet);
        var headState = level.getBlockState(feet.above());

        return floorState.isFaceSturdy(level, floor, net.minecraft.core.Direction.UP)
            && !floorState.is(net.minecraft.tags.BlockTags.LEAVES)
            && level.getFluidState(floor).isEmpty()
            && feetState.getCollisionShape(level, feet).isEmpty()
            && headState.getCollisionShape(level, feet.above()).isEmpty()
            && !feetState.is(net.minecraft.tags.BlockTags.LEAVES)
            && !headState.is(net.minecraft.tags.BlockTags.LEAVES)
            && level.getFluidState(feet).isEmpty()
            && level.getFluidState(feet.above()).isEmpty();
    }

    private static int biomeRank(Holder<Biome> biome) {
        String path = biome.unwrapKey()
            .map(key -> key.location().getPath().toLowerCase(java.util.Locale.ROOT))
            .orElse("");

        if (path.contains("ocean")) {
            return 0;
        }
        if (path.contains("river")) {
            return 1;
        }
        if (path.contains("beach") || path.contains("snow") || path.contains("mountain")
            || path.contains("peaks") || path.contains("windswept")) {
            return 2;
        }
        if (path.contains("plains") || path.contains("forest") || path.contains("taiga")
            || path.contains("savanna")) {
            return 4;
        }
        return 3;
    }

    private static String biomeName(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> key.location().toString()).orElse("unknown");
    }

    public static final class Search {
        private final ServerLevel level;
        private final int originZ;
        private final int originChunkZ;
        private final int thickness;
        private final int[] preferredX;
        private final int minChunkX;
        private final int maxChunkX;

        private Candidate bestCandidate;
        private int radiusIndex;
        private int offsetIndex;
        private Integer currentChunkZ;
        private int nextChunkX;
        private int previousRadius = -1;
        private boolean complete;

        private Search(ServerLevel level, int originZ, int thickness) {
            this.level = level;
            this.originZ = originZ;
            this.originChunkZ = Math.floorDiv(originZ, 16);
            this.thickness = WorldSliceWorldSettings.sanitize(thickness);
            this.preferredX = preferredColumns(this.thickness);
            this.minChunkX = Math.floorDiv(WorldSliceBounds.minX(), 16);
            this.maxChunkX = Math.floorDiv(WorldSliceBounds.maxX(this.thickness), 16);
            this.nextChunkX = this.minChunkX;
        }

        /** Advances at most {@code maxChunks} chunk generations. */
        public void advance(int maxChunks) {
            if (maxChunks <= 0) {
                return;
            }

            int processed = 0;
            while (!complete && processed < maxChunks) {
                if (currentChunkZ == null) {
                    currentChunkZ = nextChunkZ();
                    nextChunkX = minChunkX;
                    if (currentChunkZ == null) {
                        continue;
                    }
                }

                // Generate only chunks that intersect the configured slice.
                // A width greater than one chunk may need the final partial
                // boundary chunk as well as the first chunk column.
                ChunkAccess chunk = level.getChunk(nextChunkX, currentChunkZ, ChunkStatus.FULL, true);
                inspectChunk(chunk, currentChunkZ);
                nextChunkX++;
                if (nextChunkX > maxChunkX) {
                    currentChunkZ = null;
                }
                processed++;
            }
        }

        public boolean isComplete() {
            return complete;
        }

        public BlockPos bestSpawn() {
            return bestCandidate == null ? null : bestCandidate.position();
        }

        public String bestBiome() {
            return bestCandidate == null ? "unknown" : bestCandidate.biome();
        }

        private Integer nextChunkZ() {
            while (radiusIndex < SEARCH_RADII.length) {
                int offset = symmetricOffset(offsetIndex++);
                if (Math.abs(offset) <= previousRadius) {
                    continue;
                }
                if (Math.abs(offset) <= SEARCH_RADII[radiusIndex]) {
                    return originChunkZ + offset;
                }

                // A normal land result in the completed phase is sufficient;
                // otherwise widen the search without doing hundreds of loads
                // in this call. The first three phases are bounded at +/-4096;
                // the last phase is only an emergency extension.
                if (bestCandidate != null && biomeRank(bestCandidate.biomeHolder()) >= 3) {
                    complete = true;
                    return null;
                }

                previousRadius = SEARCH_RADII[radiusIndex];
                radiusIndex++;
                offsetIndex = 0;
            }

            complete = true;
            return null;
        }

        private void inspectChunk(ChunkAccess chunk, int chunkZ) {
            int minY = level.getMinBuildHeight() + 1;
            int maxY = level.getMaxBuildHeight() - 2;
            int localZCentre = chunkZ == originChunkZ ? Math.floorMod(originZ, 16) : 8;

            int chunkMinX = chunk.getPos().getMinBlockX();
            int chunkMaxX = chunkMinX + 15;
            for (int x : preferredX) {
                if (x < chunkMinX || x > chunkMaxX) {
                    continue;
                }

                // Local Z is searched within the already selected chunk, in
                // origin-centred order. Chunk selection, not block walking,
                // is the unit that controls world generation cost.
                for (int distance = 0; distance < 16; distance++) {
                    int plus = localZCentre + distance;
                    if (plus < 16) {
                        inspectColumn(chunk, x - chunkMinX, plus, minY, maxY);
                    }

                    if (distance != 0) {
                        int minus = localZCentre - distance;
                        if (minus >= 0) {
                            inspectColumn(chunk, x - chunkMinX, minus, minY, maxY);
                        }
                    }
                }
            }
        }

        private void inspectColumn(ChunkAccess chunk, int x, int localZ, int minY, int maxY) {
            // ChunkAccess.getHeight returns the Y of the top blocking block
            // (the same convention used by PlayerRespawnLogic), so the
            // player's feet must be one block above it.
            int surfaceY = Math.min(chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, localZ), maxY - 1);
            if (surfaceY < minY) {
                return;
            }

            int worldZ = chunk.getPos().getMinBlockZ() + localZ;
            int worldX = chunk.getPos().getMinBlockX() + x;
            BlockPos feet = new BlockPos(worldX, surfaceY + 1, worldZ);

            if (!isSafeSpawnPosition(level, feet)) {
                return;
            }

            Holder<Biome> biome = level.getBiome(feet.below());
            Candidate candidate = new Candidate(feet, biomeRank(biome), biomeName(biome), biome);
            if (bestCandidate == null || candidate.isPreferredTo(bestCandidate, originZ, WorldSliceBounds.centerX(thickness))) {
                bestCandidate = candidate;
            }
        }

        private static int symmetricOffset(int index) {
            if (index == 0) {
                return 0;
            }

            int distance = (index + 1) / 2;
            return (index & 1) == 1 ? distance : -distance;
        }

        private static int[] preferredColumns(int thickness) {
            int[] columns = new int[thickness];
            int centerLeft = (thickness - 1) / 2;
            int centerRight = thickness / 2;
            int index = 0;
            columns[index++] = centerLeft;
            if (centerRight != centerLeft) {
                columns[index++] = centerRight;
            }

            for (int distance = 1; index < thickness; distance++) {
                int left = centerLeft - distance;
                if (left >= 0) {
                    columns[index++] = left;
                }

                int right = centerRight + distance;
                if (right < thickness && index < thickness) {
                    columns[index++] = right;
                }
            }
            return columns;
        }
    }

    private record Candidate(BlockPos position, int rank, String biome, Holder<Biome> biomeHolder) {
        private boolean isPreferredTo(Candidate other, int originZ, int centerX) {
            if (rank != other.rank) {
                return rank > other.rank;
            }

            long distance = Math.abs((long)position.getZ() - originZ);
            long otherDistance = Math.abs((long)other.position.getZ() - originZ);
            if (distance != otherDistance) {
                return distance < otherDistance;
            }

            return Math.abs(position.getX() - centerX) < Math.abs(other.position.getX() - centerX);
        }
    }
}

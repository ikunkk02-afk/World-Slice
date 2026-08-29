package com.shouyun.worldslice;

import com.google.common.collect.ImmutableList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Central definition of the playable block, chunk and player bounds. */
public final class WorldSliceBounds {
    private static final int MIN_X = 0;
    private static final double MIN_PLAYER_X = MIN_X;
    private static final double PLAYER_SAFETY_EPSILON = 1.0E-4D;
    private static final double COLLISION_QUERY_MARGIN = 1.0D;

    private WorldSliceBounds() {
    }

    /** Default, context-free view retained for unit tests and old callers. */
    public static boolean isInside(BlockPos pos) {
        return isInsideX(pos.getX());
    }

    public static boolean isInside(Level level, BlockPos pos) {
        return isInsideX(level, pos.getX());
    }

    public static boolean isInsideX(int x) {
        return isInsideX(x, WorldSliceWorldSettings.DEFAULT_WORLD_THICKNESS);
    }

    public static boolean isInsideX(Level level, int x) {
        return isInsideX(x, thickness(level));
    }

    public static boolean isInsideBlockX(int x) {
        return isInsideX(x);
    }

    public static boolean isInsideBlockX(int x, int worldThickness) {
        return isInsideX(x, worldThickness);
    }

    public static boolean isInsideX(int x, int worldThickness) {
        return x >= MIN_X && x < WorldSliceWorldSettings.sanitize(worldThickness);
    }

    private static boolean isInsideX(double x, int worldThickness) {
        return x >= MIN_X && x < WorldSliceWorldSettings.sanitize(worldThickness);
    }

    /** Whether any block column in the ChunkPos intersects the slice. */
    public static boolean doesChunkIntersectSlice(ChunkPos chunkPos) {
        return doesChunkIntersectSlice(chunkPos, WorldSliceWorldSettings.DEFAULT_WORLD_THICKNESS);
    }

    public static boolean doesChunkIntersectSlice(ChunkPos chunkPos, int worldThickness) {
        int thickness = WorldSliceWorldSettings.sanitize(worldThickness);
        return chunkPos.getMaxBlockX() >= MIN_X && chunkPos.getMinBlockX() < thickness;
    }

    /** Whether every block column in the ChunkPos is inside the slice. */
    public static boolean isChunkFullyInsideSlice(ChunkPos chunkPos) {
        return isChunkFullyInsideSlice(chunkPos, WorldSliceWorldSettings.DEFAULT_WORLD_THICKNESS);
    }

    public static boolean isChunkFullyInsideSlice(ChunkPos chunkPos, int worldThickness) {
        int thickness = WorldSliceWorldSettings.sanitize(worldThickness);
        return chunkPos.getMinBlockX() >= MIN_X && chunkPos.getMaxBlockX() < thickness;
    }

    public static boolean isPartialBoundaryChunk(ChunkPos chunkPos) {
        return isPartialBoundaryChunk(chunkPos, WorldSliceWorldSettings.DEFAULT_WORLD_THICKNESS);
    }

    public static boolean isPartialBoundaryChunk(ChunkPos chunkPos, int worldThickness) {
        return doesChunkIntersectSlice(chunkPos, worldThickness)
            && !isChunkFullyInsideSlice(chunkPos, worldThickness);
    }

    /** Kept as a compatibility alias; validity now means intersection, not full containment. */
    public static boolean isValidChunk(ChunkPos chunkPos) {
        return doesChunkIntersectSlice(chunkPos);
    }

    public static boolean canFluidEnter(BlockPos pos) {
        return isInside(pos);
    }

    public static boolean canFluidEnter(BlockGetter level, BlockPos pos) {
        return isInside(level, pos);
    }

    public static boolean isInside(BlockGetter level, BlockPos pos) {
        Level actualLevel = asLevel(level);
        return actualLevel == null ? isInside(pos) : isInside(actualLevel, pos);
    }

    public static boolean isWorldSliceLevel(Level level) {
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return false;
        }

        if (level.isClientSide) {
            return WorldSliceWorldSettings.isClientWorldSliceActive();
        }

        return level.getChunkSource() instanceof ServerChunkCache serverChunkCache
            && serverChunkCache.getGenerator() instanceof WorldSliceGenerator;
    }

    public static boolean isWorldSliceLevel(BlockGetter level) {
        Level actualLevel = asLevel(level);
        return actualLevel != null && isWorldSliceLevel(actualLevel);
    }

    private static Level asLevel(BlockGetter level) {
        if (level instanceof Level actualLevel) {
            return actualLevel;
        }
        if (level instanceof WorldGenRegion region) {
            return region.getLevel();
        }
        return null;
    }

    public static int minX() {
        return MIN_X;
    }

    public static int maxX() {
        return maxX(WorldSliceWorldSettings.DEFAULT_WORLD_THICKNESS);
    }

    public static int maxX(Level level) {
        return maxX(thickness(level));
    }

    public static int maxX(int worldThickness) {
        return WorldSliceWorldSettings.sanitize(worldThickness) - 1;
    }

    public static int thickness() {
        return WorldSliceWorldSettings.DEFAULT_WORLD_THICKNESS;
    }

    public static int thickness(Level level) {
        if (level.isClientSide) {
            return WorldSliceWorldSettings.clientWorldThickness();
        }
        if (level instanceof ServerLevel serverLevel
            && serverLevel.getChunkSource() instanceof ServerChunkCache serverChunkCache
            && serverChunkCache.getGenerator() instanceof WorldSliceGenerator generator) {
            return generator.worldThickness();
        }
        return WorldSliceWorldSettings.DEFAULT_WORLD_THICKNESS;
    }

    public static int chunkWidth(int worldThickness) {
        return (WorldSliceWorldSettings.sanitize(worldThickness) + 15) / 16;
    }

    public static int centerX(int worldThickness) {
        return (WorldSliceWorldSettings.sanitize(worldThickness) - 1) / 2;
    }

    /**
     * Clears only the invalid columns of a boundary chunk after vanilla
     * generation has completed for the current status. This is intentionally
     * limited to the one or two intersecting partial chunks; fully external
     * chunks never enter the parent generator's terrain pipeline.
     */
    public static void trimChunkToSlice(ChunkAccess chunk, int worldThickness) {
        if (!isPartialBoundaryChunk(chunk.getPos(), worldThickness)) {
            return;
        }

        int thickness = WorldSliceWorldSettings.sanitize(worldThickness);
        int chunkMinX = chunk.getPos().getMinBlockX();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; localX++) {
            int x = chunkMinX + localX;
            if (isInsideX(x, thickness)) {
                continue;
            }

            for (int y = chunk.getMinBuildHeight(); y < chunk.getMaxBuildHeight(); y++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    mutablePos.set(x, y, chunk.getPos().getMinBlockZ() + localZ);
                    chunk.setBlockState(mutablePos, Blocks.AIR.defaultBlockState(), false);
                }
            }
        }

        // Features can create block entities directly on a ChunkAccess. Remove
        // any such data in columns that the slice does not expose.
        for (BlockPos pos : chunk.getBlockEntitiesPos().stream()
            .filter(pos -> !isInsideX(pos.getX(), thickness))
            .toList()) {
            chunk.removeBlockEntity(pos);
        }

        if (chunk instanceof ProtoChunk protoChunk) {
            protoChunk.getEntities().removeIf(entityTag -> {
                ListTag position = entityTag.getList("Pos", Tag.TAG_DOUBLE);
                return !position.isEmpty() && !isInsideX(position.getDouble(0), thickness);
            });
        }
        chunk.setUnsaved(true);
    }

    /** The left virtual collision plane, at the outside face of block X=0. */
    public static double minPlayerX() {
        return MIN_PLAYER_X;
    }

    public static double minPlayerX(Level level) {
        return MIN_PLAYER_X;
    }

    /** The right virtual collision plane, immediately after the last valid block column. */
    public static double maxPlayerX() {
        return maxPlayerX(WorldSliceWorldSettings.DEFAULT_WORLD_THICKNESS);
    }

    public static double maxPlayerX(Level level) {
        return maxPlayerX(thickness(level));
    }

    public static double maxPlayerX(int worldThickness) {
        return WorldSliceWorldSettings.sanitize(worldThickness);
    }

    /** Returns whether an entity's complete bounding box is inside the player collision interval. */
    public static boolean isPlayerInside(Entity entity) {
        return isPlayerInside(entity, entity.level());
    }

    public static boolean isPlayerInside(Entity entity, Level level) {
        AABB box = entity.getBoundingBox();
        return box.minX >= minPlayerX(level) - PLAYER_SAFETY_EPSILON
            && box.maxX <= maxPlayerX(level) + PLAYER_SAFETY_EPSILON;
    }

    /** Clamps a point to the open player interval with a small safety margin. */
    public static double clampPlayerX(double x) {
        return clampPlayerX(x, WorldSliceWorldSettings.DEFAULT_WORLD_THICKNESS);
    }

    public static double clampPlayerX(Level level, double x) {
        return clampPlayerX(x, thickness(level));
    }

    public static double clampPlayerX(double x, int worldThickness) {
        double maxPlayerX = maxPlayerX(worldThickness);
        return Math.max(
            MIN_PLAYER_X + PLAYER_SAFETY_EPSILON,
            Math.min(maxPlayerX - PLAYER_SAFETY_EPSILON, x)
        );
    }

    /** Clamps an entity center while accounting for its actual width. */
    public static double clampPlayerX(Entity entity, double x) {
        return clampPlayerX(entity, x, thickness(entity.level()));
    }

    public static double clampPlayerX(Entity entity, double x, int worldThickness) {
        double halfWidth = entity.getBbWidth() * 0.5D;
        double min = MIN_PLAYER_X + halfWidth + PLAYER_SAFETY_EPSILON;
        double max = maxPlayerX(worldThickness) - halfWidth - PLAYER_SAFETY_EPSILON;
        if (min > max) {
            return maxPlayerX(worldThickness) * 0.5D;
        }
        return Math.max(min, Math.min(max, x));
    }

    /** Whether this entity should receive the virtual player walls. */
    public static boolean affectsPlayerCollision(Entity entity, Level level) {
        return entity instanceof Player
            && !entity.isSpectator()
            && isWorldSliceLevel(level);
    }

    /** Adds two finite-Z virtual side walls to an existing collision result. */
    public static List<VoxelShape> addPlayerCollisionWalls(
        List<VoxelShape> collisions, Level level, AABB collisionQuery
    ) {
        return addPlayerCollisionWalls(
            collisions,
            collisionQuery,
            level.getMinBuildHeight(),
            level.getMaxBuildHeight(),
            thickness(level)
        );
    }

    static List<VoxelShape> addPlayerCollisionWalls(
        List<VoxelShape> collisions, AABB collisionQuery, int minBuildHeight, int maxBuildHeight
    ) {
        return addPlayerCollisionWalls(
            collisions,
            collisionQuery,
            minBuildHeight,
            maxBuildHeight,
            WorldSliceWorldSettings.DEFAULT_WORLD_THICKNESS
        );
    }

    private static List<VoxelShape> addPlayerCollisionWalls(
        List<VoxelShape> collisions,
        AABB collisionQuery,
        int minBuildHeight,
        int maxBuildHeight,
        int worldThickness
    ) {
        double maxPlayerX = maxPlayerX(worldThickness);
        if (collisionQuery.minX > MIN_PLAYER_X + COLLISION_QUERY_MARGIN
            && collisionQuery.maxX < maxPlayerX - COLLISION_QUERY_MARGIN) {
            return collisions;
        }

        ImmutableList.Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(collisions.size() + 2);
        builder.addAll(collisions);
        builder.add(Shapes.create(new AABB(
            MIN_PLAYER_X - 1.0D,
            minBuildHeight,
            collisionQuery.minZ,
            MIN_PLAYER_X,
            maxBuildHeight,
            collisionQuery.maxZ
        )));
        builder.add(Shapes.create(new AABB(
            maxPlayerX,
            minBuildHeight,
            collisionQuery.minZ,
            maxPlayerX + 1.0D,
            maxBuildHeight,
            collisionQuery.maxZ
        )));
        return builder.build();
    }
}

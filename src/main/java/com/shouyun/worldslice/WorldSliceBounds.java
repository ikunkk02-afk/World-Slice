package com.shouyun.worldslice;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Central definition of the playable block, chunk and player bounds.
 *
 * <p>The Overworld and Nether are sliced from {@code X=0}. The End is sliced
 * symmetrically around the vanilla dragon-fight origin {@code X=0}, so the
 * central bedrock exit portal remains at the centre of the slice. All methods
 * accept the current dimension so no caller has to special-case the End.</p>
 */
public final class WorldSliceBounds {
    private static final double PLAYER_SAFETY_EPSILON = 1.0E-4D;
    private static final double COLLISION_QUERY_MARGIN = 1.0D;

    /** The vanilla dimensions World Slice wraps. */
    private static final Set<ResourceKey<Level>> SUPPORTED_DIMENSIONS = Set.of(
        Level.OVERWORLD,
        Level.NETHER,
        Level.END
    );

    private WorldSliceBounds() {
    }

    /**
     * Whether World Slice should wrap this dimension's generator. Kept as the
     * single place that knows which vanilla dimensions are eligible; runtime
     * wrapping is still decided by the actual generator type.
     */
    public static boolean isSupportedDimension(ResourceKey<Level> dimension) {
        return SUPPORTED_DIMENSIONS.contains(dimension);
    }

    // ---------------------------------------------------------------------
    // Dimension-specific slice bounds
    // ---------------------------------------------------------------------

    /** The inclusive block-column range for a dimension and thickness. */
    public static SliceBounds forDimension(ResourceKey<Level> dimension, int thickness) {
        int sanitized = WorldSliceWorldSettings.sanitize(thickness);
        int minX = dimension == Level.END ? -(sanitized / 2) : 0;
        return new SliceBounds(minX, minX + sanitized - 1);
    }

    /** The inclusive block-column range currently active in a level. */
    public static SliceBounds forLevel(Level level) {
        return forDimension(level.dimension(), thickness(level));
    }

    public static int minX(ResourceKey<Level> dimension, int thickness) {
        return forDimension(dimension, thickness).minX();
    }

    public static int maxX(ResourceKey<Level> dimension, int thickness) {
        return forDimension(dimension, thickness).maxX();
    }

    public static int minX(Level level) {
        return forLevel(level).minX();
    }

    public static int maxX(Level level) {
        return forLevel(level).maxX();
    }

    public static int centerX(ResourceKey<Level> dimension, int thickness) {
        return forDimension(dimension, thickness).centerX();
    }

    public static int centerX(Level level) {
        return forLevel(level).centerX();
    }

    // ---------------------------------------------------------------------
    // Context-free helpers. These describe the Overworld (minX = 0) and are
    // kept for unit tests, spawn search and debug output.
    // ---------------------------------------------------------------------

    public static int minX() {
        return 0;
    }

    public static int maxX() {
        return maxX(Level.OVERWORLD, WorldSliceWorldSettings.DEFAULT_WORLD_THICKNESS);
    }

    public static int maxX(int worldThickness) {
        return forDimension(Level.OVERWORLD, worldThickness).maxX();
    }

    public static int centerX(int worldThickness) {
        return forDimension(Level.OVERWORLD, worldThickness).centerX();
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

    // ---------------------------------------------------------------------
    // Block bounds
    // ---------------------------------------------------------------------

    public static boolean isInside(BlockPos pos) {
        return isInsideX(pos.getX());
    }

    public static boolean isInside(Level level, BlockPos pos) {
        return isInsideX(level, pos.getX());
    }

    public static boolean isInside(BlockGetter level, BlockPos pos) {
        Level actualLevel = asLevel(level);
        return actualLevel == null ? isInside(pos) : isInside(actualLevel, pos);
    }

    public static boolean isInsideX(int x) {
        return isInsideX(Level.OVERWORLD, x, WorldSliceWorldSettings.DEFAULT_WORLD_THICKNESS);
    }

    public static boolean isInsideX(int x, int worldThickness) {
        return isInsideX(Level.OVERWORLD, x, worldThickness);
    }

    public static boolean isInsideX(Level level, int x) {
        return isInsideX(level.dimension(), x, thickness(level));
    }

    public static boolean isInsideX(ResourceKey<Level> dimension, int x, int worldThickness) {
        SliceBounds bounds = forDimension(dimension, worldThickness);
        return bounds.contains(x);
    }

    private static boolean isInsideX(ResourceKey<Level> dimension, double x, int worldThickness) {
        SliceBounds bounds = forDimension(dimension, worldThickness);
        return x >= bounds.minX() && x <= bounds.maxX();
    }

    public static boolean isInsideBlockX(int x) {
        return isInsideX(x);
    }

    public static boolean isInsideBlockX(int x, int worldThickness) {
        return isInsideX(x, worldThickness);
    }

    // ---------------------------------------------------------------------
    // Chunk bounds
    // ---------------------------------------------------------------------

    /** Whether any block column in the ChunkPos intersects the slice. */
    public static boolean doesChunkIntersectSlice(ChunkPos chunkPos) {
        return doesChunkIntersectSlice(chunkPos, WorldSliceWorldSettings.DEFAULT_WORLD_THICKNESS);
    }

    public static boolean doesChunkIntersectSlice(ChunkPos chunkPos, int worldThickness) {
        return doesChunkIntersectSlice(Level.OVERWORLD, chunkPos, worldThickness);
    }

    public static boolean doesChunkIntersectSlice(ResourceKey<Level> dimension, ChunkPos chunkPos, int worldThickness) {
        SliceBounds bounds = forDimension(dimension, worldThickness);
        return chunkPos.getMaxBlockX() >= bounds.minX() && chunkPos.getMinBlockX() <= bounds.maxX();
    }

    /** Whether every block column in the ChunkPos is inside the slice. */
    public static boolean isChunkFullyInsideSlice(ChunkPos chunkPos) {
        return isChunkFullyInsideSlice(chunkPos, WorldSliceWorldSettings.DEFAULT_WORLD_THICKNESS);
    }

    public static boolean isChunkFullyInsideSlice(ChunkPos chunkPos, int worldThickness) {
        return isChunkFullyInsideSlice(Level.OVERWORLD, chunkPos, worldThickness);
    }

    public static boolean isChunkFullyInsideSlice(ResourceKey<Level> dimension, ChunkPos chunkPos, int worldThickness) {
        SliceBounds bounds = forDimension(dimension, worldThickness);
        return chunkPos.getMinBlockX() >= bounds.minX() && chunkPos.getMaxBlockX() <= bounds.maxX();
    }

    public static boolean isPartialBoundaryChunk(ChunkPos chunkPos) {
        return isPartialBoundaryChunk(chunkPos, WorldSliceWorldSettings.DEFAULT_WORLD_THICKNESS);
    }

    public static boolean isPartialBoundaryChunk(ChunkPos chunkPos, int worldThickness) {
        return isPartialBoundaryChunk(Level.OVERWORLD, chunkPos, worldThickness);
    }

    public static boolean isPartialBoundaryChunk(ResourceKey<Level> dimension, ChunkPos chunkPos, int worldThickness) {
        return doesChunkIntersectSlice(dimension, chunkPos, worldThickness)
            && !isChunkFullyInsideSlice(dimension, chunkPos, worldThickness);
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

    public static boolean isWorldSliceLevel(Level level) {
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

    /** Clamps a block X coordinate into the playable slice columns. */
    public static int clampBlockX(int x, int worldThickness) {
        return clampBlockX(Level.OVERWORLD, x, worldThickness);
    }

    public static int clampBlockX(Level level, int x) {
        return clampBlockX(level.dimension(), x, thickness(level));
    }

    public static int clampBlockX(ResourceKey<Level> dimension, int x, int worldThickness) {
        SliceBounds bounds = forDimension(dimension, worldThickness);
        return Math.max(bounds.minX(), Math.min(bounds.maxX(), x));
    }

    /**
     * Clears only the invalid columns of a boundary chunk after vanilla
     * generation has completed for the current status. This is intentionally
     * limited to the one or two intersecting partial chunks; fully external
     * chunks never enter the parent generator's terrain pipeline.
     */
    public static void trimChunkToSlice(ResourceKey<Level> dimension, ChunkAccess chunk, int worldThickness) {
        if (!isPartialBoundaryChunk(dimension, chunk.getPos(), worldThickness)) {
            return;
        }

        int thickness = WorldSliceWorldSettings.sanitize(worldThickness);
        int chunkMinX = chunk.getPos().getMinBlockX();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; localX++) {
            int x = chunkMinX + localX;
            if (isInsideX(dimension, x, thickness)) {
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
            .filter(pos -> !isInsideX(dimension, pos.getX(), thickness))
            .toList()) {
            chunk.removeBlockEntity(pos);
        }

        if (chunk instanceof ProtoChunk protoChunk) {
            protoChunk.getEntities().removeIf(entityTag -> {
                ListTag position = entityTag.getList("Pos", Tag.TAG_DOUBLE);
                return !position.isEmpty() && !isInsideX(dimension, position.getDouble(0), thickness);
            });
        }
        chunk.setUnsaved(true);
    }

    // ---------------------------------------------------------------------
    // Entity boundary collision (players and living entities)
    // ---------------------------------------------------------------------

    /** The left virtual collision plane, at the outside face of the slice's first column. */
    public static double minPlayerX() {
        return 0.0D;
    }

    public static double minPlayerX(Level level) {
        return forLevel(level).minX();
    }

    /** The right virtual collision plane, immediately after the last valid block column. */
    public static double maxPlayerX() {
        return maxPlayerX(Level.OVERWORLD, WorldSliceWorldSettings.DEFAULT_WORLD_THICKNESS);
    }

    public static double maxPlayerX(Level level) {
        return forLevel(level).maxX() + 1.0D;
    }

    public static double maxPlayerX(int worldThickness) {
        return maxPlayerX(Level.OVERWORLD, worldThickness);
    }

    public static double maxPlayerX(ResourceKey<Level> dimension, int worldThickness) {
        return forDimension(dimension, worldThickness).maxX() + 1.0D;
    }

    /** Returns whether an entity's complete bounding box is inside the entity boundary interval. */
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
        return clampPlayerX(Level.OVERWORLD, x, WorldSliceWorldSettings.DEFAULT_WORLD_THICKNESS);
    }

    public static double clampPlayerX(double x, int worldThickness) {
        return clampPlayerX(Level.OVERWORLD, x, worldThickness);
    }

    public static double clampPlayerX(Level level, double x) {
        return clampPlayerX(level.dimension(), x, thickness(level));
    }

    public static double clampPlayerX(ResourceKey<Level> dimension, double x, int worldThickness) {
        SliceBounds bounds = forDimension(dimension, worldThickness);
        double min = bounds.minX() + PLAYER_SAFETY_EPSILON;
        double max = bounds.maxX() + 1.0D - PLAYER_SAFETY_EPSILON;
        return Math.max(min, Math.min(max, x));
    }

    /** Clamps an entity center while accounting for its actual width. */
    public static double clampPlayerX(Entity entity, double x) {
        return clampPlayerX(entity.level(), entity, x);
    }

    public static double clampPlayerX(Level level, Entity entity, double x) {
        SliceBounds bounds = forLevel(level);
        double halfWidth = entity.getBbWidth() * 0.5D;
        double min = bounds.minX() + halfWidth + PLAYER_SAFETY_EPSILON;
        double max = bounds.maxX() + 1.0D - halfWidth - PLAYER_SAFETY_EPSILON;
        if (min > max) {
            return (bounds.minX() + bounds.maxX() + 1.0D) * 0.5D;
        }
        return Math.max(min, Math.min(max, x));
    }

    /**
     * Whether this entity should receive the virtual World Slice boundary walls.
     *
     * <p>The boundary protects players and every ordinary living entity (mobs,
     * animals, villagers, golems and other bosses), but explicitly lets the
     * Ender Dragon pass so its vanilla fight can fly through the full 3D
     * arena. Non-living entities (items, experience orbs, projectiles, boats,
     * minecarts, End Crystals, ...) never receive the shapes.</p>
     */
    public static boolean affectsBoundaryCollision(Entity entity, Level level) {
        if (entity == null || !isWorldSliceLevel(level)) {
            return false;
        }
        if (entity.isSpectator()) {
            return false;
        }
        // The dragon needs the full 3D space for its fight; never constrain it.
        if (entity instanceof EnderDragon) {
            return false;
        }
        // Player extends LivingEntity, so this single check covers players,
        // mobs, animals, villagers, golems and bosses such as the Wither.
        return entity instanceof LivingEntity;
    }

    /** Adds two finite-Z virtual side walls to an existing collision result. */
    public static List<VoxelShape> addBoundaryCollisionWalls(
        List<VoxelShape> collisions, Level level, AABB collisionQuery
    ) {
        return addBoundaryCollisionWalls(
            collisions,
            collisionQuery,
            level.getMinBuildHeight(),
            level.getMaxBuildHeight(),
            level.dimension(),
            thickness(level)
        );
    }

    /** Context-free (Overworld) helper retained for unit tests. */
    static List<VoxelShape> addBoundaryCollisionWalls(
        List<VoxelShape> collisions, AABB collisionQuery, int minBuildHeight, int maxBuildHeight
    ) {
        return addBoundaryCollisionWalls(
            collisions,
            collisionQuery,
            minBuildHeight,
            maxBuildHeight,
            Level.OVERWORLD,
            WorldSliceWorldSettings.DEFAULT_WORLD_THICKNESS
        );
    }

    private static List<VoxelShape> addBoundaryCollisionWalls(
        List<VoxelShape> collisions,
        AABB collisionQuery,
        int minBuildHeight,
        int maxBuildHeight,
        ResourceKey<Level> dimension,
        int worldThickness
    ) {
        SliceBounds bounds = forDimension(dimension, worldThickness);
        double minPlayerX = bounds.minX();
        double maxPlayerX = bounds.maxX() + 1.0D;
        if (collisionQuery.minX > minPlayerX + COLLISION_QUERY_MARGIN
            && collisionQuery.maxX < maxPlayerX - COLLISION_QUERY_MARGIN) {
            return collisions;
        }

        ImmutableList.Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(collisions.size() + 2);
        builder.addAll(collisions);
        builder.add(Shapes.create(new AABB(
            minPlayerX - 1.0D,
            minBuildHeight,
            collisionQuery.minZ,
            minPlayerX,
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

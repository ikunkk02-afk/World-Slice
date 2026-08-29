package com.shouyun.worldslice;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.google.common.collect.ImmutableList;

import java.util.List;

/** Central definition of the playable block and chunk bounds. */
public final class WorldSliceBounds {
    private static final int MIN_X = 0;
    private static final int MAX_X = 15;
    private static final int THICKNESS = MAX_X - MIN_X + 1;
    private static final double MIN_PLAYER_X = MIN_X;
    private static final double MAX_PLAYER_X = MAX_X + 1.0D;
    private static final double PLAYER_SAFETY_EPSILON = 1.0E-4D;
    private static final double COLLISION_QUERY_MARGIN = 1.0D;

    private WorldSliceBounds() {
    }

    public static boolean isInside(BlockPos pos) {
        return isInsideX(pos.getX());
    }

    public static boolean isInsideX(int x) {
        return x >= MIN_X && x <= MAX_X;
    }

    public static boolean isValidChunk(ChunkPos chunkPos) {
        return isInsideX(chunkPos.getMinBlockX()) && isInsideX(chunkPos.getMaxBlockX());
    }

    public static boolean canFluidEnter(BlockPos pos) {
        return isInside(pos);
    }

    public static boolean isWorldSliceLevel(Level level) {
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return false;
        }

        // The client does not retain the server's ChunkGenerator. Every
        // Overworld served by this mod is a slice, so use the dimension as
        // the client-side mirror of the server generator check. This keeps
        // the client collision result identical to the server result.
        return level.isClientSide
            || level.getChunkSource() instanceof ServerChunkCache serverChunkCache
                && serverChunkCache.getGenerator() instanceof WorldSliceGenerator;
    }

    public static boolean isWorldSliceLevel(BlockGetter level) {
        return level instanceof Level actualLevel && isWorldSliceLevel(actualLevel);
    }

    public static int minX() {
        return MIN_X;
    }

    public static int maxX() {
        return MAX_X;
    }

    /** The left virtual collision plane, at the outside face of block X=0. */
    public static double minPlayerX() {
        return MIN_PLAYER_X;
    }

    /** The right virtual collision plane, at the outside face of block X=15. */
    public static double maxPlayerX() {
        return MAX_PLAYER_X;
    }

    /**
     * Returns whether an entity's complete bounding box is inside the player
     * collision interval. Touching either plane is valid; crossing it is not.
     */
    public static boolean isPlayerInside(Entity entity) {
        AABB box = entity.getBoundingBox();
        return box.minX >= minPlayerX() - PLAYER_SAFETY_EPSILON
            && box.maxX <= maxPlayerX() + PLAYER_SAFETY_EPSILON;
    }

    /** Clamps a point to the open player interval with a small safety margin. */
    public static double clampPlayerX(double x) {
        return Math.max(
            minPlayerX() + PLAYER_SAFETY_EPSILON,
            Math.min(maxPlayerX() - PLAYER_SAFETY_EPSILON, x)
        );
    }

    /**
     * Clamps an entity center while accounting for the entity's actual width,
     * so a teleport cannot leave part of a player outside the slice.
     */
    public static double clampPlayerX(Entity entity, double x) {
        double halfWidth = entity.getBbWidth() * 0.5D;
        double min = minPlayerX() + halfWidth + PLAYER_SAFETY_EPSILON;
        double max = maxPlayerX() - halfWidth - PLAYER_SAFETY_EPSILON;
        return Math.max(min, Math.min(max, x));
    }

    /**
     * Returns whether this entity should receive the virtual player walls.
     * Spectators intentionally bypass the walls through the normal no-clip
     * game-mode behavior.
     */
    public static boolean affectsPlayerCollision(Entity entity, Level level) {
        return entity instanceof Player
            && !entity.isSpectator()
            && isWorldSliceLevel(level);
    }

    /**
     * Adds the two virtual side walls to an existing normal collision result.
     * The Z interval is limited to the current collision query, so no infinite
     * AABB or persistent world data is created.
     */
    public static List<VoxelShape> addPlayerCollisionWalls(
        List<VoxelShape> collisions, Level level, AABB collisionQuery
    ) {
        return addPlayerCollisionWalls(collisions, collisionQuery, level.getMinBuildHeight(), level.getMaxBuildHeight());
    }

    static List<VoxelShape> addPlayerCollisionWalls(
        List<VoxelShape> collisions, AABB collisionQuery, int minBuildHeight, int maxBuildHeight
    ) {
        if (collisionQuery.minX > minPlayerX() + COLLISION_QUERY_MARGIN
            && collisionQuery.maxX < maxPlayerX() - COLLISION_QUERY_MARGIN) {
            return collisions;
        }

        ImmutableList.Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(collisions.size() + 2);
        builder.addAll(collisions);
        builder.add(Shapes.create(new AABB(
            minPlayerX() - 1.0D,
            minBuildHeight,
            collisionQuery.minZ,
            minPlayerX(),
            maxBuildHeight,
            collisionQuery.maxZ
        )));
        builder.add(Shapes.create(new AABB(
            maxPlayerX(),
            minBuildHeight,
            collisionQuery.minZ,
            maxPlayerX() + 1.0D,
            maxBuildHeight,
            collisionQuery.maxZ
        )));
        return builder.build();
    }

    public static int thickness() {
        return THICKNESS;
    }
}

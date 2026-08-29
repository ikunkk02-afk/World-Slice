package com.shouyun.worldslice;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/** Central definition of the playable block and chunk bounds. */
public final class WorldSliceBounds {
    private static final int MIN_X = 0;
    private static final int MAX_X = 15;
    private static final int THICKNESS = MAX_X - MIN_X + 1;

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
        return !level.isClientSide
            && level.dimension().equals(Level.OVERWORLD)
            && level.getChunkSource() instanceof ServerChunkCache serverChunkCache
            && serverChunkCache.getGenerator() instanceof WorldSliceChunkGenerator;
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

    public static int thickness() {
        return THICKNESS;
    }
}

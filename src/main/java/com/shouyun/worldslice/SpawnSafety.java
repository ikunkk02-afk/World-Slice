package com.shouyun.worldslice;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

public final class SpawnSafety {
    private static final int[] PREFERRED_X = {7, 8, 6, 9, 5, 10, 4, 11, 3, 12, 2, 13, 1, 14, 0, 15};
    private static final int[] Z_OFFSETS = {0, 1, -1, 2, -2, 3, -3, 4, -4};

    private SpawnSafety() {
    }

    public static BlockPos findSafeSpawn(ServerLevel level) {
        int minY = level.getMinBuildHeight() + 1;
        int maxY = level.getMaxBuildHeight() - 2;

        for (int x : PREFERRED_X) {
            for (int zOffset : Z_OFFSETS) {
                int y = Math.min(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, zOffset), maxY);
                for (; y >= minY; y--) {
                    BlockPos feet = new BlockPos(x, y, zOffset);
                    BlockPos floor = feet.below();
                    if (isSafe(level, feet, floor)) {
                        return feet;
                    }
                }
            }
        }

        return new BlockPos(8, level.getSeaLevel() + 1, 0);
    }

    private static boolean isSafe(ServerLevel level, BlockPos feet, BlockPos floor) {
        return level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
            && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
            && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
            && level.getFluidState(feet).isEmpty()
            && level.getFluidState(feet.above()).isEmpty();
    }
}

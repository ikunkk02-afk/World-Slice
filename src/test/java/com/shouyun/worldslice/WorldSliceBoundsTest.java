package com.shouyun.worldslice;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Test;

class WorldSliceBoundsTest {
    @Test
    void blockBoundsAreExactlyOneChunkThick() {
        assertTrue(WorldSliceBounds.isInside(new BlockPos(0, 64, 0)));
        assertTrue(WorldSliceBounds.isInside(new BlockPos(15, -64, 0)));
        assertFalse(WorldSliceBounds.isInside(new BlockPos(-1, 64, 0)));
        assertFalse(WorldSliceBounds.isInside(new BlockPos(16, 64, 0)));
        assertEquals(16, WorldSliceBounds.thickness());
    }

    @Test
    void onlyChunkColumnZeroIsPlayable() {
        assertTrue(WorldSliceBounds.isValidChunk(new ChunkPos(0, 123)));
        assertFalse(WorldSliceBounds.isValidChunk(new ChunkPos(-1, 123)));
        assertFalse(WorldSliceBounds.isValidChunk(new ChunkPos(1, 123)));
    }

    @Test
    void playerCollisionBoundsUseTheOutsideFacesOfTheSlice() {
        assertEquals(0.0D, WorldSliceBounds.minPlayerX());
        assertEquals(16.0D, WorldSliceBounds.maxPlayerX());
        assertEquals(1.0E-4D, WorldSliceBounds.clampPlayerX(-100.0D), 1.0E-9D);
        assertEquals(15.9999D, WorldSliceBounds.clampPlayerX(100.0D), 1.0E-9D);
    }

    @Test
    void virtualWallsAreFiniteInZAndCoverBuildHeight() {
        List<VoxelShape> walls = WorldSliceBounds.addPlayerCollisionWalls(
            List.of(),
            new AABB(-2.0D, 0.0D, -4.0D, 18.0D, 2.0D, 6.0D),
            -64,
            320
        );

        assertEquals(2, walls.size());
        assertEquals(0.0D, walls.get(0).bounds().maxX);
        assertEquals(16.0D, walls.get(1).bounds().minX);
        assertEquals(-64.0D, walls.get(0).bounds().minY);
        assertEquals(320.0D, walls.get(0).bounds().maxY);
        assertEquals(-4.0D, walls.get(0).bounds().minZ);
        assertEquals(6.0D, walls.get(0).bounds().maxZ);
    }
}

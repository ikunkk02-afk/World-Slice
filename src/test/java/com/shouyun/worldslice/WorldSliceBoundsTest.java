package com.shouyun.worldslice;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
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

    @Test
    void partialChunkRulesUseBlockThicknessWithoutRounding() {
        assertTrue(WorldSliceBounds.isInsideBlockX(0, 1));
        assertFalse(WorldSliceBounds.isInsideBlockX(1, 1));
        assertTrue(WorldSliceBounds.isPartialBoundaryChunk(new ChunkPos(0, 0), 1));
        assertFalse(WorldSliceBounds.isChunkFullyInsideSlice(new ChunkPos(0, 0), 1));

        assertTrue(WorldSliceBounds.isChunkFullyInsideSlice(new ChunkPos(0, 0), 16));
        assertTrue(WorldSliceBounds.isPartialBoundaryChunk(new ChunkPos(1, 0), 17));
        assertTrue(WorldSliceBounds.isInsideBlockX(16, 17));
        assertFalse(WorldSliceBounds.isInsideBlockX(17, 17));
        assertEquals(2, WorldSliceBounds.chunkWidth(17));
    }

    @Test
    void chunkIntersectionCoversOnlyTheConfiguredBlockColumns() {
        assertFalse(WorldSliceBounds.doesChunkIntersectSlice(new ChunkPos(-1, 0), 1));
        assertTrue(WorldSliceBounds.doesChunkIntersectSlice(new ChunkPos(0, 0), 1));
        assertTrue(WorldSliceBounds.doesChunkIntersectSlice(new ChunkPos(1, 0), 33));
        assertTrue(WorldSliceBounds.doesChunkIntersectSlice(new ChunkPos(2, 0), 33));
        assertFalse(WorldSliceBounds.doesChunkIntersectSlice(new ChunkPos(3, 0), 33));
        assertEquals(3, WorldSliceBounds.chunkWidth(33));
    }

    @Test
    void supportedThicknessMatrixKeepsExactBlockAndChunkSemantics() {
        for (int thickness : new int[] {1, 2, 7, 15, 16, 17, 31, 32, 33, 64}) {
            assertTrue(WorldSliceBounds.isInsideBlockX(thickness - 1, thickness));
            assertFalse(WorldSliceBounds.isInsideBlockX(thickness, thickness));
            assertEquals(thickness - 1, WorldSliceBounds.maxX(thickness));
            assertEquals((thickness + 15) / 16, WorldSliceBounds.chunkWidth(thickness));

            int lastChunk = WorldSliceBounds.chunkWidth(thickness) - 1;
            assertTrue(WorldSliceBounds.doesChunkIntersectSlice(new ChunkPos(lastChunk, 0), thickness));
            assertEquals(thickness % 16 != 0,
                WorldSliceBounds.isPartialBoundaryChunk(new ChunkPos(lastChunk, 0), thickness));
        }
    }

    @Test
    void clampBlockXStaysInsideTheSlice() {
        assertEquals(0, WorldSliceBounds.clampBlockX(-5, 16));
        assertEquals(15, WorldSliceBounds.clampBlockX(100, 16));
        assertEquals(7, WorldSliceBounds.clampBlockX(7, 16));
        assertEquals(0, WorldSliceBounds.clampBlockX(0, 1));
        assertEquals(0, WorldSliceBounds.clampBlockX(56, 1));
    }

    @Test
    void supportedDimensionsCoverTheThreeVanillaDimensions() {
        assertTrue(WorldSliceBounds.isSupportedDimension(Level.OVERWORLD));
        assertTrue(WorldSliceBounds.isSupportedDimension(Level.NETHER));
        assertTrue(WorldSliceBounds.isSupportedDimension(Level.END));
    }

    @Test
    void endSliceIsCenteredOnVanillaOrigin() {
        assertEquals(0, WorldSliceBounds.minX(Level.END, 1));
        assertEquals(0, WorldSliceBounds.maxX(Level.END, 1));

        assertEquals(-1, WorldSliceBounds.minX(Level.END, 3));
        assertEquals(1, WorldSliceBounds.maxX(Level.END, 3));

        assertEquals(-2, WorldSliceBounds.minX(Level.END, 5));
        assertEquals(2, WorldSliceBounds.maxX(Level.END, 5));

        assertEquals(-8, WorldSliceBounds.minX(Level.END, 16));
        assertEquals(7, WorldSliceBounds.maxX(Level.END, 16));

        assertEquals(-8, WorldSliceBounds.minX(Level.END, 17));
        assertEquals(8, WorldSliceBounds.maxX(Level.END, 17));

        assertEquals(0, WorldSliceBounds.centerX(Level.END, 16));
        assertEquals(0, WorldSliceBounds.centerX(Level.END, 17));
    }

    @Test
    void endSliceWidthIsExactAndAlwaysContainsOrigin() {
        for (int thickness : new int[] {1, 2, 7, 15, 16, 17, 31, 32, 33, 64}) {
            SliceBounds bounds = WorldSliceBounds.forDimension(Level.END, thickness);
            assertEquals(thickness, bounds.thickness());
            assertTrue(bounds.contains(0), "X=0 must stay inside an End slice");
            assertEquals(0, bounds.centerX());
        }
    }

    @Test
    void overworldAndNetherKeepZeroOrigin() {
        for (int thickness : new int[] {1, 2, 7, 16, 17, 33}) {
            assertEquals(0, WorldSliceBounds.minX(Level.OVERWORLD, thickness));
            assertEquals(thickness - 1, WorldSliceBounds.maxX(Level.OVERWORLD, thickness));
            assertEquals(0, WorldSliceBounds.minX(Level.NETHER, thickness));
            assertEquals(thickness - 1, WorldSliceBounds.maxX(Level.NETHER, thickness));
        }
    }
}

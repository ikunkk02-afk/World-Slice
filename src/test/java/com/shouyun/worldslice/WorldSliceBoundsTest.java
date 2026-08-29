package com.shouyun.worldslice;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
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
}

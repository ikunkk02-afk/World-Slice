package com.shouyun.worldslice;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;

/** Marker shared by World Slice generator wrappers. */
public interface WorldSliceGenerator {
    ChunkGenerator parent();

    /** Returns the live Overworld setting used by this generator instance. */
    int worldThickness();

    /** The vanilla dimension this wrapper belongs to, used for slice origin. */
    ResourceKey<Level> dimension();
}

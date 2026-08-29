package com.shouyun.worldslice;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent marker and result for the one-time World Slice spawn setup. */
public final class WorldSliceSpawnData extends SavedData {
    private static final String FILE_ID = "worldslice_spawn";
    private static final String INITIALIZED_TAG = "worldsliceSpawnInitialized";
    private static final String SPAWN_X_TAG = "spawnX";
    private static final String SPAWN_Y_TAG = "spawnY";
    private static final String SPAWN_Z_TAG = "spawnZ";

    private static final Factory<WorldSliceSpawnData> FACTORY = new Factory<>(
        WorldSliceSpawnData::new,
        WorldSliceSpawnData::load
    );

    private boolean initialized;
    private BlockPos spawnPos;

    public static WorldSliceSpawnData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    private static WorldSliceSpawnData load(CompoundTag tag, HolderLookup.Provider registries) {
        WorldSliceSpawnData data = new WorldSliceSpawnData();
        data.initialized = tag.getBoolean(INITIALIZED_TAG);
        if (data.initialized && tag.contains(SPAWN_X_TAG) && tag.contains(SPAWN_Y_TAG) && tag.contains(SPAWN_Z_TAG)) {
            data.spawnPos = new BlockPos(tag.getInt(SPAWN_X_TAG), tag.getInt(SPAWN_Y_TAG), tag.getInt(SPAWN_Z_TAG));
        }
        return data;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public BlockPos spawnPos() {
        return spawnPos;
    }

    public void markInitialized(BlockPos spawnPos) {
        this.initialized = true;
        this.spawnPos = spawnPos.immutable();
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean(INITIALIZED_TAG, initialized);
        if (spawnPos != null) {
            tag.putInt(SPAWN_X_TAG, spawnPos.getX());
            tag.putInt(SPAWN_Y_TAG, spawnPos.getY());
            tag.putInt(SPAWN_Z_TAG, spawnPos.getZ());
        }
        return tag;
    }
}

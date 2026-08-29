package com.shouyun.worldslice;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.Level;

/**
 * The authoritative per-world World Slice settings.
 *
 * <p>This data is stored in the Overworld's data storage, so it survives
 * restarts and is shared by every player on a server. The client mirror below
 * is intentionally only a transient view used by client rendering and UI;
 * it is never used as the server's source of truth.</p>
 */
public final class WorldSliceWorldSettings extends SavedData {
    public static final int DEFAULT_WORLD_THICKNESS = 16;
    public static final int MIN_WORLD_THICKNESS = 1;
    public static final int MAX_WORLD_THICKNESS = 4096;

    private static final String FILE_ID = "worldslice_settings";
    private static final String WORLD_THICKNESS_TAG = "worldThickness";

    private static final Factory<WorldSliceWorldSettings> FACTORY = new Factory<>(
        WorldSliceWorldSettings::new,
        WorldSliceWorldSettings::load
    );

    private static volatile int clientWorldThickness = DEFAULT_WORLD_THICKNESS;
    private static volatile boolean clientWorldSliceActive;
    private static volatile long clientSettingsRevision;

    private volatile int worldThickness;

    private WorldSliceWorldSettings() {
        // A brand new world starts from the user's configured default. Only
        // this first initialization reads the default; afterwards the value is
        // persisted per save and never changes when the default is edited.
        this.worldThickness = WorldSliceDefaultsConfig.defaultWorldThickness();
        // A newly created setting must be written even before the first UI
        // change, so old worlds receive an explicit default SavedData file.
        setDirty();
    }

    /** Resolves the Overworld level that owns the shared World Slice settings. */
    public static ServerLevel getStorageLevel(ServerLevel level) {
        ServerLevel storageLevel = level;
        if (!level.dimension().equals(Level.OVERWORLD) && level.getServer().overworld() != null) {
            storageLevel = level.getServer().overworld();
        }
        return storageLevel;
    }

    public static WorldSliceWorldSettings get(ServerLevel level) {
        // Always bind this setting to the Overworld save, even if a future
        // caller reaches this helper while handling another dimension.
        return getStorageLevel(level).getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    private static WorldSliceWorldSettings load(CompoundTag tag, HolderLookup.Provider registries) {
        WorldSliceWorldSettings settings = new WorldSliceWorldSettings();
        settings.worldThickness = tag.contains(WORLD_THICKNESS_TAG)
            ? sanitize(tag.getInt(WORLD_THICKNESS_TAG))
            : WorldSliceDefaultsConfig.defaultWorldThickness();
        return settings;
    }

    public static int sanitize(int thickness) {
        return Math.max(MIN_WORLD_THICKNESS, Math.min(MAX_WORLD_THICKNESS, thickness));
    }

    public int worldThickness() {
        return worldThickness;
    }

    public boolean setWorldThickness(int thickness) {
        int sanitized = sanitize(thickness);
        if (this.worldThickness == sanitized) {
            return false;
        }

        this.worldThickness = sanitized;
        setDirty();
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt(WORLD_THICKNESS_TAG, worldThickness);
        return tag;
    }

    /** Returns the last server-provided value on the logical client. */
    public static int clientWorldThickness() {
        return clientWorldThickness;
    }

    public static boolean isClientWorldSliceActive() {
        return clientWorldSliceActive;
    }

    public static long clientSettingsRevision() {
        return clientSettingsRevision;
    }

    /** Applies an authoritative server update to the client-only mirror. */
    public static void applyClientWorldThickness(int thickness) {
        clientWorldThickness = sanitize(thickness);
        clientWorldSliceActive = true;
        clientSettingsRevision++;
    }

    /** Clears stale state when the client leaves a World Slice server. */
    public static void clearClientWorldSettings() {
        clientWorldThickness = DEFAULT_WORLD_THICKNESS;
        clientWorldSliceActive = false;
        clientSettingsRevision++;
    }
}

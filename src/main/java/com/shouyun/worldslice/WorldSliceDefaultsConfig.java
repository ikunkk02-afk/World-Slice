package com.shouyun.worldslice;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client-editable defaults that are not tied to any individual world save.
 *
 * <p>The value stored here is read only when a brand new World Slice world
 * initializes its per-world {@link WorldSliceWorldSettings} for the first
 * time. Worlds that already exist keep their own saved thickness.</p>
 */
public final class WorldSliceDefaultsConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue DEFAULT_WORLD_THICKNESS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        DEFAULT_WORLD_THICKNESS = builder
            .comment(
                "Default world thickness used when a brand new World Slice world is created.",
                "Existing worlds keep their own per-save world thickness and are not affected by this value."
            )
            .translation("worldslice.configuration.default_world_thickness")
            .defineInRange(
                "defaultWorldThickness",
                WorldSliceWorldSettings.DEFAULT_WORLD_THICKNESS,
                WorldSliceWorldSettings.MIN_WORLD_THICKNESS,
                WorldSliceWorldSettings.MAX_WORLD_THICKNESS
            );
        SPEC = builder.build();
    }

    private WorldSliceDefaultsConfig() {
    }

    /** The current default thickness, clamped to the supported range. */
    public static int defaultWorldThickness() {
        try {
            return WorldSliceWorldSettings.sanitize(DEFAULT_WORLD_THICKNESS.get());
        } catch (IllegalStateException exception) {
            // The config is not loaded yet (for example during very early
            // static initialization). Fall back to the compile-time default.
            return WorldSliceWorldSettings.DEFAULT_WORLD_THICKNESS;
        }
    }

    /** Stores and persists a new default thickness. */
    public static void setDefaultWorldThickness(int thickness) {
        DEFAULT_WORLD_THICKNESS.set(WorldSliceWorldSettings.sanitize(thickness));
        SPEC.save();
    }
}

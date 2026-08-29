package com.shouyun.worldslice;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SideCameraConfig {
    public static final int DEFAULT_CAMERA_DISTANCE = 28;
    public static final int MIN_CAMERA_DISTANCE = 8;
    public static final int MAX_CAMERA_DISTANCE = 64;
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue CAMERA_DISTANCE;
    public static final double CAMERA_FOV = 30.0D;
    public static final float CAMERA_YAW = -90.0F;
    public static final float CAMERA_PITCH = 0.0F;
    public static final float CAMERA_ROLL = 0.0F;

    /** Scales raw relative mouse movement before it is converted to GUI pixels. */
    public static final double CROSSHAIR_MOUSE_SCALE = 1.0D;

    /** Prevents a cursor exactly on the player projection from producing an invalid aim vector. */
    public static final double AIM_EPSILON = 1.0E-6D;

    private static int previewDistance = DEFAULT_CAMERA_DISTANCE;
    private static boolean previewActive;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        CAMERA_DISTANCE = builder
            .comment("Distance of the Terraria-style side camera in blocks.")
            .translation("worldslice.configuration.camera_distance")
            .defineInRange("cameraDistance", DEFAULT_CAMERA_DISTANCE, MIN_CAMERA_DISTANCE, MAX_CAMERA_DISTANCE);
        SPEC = builder.build();
    }

    private SideCameraConfig() {
    }

    public static int cameraDistance() {
        if (previewActive) {
            return previewDistance;
        }

        try {
            return CAMERA_DISTANCE.get();
        } catch (IllegalStateException exception) {
            return DEFAULT_CAMERA_DISTANCE;
        }
    }

    public static void beginPreview() {
        previewDistance = configuredDistance();
        previewActive = true;
    }

    public static void previewCameraDistance(int distance) {
        previewDistance = clampCameraDistance(distance);
    }

    public static void commitPreview(int distance) {
        int committedDistance = clampCameraDistance(distance);
        CAMERA_DISTANCE.set(committedDistance);
        SPEC.save();
        previewDistance = committedDistance;
        previewActive = false;
    }

    public static void cancelPreview() {
        previewDistance = configuredDistance();
        previewActive = false;
    }

    public static int clampCameraDistance(int distance) {
        return Math.max(MIN_CAMERA_DISTANCE, Math.min(MAX_CAMERA_DISTANCE, distance));
    }

    private static int configuredDistance() {
        try {
            return clampCameraDistance(CAMERA_DISTANCE.get());
        } catch (IllegalStateException exception) {
            return DEFAULT_CAMERA_DISTANCE;
        }
    }
}

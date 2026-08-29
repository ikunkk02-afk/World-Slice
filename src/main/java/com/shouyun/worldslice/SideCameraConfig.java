package com.shouyun.worldslice;

public final class SideCameraConfig {
    public static final double CAMERA_DISTANCE = 28.0D;
    public static final double CAMERA_FOV = 30.0D;
    public static final float CAMERA_YAW = -90.0F;
    public static final float CAMERA_PITCH = 0.0F;
    public static final float CAMERA_ROLL = 0.0F;

    /** Scales raw relative mouse movement before it is converted to GUI pixels. */
    public static final double CROSSHAIR_MOUSE_SCALE = 1.0D;

    /** Prevents a cursor exactly on the player projection from producing an invalid aim vector. */
    public static final double AIM_EPSILON = 1.0E-6D;

    private SideCameraConfig() {
    }
}

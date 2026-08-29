package com.shouyun.worldslice;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Keeps player aim independent from the fixed side-camera orientation. */
@EventBusSubscriber(modid = WorldSlice.MODID, value = Dist.CLIENT)
public final class SideAimController {
    private static final Vec3 DEFAULT_AIM_DIRECTION = new Vec3(1.0D, 0.0D, 0.0D);

    private static double virtualCrosshairX;
    private static double virtualCrosshairY;
    private static Vec3 lastAimDirection = DEFAULT_AIM_DIRECTION;
    private static int lastGuiWidth = -1;
    private static int lastGuiHeight = -1;
    private static int depthSign = 1;
    private static boolean initialized;

    private SideAimController() {
    }

    public static void enable(Minecraft minecraft) {
        initialized = false;
        depthSign = 1;
        syncViewport(minecraft, true);

        LocalPlayer player = minecraft.player;
        if (player != null) {
            lastAimDirection = SideAimMath.normalizeOrFallback(player.getViewVector(1.0F), DEFAULT_AIM_DIRECTION);
            applyAimDirection(player, lastAimDirection);
        } else {
            lastAimDirection = DEFAULT_AIM_DIRECTION;
        }
    }

    public static void disable() {
        initialized = false;
        lastGuiWidth = -1;
        lastGuiHeight = -1;
        depthSign = 1;
    }

    public static void toggleDepth() {
        depthSign = -depthSign;
        updatePlayerAim(Minecraft.getInstance());
    }

    /**
     * Called by the MouseHandler mixin while the mouse is still grabbed. Returning true tells the mixin
     * to suppress vanilla camera/player turning for this movement sample.
     */
    public static boolean handleMouseMovement() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!SideCameraController.isEnabled() || !isWorldInputActive(minecraft)) {
            return false;
        }

        syncViewport(minecraft, false);
        if (lastGuiWidth <= 0 || lastGuiHeight <= 0) {
            return true;
        }

        double movementX = minecraft.mouseHandler.getXVelocity();
        double movementY = minecraft.mouseHandler.getYVelocity();
        double guiScaleX = guiScaleX(minecraft);
        double guiScaleY = guiScaleY(minecraft);
        double mouseScale = mouseScale(minecraft);

        virtualCrosshairX = Mth.clamp(
            virtualCrosshairX + movementX * mouseScale * guiScaleX,
            0.0D,
            lastGuiWidth
        );
        virtualCrosshairY = Mth.clamp(
            virtualCrosshairY + movementY * mouseScale * guiScaleY,
            0.0D,
            lastGuiHeight
        );
        updatePlayerAim(minecraft);
        return true;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!SideCameraController.isEnabled()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        syncViewport(minecraft, false);
        if (minecraft.screen != null || minecraft.getOverlay() != null || !minecraft.mouseHandler.isMouseGrabbed()) {
            return;
        }

        // Re-evaluate against the current camera projection so movement, resize, or camera clipping cannot
        // desynchronise an absolute virtual cursor from the player's projected screen position.
        updatePlayerAim(minecraft);
    }

    public static double getVirtualCrosshairX() {
        return virtualCrosshairX;
    }

    public static double getVirtualCrosshairY() {
        return virtualCrosshairY;
    }

    private static boolean isWorldInputActive(Minecraft minecraft) {
        return minecraft.player != null
            && minecraft.level != null
            && minecraft.screen == null
            && minecraft.getOverlay() == null
            && minecraft.mouseHandler.isMouseGrabbed();
    }

    private static void syncViewport(Minecraft minecraft, boolean reset) {
        int guiWidth = minecraft.getWindow().getGuiScaledWidth();
        int guiHeight = minecraft.getWindow().getGuiScaledHeight();
        if (guiWidth <= 0 || guiHeight <= 0) {
            return;
        }

        if (reset || !initialized || lastGuiWidth <= 0 || lastGuiHeight <= 0) {
            virtualCrosshairX = guiWidth * 0.5D;
            virtualCrosshairY = guiHeight * 0.5D;
        } else if (guiWidth != lastGuiWidth || guiHeight != lastGuiHeight) {
            virtualCrosshairX *= (double)guiWidth / lastGuiWidth;
            virtualCrosshairY *= (double)guiHeight / lastGuiHeight;
        }

        lastGuiWidth = guiWidth;
        lastGuiHeight = guiHeight;
        virtualCrosshairX = Mth.clamp(virtualCrosshairX, 0.0D, guiWidth);
        virtualCrosshairY = Mth.clamp(virtualCrosshairY, 0.0D, guiHeight);
        initialized = true;
    }

    private static double guiScaleX(Minecraft minecraft) {
        int screenWidth = minecraft.getWindow().getScreenWidth();
        return screenWidth > 0 ? (double)lastGuiWidth / screenWidth : 1.0D;
    }

    private static double guiScaleY(Minecraft minecraft) {
        int screenHeight = minecraft.getWindow().getScreenHeight();
        return screenHeight > 0 ? (double)lastGuiHeight / screenHeight : 1.0D;
    }

    private static double mouseScale(Minecraft minecraft) {
        double sensitivity = minecraft.options.sensitivity().get();
        double vanillaSensitivity = Math.pow(sensitivity * 0.6D + 0.2D, 3.0D) * 8.0D;
        return SideCameraConfig.CROSSHAIR_MOUSE_SCALE * Mth.clamp(vanillaSensitivity, 0.5D, 1.5D);
    }

    private static void updatePlayerAim(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        Camera camera = minecraft.gameRenderer.getMainCamera();
        if (player == null || !camera.isInitialized()) {
            return;
        }

        int screenWidth = lastGuiWidth;
        int screenHeight = lastGuiHeight;
        double aspectRatio = cameraAspectRatio(minecraft);
        Vec3 cameraForward = new Vec3(camera.getLookVector());
        Vec3 cameraLeft = new Vec3(camera.getLeftVector());
        Vec3 cameraUp = new Vec3(camera.getUpVector());
        SideAimMath.ScreenPosition playerPosition = SideAimMath.projectPoint(
            player.getEyePosition(camera.getPartialTickTime()),
            camera.getPosition(),
            cameraForward,
            cameraLeft,
            cameraUp,
            screenWidth,
            screenHeight,
            SideCameraConfig.CAMERA_FOV,
            aspectRatio
        );
        Vec3 depthDirection = cameraForward.scale(depthSign);
        Vec3 screenRight = cameraLeft.reverse();
        Vec3 aimDirection = SideAimMath.directionFromScreen(
            virtualCrosshairX,
            virtualCrosshairY,
            playerPosition,
            screenWidth,
            screenHeight,
            SideCameraConfig.CAMERA_FOV,
            aspectRatio,
            depthDirection,
            screenRight,
            cameraUp
        );
        if (aimDirection == null) {
            // A degenerate camera basis must not overwrite the last valid direction.
            aimDirection = lastAimDirection;
        } else {
            lastAimDirection = aimDirection;
        }

        applyAimDirection(player, aimDirection);
    }

    private static double cameraAspectRatio(Minecraft minecraft) {
        int width = minecraft.getWindow().getWidth();
        int height = minecraft.getWindow().getHeight();
        return width > 0 && height > 0 ? (double)width / height : 1.0D;
    }

    private static void applyAimDirection(LocalPlayer player, Vec3 aimDirection) {
        float yaw = SideAimMath.yawFor(aimDirection, player.getYRot());
        float pitch = SideAimMath.pitchFor(aimDirection);

        player.setYRot(yaw);
        player.setXRot(pitch);
        player.setYHeadRot(yaw);

        // Match vanilla's immediate interpolation behaviour for mouse turning without taking over body rotation.
        player.yRotO = yaw;
        player.xRotO = pitch;
        player.yHeadRotO = yaw;
    }
}

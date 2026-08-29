package com.shouyun.worldslice;

import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import com.mojang.blaze3d.platform.InputConstants;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = WorldSlice.MODID, value = Dist.CLIENT)
public final class SideCameraController {
    private static final KeyMapping TOGGLE_KEY = new KeyMapping(
        "key.worldslice.side_camera",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_V,
        "key.categories.worldslice"
    );
    private static boolean enabled;
    private static CameraType previousCameraType = CameraType.FIRST_PERSON;
    private static boolean previousBobView = true;

    private SideCameraController() {
    }

    public static void registerKeyMapping(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_KEY);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (TOGGLE_KEY.consumeClick()) {
            toggle(minecraft);
        }
    }

    private static void toggle(Minecraft minecraft) {
        Options options = minecraft.options;
        if (enabled) {
            enabled = false;
            SideAimController.disable();
            options.setCameraType(previousCameraType);
            options.bobView().set(previousBobView);
            return;
        }

        previousCameraType = options.getCameraType();
        previousBobView = options.bobView().get();
        enabled = true;
        options.setCameraType(CameraType.THIRD_PERSON_BACK);
        options.bobView().set(false);
        SideAimController.enable(minecraft);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    @SubscribeEvent
    public static void computeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (enabled) {
            event.setYaw(SideCameraConfig.CAMERA_YAW);
            event.setPitch(SideCameraConfig.CAMERA_PITCH);
            event.setRoll(SideCameraConfig.CAMERA_ROLL);
        }
    }

    @SubscribeEvent
    public static void computeFov(ViewportEvent.ComputeFov event) {
        if (enabled) {
            event.setFOV(SideCameraConfig.CAMERA_FOV);
        }
    }
}

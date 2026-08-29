package com.shouyun.worldslice;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** Client-only O shortcut for the World Slice settings screen. */
@EventBusSubscriber(modid = WorldSlice.MODID, value = Dist.CLIENT)
public final class WorldSliceSettingsController {
    private static final KeyMapping SETTINGS_KEY = new KeyMapping(
        "key.worldslice.settings",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_O,
        "key.categories.worldslice"
    );
    private static long lastSettingsRevision = Long.MIN_VALUE;

    private WorldSliceSettingsController() {
    }

    public static void registerKeyMapping(RegisterKeyMappingsEvent event) {
        event.register(SETTINGS_KEY);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        long settingsRevision = WorldSliceWorldSettings.clientSettingsRevision();
        if (settingsRevision != lastSettingsRevision) {
            lastSettingsRevision = settingsRevision;
            if (minecraft.level != null) {
                minecraft.levelRenderer.allChanged();
            }
        }

        while (SETTINGS_KEY.consumeClick()) {
            if (minecraft.screen == null && minecraft.player != null && minecraft.level != null) {
                minecraft.setScreen(new WorldSliceConfigScreen(null));
            }
        }
    }

    /**
     * Adds a second, low-risk entry point to the pause menu. The button is
     * appended below the existing vanilla/loader buttons so the vanilla layout
     * is never disturbed.
     */
    @SubscribeEvent
    public static void onScreenInitPost(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof PauseScreen pauseScreen) || !pauseScreen.showsPauseMenu()) {
            return;
        }

        Button lastButton = null;
        for (GuiEventListener listener : pauseScreen.children()) {
            if (listener instanceof Button button) {
                lastButton = button;
            }
        }
        if (lastButton == null) {
            return;
        }

        int x = lastButton.getX();
        int y = lastButton.getY() + lastButton.getHeight() + 4;
        event.addListener(Button.builder(
            Component.translatable("worldslice.screen.pause_button"),
            button -> Minecraft.getInstance().setScreen(new WorldSliceConfigScreen(pauseScreen))
        ).bounds(x, y, 204, 20).build());
    }
}

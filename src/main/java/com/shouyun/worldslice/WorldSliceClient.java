package com.shouyun.worldslice;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = WorldSlice.MODID, dist = Dist.CLIENT)
public final class WorldSliceClient {
    public WorldSliceClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(SideCameraController::registerKeyMapping);
        modEventBus.addListener(WorldSliceSettingsController::registerKeyMapping);
        container.registerConfig(ModConfig.Type.CLIENT, SideCameraConfig.SPEC);
        container.registerExtensionPoint(
            IConfigScreenFactory.class,
            (modContainer, modListScreen) -> new WorldSliceConfigScreen(modListScreen, ConfigScreenContext.MAIN_MENU)
        );
        NeoForge.EVENT_BUS.addListener(WorldSliceClient::onClientLoggingOut);
    }

    private static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        WorldSliceWorldSettings.clearClientWorldSettings();
    }
}

package com.shouyun.worldslice;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@Mod(value = WorldSlice.MODID, dist = Dist.CLIENT)
public final class WorldSliceClient {
    public WorldSliceClient(IEventBus modEventBus) {
        modEventBus.addListener(SideCameraController::registerKeyMapping);
        modEventBus.addListener(WorldSliceSettingsController::registerKeyMapping);
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.CLIENT, SideCameraConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(WorldSliceClient::onClientLoggingOut);
    }

    private static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        WorldSliceWorldSettings.clearClientWorldSettings();
    }
}

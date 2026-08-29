package com.shouyun.worldslice;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = WorldSlice.MODID, dist = Dist.CLIENT)
public final class WorldSliceClient {
    public WorldSliceClient(IEventBus modEventBus) {
        modEventBus.addListener(SideCameraController::registerKeyMapping);
    }
}

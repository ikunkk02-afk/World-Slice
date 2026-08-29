package com.shouyun.worldslice;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(WorldSlice.MODID)
public final class WorldSlice {
    public static final String MODID = "worldslice";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WorldSlice(IEventBus modEventBus) {
        modEventBus.addListener(WorldSliceNetworking::registerPayloads);
        NeoForge.EVENT_BUS.register(WorldSliceServerEvents.class);
    }
}

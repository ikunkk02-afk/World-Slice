package com.shouyun.worldslice;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(WorldSlice.MODID)
public final class WorldSlice {
    public static final String MODID = "worldslice";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WorldSlice(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(WorldSliceNetworking::registerPayloads);
        container.registerConfig(ModConfig.Type.COMMON, WorldSliceDefaultsConfig.SPEC);
        NeoForge.EVENT_BUS.register(WorldSliceServerEvents.class);
    }
}

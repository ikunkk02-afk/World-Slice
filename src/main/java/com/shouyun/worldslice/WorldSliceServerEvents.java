package com.shouyun.worldslice;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

public final class WorldSliceServerEvents {
    private WorldSliceServerEvents() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ServerLevel overworld = event.getServer().overworld();
        if (WorldSliceBounds.isWorldSliceLevel(overworld)) {
            BlockPos safeSpawn = SpawnSafety.findSafeSpawn(overworld);
            overworld.setDefaultSpawnPos(safeSpawn, overworld.getLevelData().getSpawnAngle());
            WorldSlice.LOGGER.info("World Slice Overworld active: X={}..{}, spawn={}",
                WorldSliceBounds.minX(), WorldSliceBounds.maxX(), safeSpawn);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (WorldSliceBounds.isWorldSliceLevel(level) && !WorldSliceBounds.isInside(player.blockPosition())) {
            BlockPos safeSpawn = SpawnSafety.findSafeSpawn(level);
            player.teleportTo(safeSpawn.getX() + 0.5D, safeSpawn.getY(), safeSpawn.getZ() + 0.5D);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerRespawnPositionEvent event) {
        DimensionTransition transition = event.getDimensionTransition();
        if (transition.newLevel().dimension().equals(Level.OVERWORLD)
            && WorldSliceBounds.isWorldSliceLevel(transition.newLevel())) {
            BlockPos safeSpawn = SpawnSafety.findSafeSpawn(transition.newLevel());
            event.setDimensionTransition(new DimensionTransition(
                transition.newLevel(),
                safeSpawn.getBottomCenter(),
                transition.speed(),
                transition.yRot(),
                transition.xRot(),
                transition.missingRespawnBlock(),
                transition.postDimensionTransition()
            ));
        }
    }
}

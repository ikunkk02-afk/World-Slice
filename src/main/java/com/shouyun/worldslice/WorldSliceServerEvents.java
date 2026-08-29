package com.shouyun.worldslice;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class WorldSliceServerEvents {
    private static final Map<MinecraftServer, PendingSpawnSearch> PENDING_SPAWN_SEARCHES = new WeakHashMap<>();

    private WorldSliceServerEvents() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ServerLevel overworld = event.getServer().overworld();
        if (!WorldSliceBounds.isWorldSliceLevel(overworld)) {
            return;
        }

        BlockPos vanillaSpawn = overworld.getLevelData().getSpawnPos();
        WorldSlice.LOGGER.debug("World Slice world seed: {}", overworld.getSeed());
        WorldSlice.LOGGER.debug("Vanilla/default spawn before adjustment: {}", vanillaSpawn);

        WorldSliceSpawnData savedData = WorldSliceSpawnData.get(overworld);
        if (savedData.isInitialized()) {
            BlockPos savedSpawn = savedData.spawnPos();
            if (savedSpawn != null && !savedSpawn.equals(vanillaSpawn)) {
                // SavedData is written immediately after initialization. If a
                // process stops before level.dat is flushed, restore that same
                // position; this is restoration, never a new search.
                overworld.setDefaultSpawnPos(savedSpawn, overworld.getLevelData().getSpawnAngle());
            }

            logFinalSpawn(overworld, savedSpawn != null ? savedSpawn : overworld.getLevelData().getSpawnPos());
            WorldSlice.LOGGER.info("World Slice Overworld active: X={}..{} (saved spawn retained)",
                WorldSliceBounds.minX(), WorldSliceBounds.maxX());
            return;
        }

        PendingSpawnSearch pending = new PendingSpawnSearch(
            overworld, savedData, vanillaSpawn, SpawnSafety.beginSearch(overworld, vanillaSpawn)
        );
        PENDING_SPAWN_SEARCHES.put(event.getServer(), pending);

        // The first phase is small enough to make the common case ready at
        // startup. Wider phases are continued four chunks per server tick.
        advanceSpawnSearch(event.getServer(), SpawnSafety.INITIAL_CHUNKS_PER_START);
        WorldSlice.LOGGER.info("World Slice Overworld active: X={}..{} (spawn search in progress)",
            WorldSliceBounds.minX(), WorldSliceBounds.maxX());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        advanceSpawnSearch(event.getServer(), SpawnSafety.CHUNKS_PER_TICK);
    }

    private static void advanceSpawnSearch(MinecraftServer server, int chunkBudget) {
        PendingSpawnSearch pending = PENDING_SPAWN_SEARCHES.get(server);
        if (pending == null) {
            return;
        }

        pending.search().advance(chunkBudget);
        if (pending.search().isComplete()) {
            PENDING_SPAWN_SEARCHES.remove(server);
            applySearchResult(pending);
        }
    }

    private static void applySearchResult(PendingSpawnSearch pending) {
        BlockPos spawn = pending.search().bestSpawn();
        if (spawn == null) {
            spawn = SpawnSafety.createSafeFallback(pending.level(), pending.vanillaSpawn());
            if (spawn == null) {
                WorldSlice.LOGGER.error(
                    "World Slice could not create a verified fallback spawn around vanilla spawn Z={}; leaving the vanilla spawn unchanged.",
                    pending.vanillaSpawn().getZ()
                );
                return;
            }

            WorldSlice.LOGGER.warn(
                "World Slice found no dry natural surface in its search range; created a seed-derived safe fallback pad at {}.",
                spawn
            );
        }

        ServerLevel level = pending.level();
        level.setDefaultSpawnPos(spawn, level.getLevelData().getSpawnAngle());
        pending.savedData().markInitialized(spawn);
        // Persist the one-time marker and the chosen position immediately so
        // a stop before the next autosave cannot trigger another search.
        level.getDataStorage().save();
        logFinalSpawn(level, spawn);
        relocatePlayersAtUninitializedSpawn(level, spawn);
    }

    private static void logFinalSpawn(ServerLevel level, BlockPos spawn) {
        WorldSlice.LOGGER.debug("Final World Slice spawn: {}", spawn);
        WorldSlice.LOGGER.debug("Spawn biome: {}", SpawnSafety.describeBiome(level, spawn));
    }

    private static void relocatePlayersAtUninitializedSpawn(ServerLevel level, BlockPos spawn) {
        for (ServerPlayer player : level.players()) {
            if (!WorldSliceBounds.isInside(player.blockPosition())
                || !SpawnSafety.isSafeSpawnPosition(level, player.blockPosition())) {
                player.teleportTo(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
            }
        }
    }

    private static BlockPos getFallbackSpawn(ServerLevel level) {
        WorldSliceSpawnData savedData = WorldSliceSpawnData.get(level);
        if (savedData.spawnPos() != null) {
            return savedData.spawnPos();
        }

        PendingSpawnSearch pending = PENDING_SPAWN_SEARCHES.get(level.getServer());
        if (pending != null) {
            // A player should not wait for a 4096-block search to finish. Use
            // the best already verified candidate while the staged search runs.
            advanceSpawnSearch(level.getServer(), SpawnSafety.CHUNKS_PER_PLAYER_EVENT);
            if (savedData.spawnPos() != null) {
                return savedData.spawnPos();
            }
            return pending.search().bestSpawn();
        }

        BlockPos currentSpawn = level.getLevelData().getSpawnPos();
        return WorldSliceBounds.isInside(currentSpawn) && SpawnSafety.isSafeSpawnPosition(level, currentSpawn)
            ? currentSpawn
            : null;
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (WorldSliceBounds.isWorldSliceLevel(level) && !WorldSliceBounds.isInside(player.blockPosition())) {
            BlockPos safeSpawn = getFallbackSpawn(level);
            if (safeSpawn != null) {
                player.teleportTo(safeSpawn.getX() + 0.5D, safeSpawn.getY(), safeSpawn.getZ() + 0.5D);
            } else {
                WorldSlice.LOGGER.warn("World Slice has no verified fallback spawn yet for player {}", player.getGameProfile().getName());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerRespawnPositionEvent event) {
        DimensionTransition transition = event.getDimensionTransition();
        if (transition.newLevel().dimension().equals(Level.OVERWORLD)
            && WorldSliceBounds.isWorldSliceLevel(transition.newLevel())) {
            ServerLevel level = transition.newLevel();
            BlockPos vanillaRespawn = BlockPos.containing(transition.pos());
            if (WorldSliceBounds.isInside(vanillaRespawn)
                && SpawnSafety.isSafeSpawnPosition(level, vanillaRespawn)) {
                // Keep vanilla bed/default/dimension respawn positions when
                // they are already valid inside the one playable chunk.
                return;
            }

            BlockPos safeSpawn = getFallbackSpawn(level);
            if (safeSpawn == null) {
                WorldSlice.LOGGER.warn("World Slice could not replace invalid vanilla respawn position {} yet", vanillaRespawn);
                return;
            }

            event.setDimensionTransition(new DimensionTransition(
                level,
                safeSpawn.getBottomCenter(),
                transition.speed(),
                transition.yRot(),
                transition.xRot(),
                transition.missingRespawnBlock(),
                transition.postDimensionTransition()
            ));
        }
    }

    private record PendingSpawnSearch(
        ServerLevel level,
        WorldSliceSpawnData savedData,
        BlockPos vanillaSpawn,
        SpawnSafety.Search search
    ) {
    }
}

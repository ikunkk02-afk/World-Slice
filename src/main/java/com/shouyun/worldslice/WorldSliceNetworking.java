package com.shouyun.worldslice;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Registers and handles the small authoritative settings protocol. */
public final class WorldSliceNetworking {
    private WorldSliceNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
            .playToClient(
                WorldSliceSettingsSyncPayload.TYPE,
                WorldSliceSettingsSyncPayload.STREAM_CODEC,
                WorldSliceNetworking::handleSettingsSync
            )
            .playToServer(
                WorldSliceSettingsRequestPayload.TYPE,
                WorldSliceSettingsRequestPayload.STREAM_CODEC,
                WorldSliceNetworking::handleSettingsRequest
            );
    }

    private static void handleSettingsSync(WorldSliceSettingsSyncPayload payload, IPayloadContext context) {
        WorldSliceWorldSettings.applyClientWorldThickness(payload.worldThickness());
    }

    private static void handleSettingsRequest(WorldSliceSettingsRequestPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        context.enqueueWork(() -> {
            if (!(player.level() instanceof ServerLevel level)
                || !WorldSliceBounds.isWorldSliceLevel(level)
                || !level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
                return;
            }

            WorldSliceWorldSettings settings = WorldSliceWorldSettings.get(level);
            if (!payload.apply()) {
                sendSettings(player, settings.worldThickness());
                return;
            }

            // The request is untrusted. Validate both permission and range on
            // the server before mutating the world's SavedData.
            if (!player.hasPermissions(2)
                || payload.requestedThickness() < WorldSliceWorldSettings.MIN_WORLD_THICKNESS
                || payload.requestedThickness() > WorldSliceWorldSettings.MAX_WORLD_THICKNESS) {
                sendSettings(player, settings.worldThickness());
                return;
            }

            settings.setWorldThickness(payload.requestedThickness());
            level.getDataStorage().save();
            PacketDistributor.sendToAllPlayers(new WorldSliceSettingsSyncPayload(settings.worldThickness()));
        });
    }

    private static void sendSettings(ServerPlayer player, int worldThickness) {
        PacketDistributor.sendToPlayer(player, new WorldSliceSettingsSyncPayload(worldThickness));
    }
}

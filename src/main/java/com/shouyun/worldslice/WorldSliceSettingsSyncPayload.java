package com.shouyun.worldslice;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server-authoritative World Thickness sent to each connected client. */
public record WorldSliceSettingsSyncPayload(int worldThickness) implements CustomPacketPayload {
    public static final Type<WorldSliceSettingsSyncPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(WorldSlice.MODID, "settings_sync")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, WorldSliceSettingsSyncPayload> STREAM_CODEC =
        StreamCodec.composite(ByteBufCodecs.VAR_INT, WorldSliceSettingsSyncPayload::worldThickness, WorldSliceSettingsSyncPayload::new);

    @Override
    public Type<WorldSliceSettingsSyncPayload> type() {
        return TYPE;
    }
}

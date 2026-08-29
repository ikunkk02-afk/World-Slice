package com.shouyun.worldslice;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client request for the current value, or a permission-checked update. */
public record WorldSliceSettingsRequestPayload(int requestedThickness, boolean apply) implements CustomPacketPayload {
    public static final Type<WorldSliceSettingsRequestPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(WorldSlice.MODID, "settings_request")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, WorldSliceSettingsRequestPayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            WorldSliceSettingsRequestPayload::requestedThickness,
            ByteBufCodecs.BOOL,
            WorldSliceSettingsRequestPayload::apply,
            WorldSliceSettingsRequestPayload::new
        );

    @Override
    public Type<WorldSliceSettingsRequestPayload> type() {
        return TYPE;
    }
}

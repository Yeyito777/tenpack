package dev.yeyito.tenpacktravel;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record WhistlePayload() implements CustomPacketPayload {
    public static final WhistlePayload INSTANCE = new WhistlePayload();
    public static final Type<WhistlePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TenpackTravel.MODID, "whistle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WhistlePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package dev.yeyito.tenpackfoodui;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FoodBalancePayload(int staple, int protein, int produce) implements CustomPacketPayload {
    public static final Type<FoodBalancePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TenpackFoodUi.MODID, "food_balance"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FoodBalancePayload> STREAM_CODEC = StreamCodec.ofMember(
            FoodBalancePayload::encode,
            FoodBalancePayload::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(staple);
        buffer.writeVarInt(protein);
        buffer.writeVarInt(produce);
    }

    private static FoodBalancePayload decode(RegistryFriendlyByteBuf buffer) {
        return new FoodBalancePayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    static void handle(FoodBalancePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist != Dist.CLIENT) {
                return;
            }
            TenpackFoodUiClient.update(payload.staple(), payload.protein(), payload.produce());
        });
    }
}

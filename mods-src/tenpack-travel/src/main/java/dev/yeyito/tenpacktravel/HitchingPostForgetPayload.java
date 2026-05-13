package dev.yeyito.tenpacktravel;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record HitchingPostForgetPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<HitchingPostForgetPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TenpackTravel.MODID, "hitching_post_forget"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HitchingPostForgetPayload> STREAM_CODEC = StreamCodec.ofMember(
            HitchingPostForgetPayload::encode,
            HitchingPostForgetPayload::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
    }

    private static HitchingPostForgetPayload decode(RegistryFriendlyByteBuf buffer) {
        return new HitchingPostForgetPayload(buffer.readBlockPos());
    }

    static void handle(HitchingPostForgetPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (player.distanceToSqr(payload.pos().getCenter()) > 16.0D * 16.0D) {
                player.displayClientMessage(Component.translatable("message.tenpack_travel.hitching_post.too_far_update"), true);
                return;
            }
            if (!(player.level().getBlockEntity(payload.pos()) instanceof HitchingPostBlockEntity post)) {
                player.displayClientMessage(Component.translatable("message.tenpack_travel.hitching_post.unavailable"), true);
                return;
            }
            int removed = post.forgetUnhitchedMemories();
            if (removed <= 0) {
                player.displayClientMessage(Component.translatable("message.tenpack_travel.hitching_post.forget_none").withStyle(ChatFormatting.GRAY), true);
            } else {
                player.displayClientMessage(Component.translatable("message.tenpack_travel.hitching_post.forget_removed", removed).withStyle(ChatFormatting.GRAY), true);
            }
        });
    }
}

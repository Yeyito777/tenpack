package dev.yeyito.tenpacktravel;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record HitchingPostCommandPayload(BlockPos pos, AnimalCommand.Mode mode) implements CustomPacketPayload {
    public static final Type<HitchingPostCommandPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TenpackTravel.MODID, "hitching_post_command"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HitchingPostCommandPayload> STREAM_CODEC = StreamCodec.ofMember(
            HitchingPostCommandPayload::encode,
            HitchingPostCommandPayload::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeUtf(mode.id(), 16);
    }

    private static HitchingPostCommandPayload decode(RegistryFriendlyByteBuf buffer) {
        return new HitchingPostCommandPayload(buffer.readBlockPos(), AnimalCommand.Mode.fromId(buffer.readUtf(16)));
    }

    static void handle(HitchingPostCommandPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (player.distanceToSqr(payload.pos().getCenter()) > 16.0D * 16.0D) {
                player.displayClientMessage(Component.translatable("message.tenpack_travel.hitching_post.too_far_adjust"), true);
                return;
            }
            if (!(player.level().getBlockEntity(payload.pos()) instanceof HitchingPostBlockEntity post)) {
                player.displayClientMessage(Component.translatable("message.tenpack_travel.hitching_post.unavailable"), true);
                return;
            }
            if (payload.mode() != AnimalCommand.Mode.STAY && payload.mode() != AnimalCommand.Mode.ROAM) {
                player.displayClientMessage(Component.translatable("message.tenpack_travel.hitching_post.unsupported_mode").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            HitchingPostBlockEntity.ModeApplyResult result = post.setPostMode(player, payload.mode());
            sendPostModeResult(player, payload.mode(), result);
        });
    }

    static void sendPostModeResult(Player player, AnimalCommand.Mode mode, HitchingPostBlockEntity.ModeApplyResult result) {
        if (!result.postModeChanged()) {
            player.displayClientMessage(Component.translatable("message.tenpack_travel.hitching_post.low_trust").withStyle(ChatFormatting.GRAY), true);
        } else if (result.skipped() > 0) {
            player.displayClientMessage(Component.translatable("message.tenpack_travel.hitching_post.mode_set_skipped", mode.labelComponent(), result.skipped()).withStyle(ChatFormatting.GRAY), true);
        } else {
            player.displayClientMessage(Component.translatable("message.tenpack_travel.hitching_post.mode_set", mode.labelComponent()), true);
        }
    }
}

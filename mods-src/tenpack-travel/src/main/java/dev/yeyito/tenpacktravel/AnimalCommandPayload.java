package dev.yeyito.tenpacktravel;

import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AnimalCommandPayload(int entityId, AnimalCommand.Mode mode) implements CustomPacketPayload {
    public static final Type<AnimalCommandPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TenpackTravel.MODID, "animal_command"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AnimalCommandPayload> STREAM_CODEC = StreamCodec.ofMember(
            AnimalCommandPayload::encode,
            AnimalCommandPayload::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeUtf(mode.id(), 16);
    }

    private static AnimalCommandPayload decode(RegistryFriendlyByteBuf buffer) {
        return new AnimalCommandPayload(buffer.readVarInt(), AnimalCommand.Mode.fromId(buffer.readUtf(16)));
    }

    static void handle(AnimalCommandPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            Entity entity = player.level().getEntity(payload.entityId());
            if (!(entity instanceof LivingEntity animal) || !animal.isAlive()) {
                player.displayClientMessage(Component.translatable("message.tenpack_travel.animal_command.missing"), true);
                return;
            }
            if (player.distanceToSqr(animal) > 18.0D * 18.0D) {
                player.displayClientMessage(Component.translatable("message.tenpack_travel.animal_command.too_far"), true);
                return;
            }
            if (!AnimalCommand.canCommand(player, animal)) {
                player.displayClientMessage(Component.translatable("message.tenpack_travel.animal_command.low_trust").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            if (AstikorIntegration.isActiveDraftAnimal(animal) && payload.mode() != AnimalCommand.Mode.FREE) {
                player.displayClientMessage(Component.translatable("message.tenpack_travel.animal_command.detach_cart").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            AnimalCommand.setMode(player, animal, payload.mode());
            player.displayClientMessage(Component.translatable(
                    "message.tenpack_travel.animal_command.set",
                    animal.getDisplayName(),
                    payload.mode().labelComponent()
            ), true);
        });
    }
}

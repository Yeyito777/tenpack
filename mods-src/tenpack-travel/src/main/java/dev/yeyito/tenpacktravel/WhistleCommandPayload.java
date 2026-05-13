package dev.yeyito.tenpacktravel;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Comparator;
import java.util.List;

public record WhistleCommandPayload(AnimalCommand.Mode mode) implements CustomPacketPayload {
    private static final String LAST_WHISTLE_COMMAND_TICK_KEY = "tenpack_travel_last_whistle_command_tick";
    private static final int COMMAND_COOLDOWN_TICKS = 40;
    private static final int MIN_COMMAND_XP = 20;
    private static final int MAX_TARGETS = 8;
    private static final double MAX_RANGE = 42.0D;

    public static final Type<WhistleCommandPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TenpackTravel.MODID, "whistle_command"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WhistleCommandPayload> STREAM_CODEC = StreamCodec.ofMember(
            WhistleCommandPayload::encode,
            WhistleCommandPayload::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(mode.id(), 16);
    }

    private static WhistleCommandPayload decode(RegistryFriendlyByteBuf buffer) {
        return new WhistleCommandPayload(AnimalCommand.Mode.fromId(buffer.readUtf(16)));
    }

    static void handle(WhistleCommandPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (payload.mode() == AnimalCommand.Mode.FREE || payload.mode() == AnimalCommand.Mode.FOLLOW
                    || payload.mode() == AnimalCommand.Mode.STAY || payload.mode() == AnimalCommand.Mode.ROAM) {
                runCommand(player, payload.mode());
            }
        });
    }

    private static void runCommand(ServerPlayer player, AnimalCommand.Mode mode) {
        if (!TenpackTravel.canUseWhistle(player)) {
            player.displayClientMessage(Component.translatable("message.tenpack_travel.whistle.need_item_command"), true);
            return;
        }

        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        long last = player.getPersistentData().getLong(LAST_WHISTLE_COMMAND_TICK_KEY);
        if (last != 0L && now - last < COMMAND_COOLDOWN_TICKS) {
            player.displayClientMessage(Component.translatable("message.tenpack_travel.whistle.command_cooldown"), true);
            return;
        }
        player.getPersistentData().putLong(LAST_WHISTLE_COMMAND_TICK_KEY, now);

        List<Mob> targets = findCommandTargets(player, level);
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.tenpack_travel.whistle.no_command_targets"), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 0.55F, 1.25F);
            return;
        }

        int commanded = 0;
        for (Mob mob : targets) {
            AnimalCommand.setMode(player, mob, mode);
            mob.getLookControl().setLookAt(player, 30.0F, 30.0F);
            level.sendParticles(ParticleTypes.NOTE, mob.getX(), mob.getY() + mob.getBbHeight() + 0.1D, mob.getZ(), 1, 0.15D, 0.08D, 0.15D, 0.01D);
            commanded++;
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 0.9F, pitchFor(mode));
        level.sendParticles(ParticleTypes.NOTE, player.getX(), player.getY() + 1.75D, player.getZ(), 3, 0.25D, 0.1D, 0.25D, 0.02D);
        player.displayClientMessage(Component.translatable("message.tenpack_travel.whistle.commanded", commanded, mode.label()), true);
    }

    private static List<Mob> findCommandTargets(ServerPlayer player, ServerLevel level) {
        return level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(MAX_RANGE), AnimalBond::isBondable)
                .stream()
                .filter(entity -> entity instanceof Mob)
                .map(entity -> (Mob) entity)
                .filter(Mob::isAlive)
                .filter(mob -> mob.getVehicle() == null && mob.getFirstPassenger() == null)
                .filter(WhistleCommandPayload::isUnhitched)
                .filter(mob -> !AstikorIntegration.isActiveDraftAnimal(mob))
                .filter(mob -> AnimalCommand.canCommand(player, mob))
                .filter(mob -> AnimalBond.snapshot(mob, player).xp() >= MIN_COMMAND_XP || player.isCreative() || player.isSpectator())
                .sorted(Comparator
                        .comparingInt((Mob mob) -> AnimalBond.snapshot(mob, player).xp()).reversed()
                        .thenComparingDouble(mob -> mob.distanceToSqr(player)))
                .limit(MAX_TARGETS)
                .toList();
    }

    private static boolean isUnhitched(Entity entity) {
        return !(entity instanceof Leashable leashable) || leashable.getLeashHolder() == null;
    }

    private static float pitchFor(AnimalCommand.Mode mode) {
        return switch (mode) {
            case FOLLOW -> 1.75F;
            case STAY -> 1.25F;
            case ROAM -> 1.45F;
            case FREE -> 1.05F;
        };
    }
}

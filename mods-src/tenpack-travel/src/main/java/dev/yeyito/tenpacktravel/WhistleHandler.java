package dev.yeyito.tenpacktravel;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Comparator;
import java.util.List;

final class WhistleHandler {
    private static final String LAST_WHISTLE_TICK_KEY = "tenpack_travel_last_whistle_tick";
    private static final int WHISTLE_COOLDOWN_TICKS = 60;
    private static final int WHISTLE_MIN_XP = 20;
    private static final int MAX_RESPONDERS = 3;
    private static final double MAX_WHISTLE_RANGE = 56.0D;

    private WhistleHandler() {
    }

    static void handle(WhistlePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                whistle(player);
            }
        });
    }

    private static void whistle(ServerPlayer player) {
        if (!TenpackTravel.canUseWhistle(player)) {
            player.displayClientMessage(Component.translatable("message.tenpack_travel.whistle.need_item_call"), true);
            return;
        }

        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        long last = player.getPersistentData().getLong(LAST_WHISTLE_TICK_KEY);
        if (last != 0L && now - last < WHISTLE_COOLDOWN_TICKS) {
            player.displayClientMessage(Component.translatable("message.tenpack_travel.whistle.cooldown"), true);
            return;
        }
        player.getPersistentData().putLong(LAST_WHISTLE_TICK_KEY, now);

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 1.0F, 1.65F);
        level.sendParticles(ParticleTypes.NOTE, player.getX(), player.getY() + 1.75D, player.getZ(), 2, 0.25D, 0.1D, 0.25D, 0.02D);

        List<Mob> responders = findResponders(player, level);
        if (responders.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.tenpack_travel.whistle.no_answer"), true);
            return;
        }

        int called = 0;
        for (Mob mob : responders) {
            if (callResponder(player, level, mob)) {
                called++;
            }
        }
        int rolePings = AnimalRoleActions.runWhistleRoles(player, responders);
        if (rolePings > 0) {
            return;
        }
        sendCallSummary(player, called);
    }

    private static List<Mob> findResponders(ServerPlayer player, ServerLevel level) {
        return level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(MAX_WHISTLE_RANGE), AnimalBond::isBondable)
                .stream()
                .filter(entity -> entity instanceof Mob)
                .map(entity -> (Mob) entity)
                .filter(Mob::isAlive)
                .filter(mob -> mob.getVehicle() == null && mob.getFirstPassenger() == null)
                .filter(mob -> !AstikorIntegration.isActiveDraftAnimal(mob))
                .filter(WhistleHandler::canLeaveCurrentCommand)
                .filter(mob -> canRespondToWhistle(player, mob))
                .sorted(Comparator
                        .comparingInt((Mob mob) -> AnimalBond.snapshot(mob, player).xp()).reversed()
                        .thenComparingDouble(mob -> mob.distanceToSqr(player)))
                .limit(MAX_RESPONDERS)
                .toList();
    }

    private static boolean callResponder(Player player, ServerLevel level, Mob mob) {
        boolean pathing = mob.getNavigation().moveTo(player, whistleSpeed(mob, player));
        mob.getLookControl().setLookAt(player, 30.0F, 30.0F);
        level.sendParticles(ParticleTypes.NOTE, mob.getX(), mob.getY() + mob.getBbHeight() + 0.1D, mob.getZ(), 2, 0.2D, 0.1D, 0.2D, 0.02D);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, mob.getX(), mob.getY() + mob.getBbHeight() * 0.75D, mob.getZ(), 2, 0.18D, 0.12D, 0.18D, 0.01D);
        mob.playAmbientSound();
        return pathing;
    }

    private static void sendCallSummary(ServerPlayer player, int called) {
        if (called == 0) {
            player.displayClientMessage(Component.translatable("message.tenpack_travel.whistle.path_fail"), true);
        } else if (called == 1) {
            player.displayClientMessage(Component.translatable("message.tenpack_travel.whistle.answer_one"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.tenpack_travel.whistle.answer_many", called), true);
        }
    }

    private static boolean canRespondToWhistle(Player player, LivingEntity animal) {
        AnimalBond.Snapshot bond = AnimalBond.snapshot(animal, player);
        if (bond.ownedByOther()) {
            return false;
        }
        if (bond.xp() < WHISTLE_MIN_XP) {
            return false;
        }
        double range = whistleRange(bond.xp());
        return animal.distanceToSqr(player) <= range * range;
    }

    private static boolean canLeaveCurrentCommand(Mob mob) {
        AnimalCommand.Mode mode = AnimalCommand.snapshot(mob).mode();
        return mode == AnimalCommand.Mode.FREE || mode == AnimalCommand.Mode.FOLLOW;
    }

    private static double whistleRange(int xp) {
        if (xp >= 220) return 56.0D;
        if (xp >= 120) return 44.0D;
        if (xp >= 60) return 32.0D;
        return 20.0D;
    }

    private static double whistleSpeed(LivingEntity animal, Player player) {
        int xp = AnimalBond.snapshot(animal, player).xp();
        if (xp >= 220) return 1.35D;
        if (xp >= 120) return 1.25D;
        if (xp >= 60) return 1.15D;
        return 1.05D;
    }
}

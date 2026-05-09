package dev.yeyito.tenpacktravel;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Comparator;
import java.util.List;

public final class TenpackTravelNetwork {
    private static final String VERSION = "1";
    private static final String LAST_WHISTLE_TICK_KEY = "tenpack_travel_last_whistle_tick";
    private static final int WHISTLE_COOLDOWN_TICKS = 60;
    private static final int WHISTLE_MIN_XP = 20;
    private static final int MAX_RESPONDERS = 3;
    private static final double MAX_WHISTLE_RANGE = 56.0D;

    private TenpackTravelNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(TenpackTravel.MODID).versioned(VERSION);
        registrar.playToServer(WhistlePayload.TYPE, WhistlePayload.STREAM_CODEC, TenpackTravelNetwork::handleWhistle);
    }

    private static void handleWhistle(WhistlePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                whistle(player);
            }
        });
    }

    private static void whistle(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        long last = player.getPersistentData().getLong(LAST_WHISTLE_TICK_KEY);
        if (last != 0L && now - last < WHISTLE_COOLDOWN_TICKS) {
            player.displayClientMessage(Component.literal("You catch your breath before whistling again."), true);
            return;
        }
        player.getPersistentData().putLong(LAST_WHISTLE_TICK_KEY, now);

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 1.0F, 1.65F);
        level.sendParticles(ParticleTypes.NOTE, player.getX(), player.getY() + 1.75D, player.getZ(), 2, 0.25D, 0.1D, 0.25D, 0.02D);

        List<Mob> responders = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(MAX_WHISTLE_RANGE), AnimalBond::isBondable)
                .stream()
                .filter(entity -> entity instanceof Mob)
                .map(entity -> (Mob) entity)
                .filter(Mob::isAlive)
                .filter(mob -> mob.getVehicle() == null && mob.getFirstPassenger() == null)
                .filter(mob -> canRespondToWhistle(player, mob))
                .sorted(Comparator
                        .comparingInt((Mob mob) -> AnimalBond.snapshot(mob, player).xp()).reversed()
                        .thenComparingDouble(mob -> mob.distanceToSqr(player)))
                .limit(MAX_RESPONDERS)
                .toList();

        if (responders.isEmpty()) {
            player.displayClientMessage(Component.literal("No familiar animal answers your whistle."), true);
            return;
        }

        int called = 0;
        for (Mob mob : responders) {
            if (mob.getNavigation().moveTo(player, whistleSpeed(mob, player))) {
                called++;
            }
            mob.getLookControl().setLookAt(player, 30.0F, 30.0F);
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 45, 0, true, false, false), player);
            level.sendParticles(ParticleTypes.NOTE, mob.getX(), mob.getY() + mob.getBbHeight() + 0.1D, mob.getZ(), 2, 0.2D, 0.1D, 0.2D, 0.02D);
            mob.playAmbientSound();
        }
        int rolePings = AnimalRoleActions.runWhistleRoles(player, responders);

        if (rolePings > 0) {
            return;
        }
        if (called == 0) {
            player.displayClientMessage(Component.literal("Your animals hear you, but cannot find a path."), true);
        } else if (called == 1) {
            player.displayClientMessage(Component.literal("A familiar animal answers your whistle."), true);
        } else {
            player.displayClientMessage(Component.literal(called + " familiar animals answer your whistle."), true);
        }
    }

    private static boolean canRespondToWhistle(Player player, LivingEntity animal) {
        AnimalBond.Snapshot bond = AnimalBond.snapshot(animal, player);
        if (bond.xp() < WHISTLE_MIN_XP) {
            return false;
        }
        double range = whistleRange(bond.xp());
        return animal.distanceToSqr(player) <= range * range;
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

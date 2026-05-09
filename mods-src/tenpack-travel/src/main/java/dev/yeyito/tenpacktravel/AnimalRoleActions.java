package dev.yeyito.tenpacktravel;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;

import java.util.Comparator;
import java.util.List;

public final class AnimalRoleActions {
    private static final int SCOUT_MIN_XP = 20;
    private static final int SCOUT_MAX_PINGS = 2;
    private static final int SCOUT_ROLE_XP = 2;
    private static final int SCOUT_ROLE_XP_COOLDOWN_TICKS = 20 * 60;

    private AnimalRoleActions() {
    }

    public static int runWhistleRoles(ServerPlayer player, List<Mob> responders) {
        int pings = 0;
        for (Mob responder : responders) {
            if (AnimalRoles.hasRole(responder, AnimalRoles.ActiveRole.SCOUT)) {
                pings += runScoutRole(player, responder, SCOUT_MAX_PINGS - pings);
                if (pings >= SCOUT_MAX_PINGS) {
                    break;
                }
            }
        }
        return pings;
    }

    private static int runScoutRole(ServerPlayer player, Mob scout, int remainingPings) {
        if (remainingPings <= 0) {
            return 0;
        }
        AnimalBond.Snapshot bond = AnimalBond.snapshot(scout, player);
        if (bond.xp() < SCOUT_MIN_XP) {
            return 0;
        }

        ServerLevel level = player.serverLevel();
        double range = scoutRange(bond.xp());
        List<LivingEntity> visibleDangers = level.getEntitiesOfClass(LivingEntity.class, scout.getBoundingBox().inflate(range), AnimalRoleActions::isDanger)
                .stream()
                .filter(target -> target != scout)
                .filter(target -> target.distanceToSqr(scout) <= range * range)
                .filter(scout::hasLineOfSight)
                .filter(target -> !target.isAlliedTo(player) && !target.isAlliedTo(scout))
                .sorted(Comparator.comparingDouble(target -> target.distanceToSqr(scout)))
                .limit(remainingPings)
                .toList();

        if (visibleDangers.isEmpty()) {
            return 0;
        }

        for (LivingEntity danger : visibleDangers) {
            pingDanger(player, scout, danger);
        }
        AnimalBond.roleUse(player, scout, "scout", SCOUT_ROLE_XP, SCOUT_ROLE_XP_COOLDOWN_TICKS);
        player.displayClientMessage(Component.literal(visibleDangers.size() == 1
                ? "Your scout cries a warning."
                : "Your scout cries warnings."), true);
        return visibleDangers.size();
    }

    private static boolean isDanger(LivingEntity target) {
        return target.isAlive() && target instanceof Enemy;
    }

    private static double scoutRange(int xp) {
        if (xp >= 220) return 64.0D;
        if (xp >= 120) return 52.0D;
        if (xp >= 60) return 40.0D;
        return 28.0D;
    }

    private static void pingDanger(ServerPlayer player, Mob scout, LivingEntity danger) {
        ServerLevel level = player.serverLevel();
        player.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, 0.8F, 1.8F);
        level.sendParticles(player, ParticleTypes.NOTE, false,
                scout.getX(), scout.getY() + scout.getBbHeight() + 0.15D, scout.getZ(),
                3, 0.25D, 0.12D, 0.25D, 0.03D);
        level.sendParticles(player, ParticleTypes.GLOW, false,
                danger.getX(), danger.getY() + danger.getBbHeight() + 0.1D, danger.getZ(),
                6, 0.35D, 0.25D, 0.35D, 0.02D);
    }
}

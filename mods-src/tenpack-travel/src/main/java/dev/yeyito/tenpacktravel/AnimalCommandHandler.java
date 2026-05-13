package dev.yeyito.tenpacktravel;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Optional;
import java.util.UUID;

final class AnimalCommandHandler {
    private static final int COMMAND_TICK_INTERVAL = 30;
    private static final double FOLLOW_START_DISTANCE_SQR = 16.0D;
    private static final double FOLLOW_STOP_DISTANCE_SQR = 6.25D;
    private static final double FOLLOW_MAX_LOADED_RANGE_SQR = 64.0D * 64.0D;
    private static final double STAY_DRIFT_DISTANCE_SQR = 9.0D;
    private static final double ROAM_DRIFT_DISTANCE_SQR = 12.0D * 12.0D;

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Mob mob) || !(event.getEntity() instanceof LivingEntity animal)) {
            return;
        }
        if ((animal.tickCount + animal.getId()) % COMMAND_TICK_INTERVAL != 0) {
            return;
        }
        AnimalCommand.Snapshot snapshot = AnimalCommand.snapshot(animal);
        if (snapshot.mode() == AnimalCommand.Mode.FREE) {
            return;
        }
        if (!(animal.level() instanceof ServerLevel level)) {
            return;
        }
        switch (snapshot.mode()) {
            case FOLLOW -> followOwner(level, mob, animal, snapshot);
            case STAY -> holdStay(mob, animal, snapshot);
            case ROAM -> holdRoam(mob, animal, snapshot);
            case FREE -> {
            }
        }
    }

    private static void followOwner(ServerLevel level, Mob mob, LivingEntity animal, AnimalCommand.Snapshot snapshot) {
        Optional<UUID> maybeOwner = snapshot.owner();
        if (maybeOwner.isEmpty()) {
            return;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(maybeOwner.get());
        if (owner == null || owner.level() != level || !owner.isAlive()) {
            mob.getNavigation().stop();
            return;
        }
        if (animal.getVehicle() != null || animal.getFirstPassenger() != null) {
            return;
        }
        if (animal instanceof TamableAnimal tameable) {
            tameable.setOrderedToSit(false);
        }
        mob.clearRestriction();
        double distance = animal.distanceToSqr(owner);
        if (distance > FOLLOW_MAX_LOADED_RANGE_SQR) {
            mob.getNavigation().stop();
            return;
        }
        if (distance <= FOLLOW_STOP_DISTANCE_SQR) {
            mob.getNavigation().stop();
            mob.getLookControl().setLookAt(owner, 30.0F, 30.0F);
            return;
        }
        if (distance >= FOLLOW_START_DISTANCE_SQR || mob.getNavigation().isDone()) {
            mob.getNavigation().moveTo(owner, AnimalCommand.followSpeed(animal, owner));
        }
    }

    private static void holdStay(Mob mob, LivingEntity animal, AnimalCommand.Snapshot snapshot) {
        BlockPos anchor = snapshot.anchor().orElse(animal.blockPosition());
        mob.restrictTo(anchor, 2);
        if (animal instanceof TamableAnimal tameable) {
            tameable.setOrderedToSit(true);
        }
        if (distanceToAnchorSqr(animal, anchor) > STAY_DRIFT_DISTANCE_SQR) {
            mob.getNavigation().moveTo(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D, 1.0D);
        } else {
            mob.getNavigation().stop();
        }
    }

    private static void holdRoam(Mob mob, LivingEntity animal, AnimalCommand.Snapshot snapshot) {
        BlockPos anchor = snapshot.anchor().orElse(animal.blockPosition());
        mob.restrictTo(anchor, 8);
        if (animal instanceof TamableAnimal tameable) {
            tameable.setOrderedToSit(false);
        }
        if (distanceToAnchorSqr(animal, anchor) > ROAM_DRIFT_DISTANCE_SQR) {
            mob.getNavigation().moveTo(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D, 1.0D);
        }
    }

    private static double distanceToAnchorSqr(LivingEntity animal, BlockPos anchor) {
        double dx = animal.getX() - (anchor.getX() + 0.5D);
        double dy = animal.getY() - anchor.getY();
        double dz = animal.getZ() - (anchor.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }
}

package dev.yeyito.tenpacktravel;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

final class MountedTravelBondHandler {
    private static final double RIDDEN_TRAVEL_XZ_SQR_THRESHOLD = 0.0025D;

    @SubscribeEvent
    public void onEntityMount(EntityMountEvent event) {
        if (!event.isMounting() || event.getLevel().isClientSide) {
            return;
        }
        if (event.getEntityMounting() instanceof Player player && event.getEntityBeingMounted() instanceof LivingEntity animal && AnimalBond.isBondable(animal)) {
            AnimalBond.mount(player, animal);
        }
    }

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof LivingEntity animal) || !AnimalBond.isBondable(animal)) {
            return;
        }
        if (riddenHorizontalTravelSqr(animal) < RIDDEN_TRAVEL_XZ_SQR_THRESHOLD) {
            return;
        }
        if (animal.getControllingPassenger() instanceof Player player) {
            AnimalBond.ride(player, animal);
            return;
        }
        if (animal.getFirstPassenger() instanceof Player player) {
            AnimalBond.ride(player, animal);
        }
    }

    private static double riddenHorizontalTravelSqr(LivingEntity animal) {
        double dx = animal.getX() - animal.xOld;
        double dz = animal.getZ() - animal.zOld;
        return dx * dx + dz * dz;
    }
}

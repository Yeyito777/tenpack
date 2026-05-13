package dev.yeyito.tenpacktravel;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Soft integration for AstikorCarts Redux. Detects real draft/cart movement without making Tenpack Travel depend on Astikor at compile time. */
final class AstikorDraftWorkHandler {
    private static final int WORK_CHECK_INTERVAL_TICKS = 60;
    private static final double CART_TRAVEL_XZ_SQR_THRESHOLD = 0.01D;

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        Entity cart = event.getEntity();
        if (cart.level().isClientSide || !(cart.level() instanceof ServerLevel)) {
            return;
        }
        if ((cart.tickCount + cart.getId()) % WORK_CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        if (!AstikorIntegration.isDrawnEntity(cart) || horizontalTravelSqr(cart) < CART_TRAVEL_XZ_SQR_THRESHOLD) {
            return;
        }

        Entity pulling = AstikorIntegration.getPulling(cart);
        if (!(pulling instanceof LivingEntity animal) || !AnimalEligibility.isBondable(animal)) {
            return;
        }
        Player handler = controllingPlayer(animal, cart);
        if (handler == null) {
            return;
        }

        if (AnimalBond.snapshot(animal, handler).ownedByOther()) {
            return;
        }

        AnimalCare.draftWork(handler, animal);
        AnimalBond.draftWork(handler, animal);
    }

    private static Player controllingPlayer(LivingEntity animal, Entity cart) {
        if (animal.getControllingPassenger() instanceof Player player) {
            return player;
        }
        if (animal.getFirstPassenger() instanceof Player player) {
            return player;
        }
        if (cart.getControllingPassenger() instanceof Player player) {
            return player;
        }
        return null;
    }

    private static double horizontalTravelSqr(Entity entity) {
        double dx = entity.getX() - entity.xOld;
        double dz = entity.getZ() - entity.zOld;
        return dx * dx + dz * dz;
    }

}

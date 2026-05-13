package dev.yeyito.tenpackfoodui;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class FoodBalanceServer {
    private final Map<UUID, FoodBalanceState> states = new HashMap<>();

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FoodBalanceState state = stateFor(player);
            state.reset(player.getX(), player.getY(), player.getZ());
            sync(player, state);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FoodBalanceState state = stateFor(player);
            state.reset(player.getX(), player.getY(), player.getZ());
            sync(player, state);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        states.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        FoodBalanceState state = stateFor(player);
        state.tickBucket++;
        if (state.tickBucket < 20) {
            return;
        }
        state.tickBucket = 0;

        double dx = player.getX() - state.lastX;
        double dy = player.getY() - state.lastY;
        double dz = player.getZ() - state.lastZ;
        double movedSqr = dx * dx + dy * dy + dz * dz;
        state.lastX = player.getX();
        state.lastY = player.getY();
        state.lastZ = player.getZ();

        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        double baseDrain = 0.0D;
        if (movedSqr > 0.0004D) {
            baseDrain += 0.004D;
        }
        if (player.isPassenger()) {
            baseDrain += 0.001D;
        }
        if (player.isUsingItem()) {
            baseDrain += 0.002D;
        }
        if (player.isSprinting() || player.isSwimming()) {
            baseDrain += 0.008D;
        }
        if (baseDrain <= 0.0D) {
            return;
        }

        if (state.activeDecayTick()) {
            sync(player, state);
        }
    }

    @SubscribeEvent
    public void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getItem();
        if (stack.isEmpty()) {
            return;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        FoodBalanceState state = stateFor(player);
        if (!state.knows(itemId)) {
            return;
        }
        state.add(itemId);
        sync(player, state);
    }

    private FoodBalanceState stateFor(ServerPlayer player) {
        return states.computeIfAbsent(player.getUUID(), ignored -> {
            FoodBalanceState state = new FoodBalanceState();
            state.reset(player.getX(), player.getY(), player.getZ());
            return state;
        });
    }

    private static void sync(ServerPlayer player, FoodBalanceState state) {
        PacketDistributor.sendToPlayer(player, state.payload());
    }
}

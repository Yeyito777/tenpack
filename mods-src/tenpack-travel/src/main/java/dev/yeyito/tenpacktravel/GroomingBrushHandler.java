package dev.yeyito.tenpacktravel;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.function.Supplier;

final class GroomingBrushHandler {
    private final Supplier<? extends Item> brush;

    GroomingBrushHandler(Supplier<? extends Item> brush) {
        this.brush = brush;
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!stack.is(brush.get())) {
            return;
        }

        AnimalInspectionReport report = AnimalInspectionReport.from(event.getTarget(), player);
        if (report == null) {
            if (!player.level().isClientSide) {
                player.sendSystemMessage(Component.translatable("item.tenpack_travel.grooming_brush.no_notes"));
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        Level level = player.level();
        if (!level.isClientSide) {
            LivingEntity inspected = event.getTarget() instanceof LivingEntity living ? living : null;
            if (inspected != null) {
                AnimalBond.brush(player, inspected);
                AnimalCare.CareUpdate care = AnimalCare.groom(player, inspected);
                if (level instanceof ServerLevel serverLevel) {
                    AnimalCare.playGroomingReaction(serverLevel, inspected, player, care.firstToday());
                }
                player.displayClientMessage(Component.translatable(care.firstToday()
                                ? "item.tenpack_travel.grooming_brush.groomed_today"
                                : "item.tenpack_travel.grooming_brush.already_groomed",
                        inspected.getDisplayName(), care.mood(), care.careSummary()), true);
                report = AnimalInspectionReport.from(inspected, player);
            }
            boolean exact = player.isCreative() || player.isSpectator();
            if (player instanceof ServerPlayer serverPlayer && inspected != null) {
                PacketDistributor.sendToPlayer(serverPlayer, AnimalInspectionPayload.from(inspected, report, exact));
            } else {
                for (Component line : report.lines(exact)) {
                    player.sendSystemMessage(line);
                }
            }
            if (player instanceof ServerPlayer && !player.hasInfiniteMaterials()) {
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(event.getHand()));
            }
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}

package dev.yeyito.tenpacktravel;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class LodestoneCompassPolicyHandler {
    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!event.getItemStack().is(Items.COMPASS)) {
            return;
        }
        if (!event.getLevel().getBlockState(event.getPos()).is(Blocks.LODESTONE)) {
            return;
        }
        event.getEntity().displayClientMessage(Component.translatable("message.tenpack_travel.lodestone_compass.disabled"), true);
        event.setCancellationResult(InteractionResult.CONSUME);
        event.setCanceled(true);
    }
}

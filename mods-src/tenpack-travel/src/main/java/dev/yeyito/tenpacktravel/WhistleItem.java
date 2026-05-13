package dev.yeyito.tenpacktravel;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class WhistleItem extends Item {
    public WhistleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide) {
            if (player.isShiftKeyDown()) {
                openClientWhistleCommands();
            } else {
                PacketDistributor.sendToServer(WhistlePayload.INSTANCE);
            }
        }
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.tenpack_travel.whistle.tooltip.call").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.tenpack_travel.whistle.tooltip.commands").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void openClientWhistleCommands() {
        try {
            Class.forName("dev.yeyito.tenpacktravel.TenpackTravelClient")
                    .getMethod("openWhistleCommands")
                    .invoke(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to open Tenpack Travel whistle command screen", exception);
        }
    }
}

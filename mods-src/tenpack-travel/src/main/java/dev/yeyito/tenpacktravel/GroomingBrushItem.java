package dev.yeyito.tenpacktravel;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class GroomingBrushItem extends Item {
    public GroomingBrushItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.tenpack_travel.grooming_brush.tooltip.groom").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.tenpack_travel.grooming_brush.tooltip.notes").withStyle(ChatFormatting.DARK_GRAY));
    }
}

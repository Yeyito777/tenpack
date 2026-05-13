package dev.yeyito.tenpacktravel;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.List;

public class ChartTableBlock extends CartographyTableBlock {
    public ChartTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("block.tenpack_travel.chart_table.tooltip.infrastructure").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("block.tenpack_travel.chart_table.tooltip.no_gps").withStyle(ChatFormatting.DARK_GRAY));
    }
}

package dev.yeyito.tenpacktravel;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.List;

public class MooringPostBlock extends FenceBlock {
    public MooringPostBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("block.tenpack_travel.mooring_post.tooltip.infrastructure").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("block.tenpack_travel.mooring_post.tooltip.no_gps").withStyle(ChatFormatting.DARK_GRAY));
    }
}

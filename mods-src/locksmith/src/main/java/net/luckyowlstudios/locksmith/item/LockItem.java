package net.luckyowlstudios.locksmith.item;

import java.util.List;
import net.luckyowlstudios.locksmith.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;

public class LockItem extends Item {
   public LockItem(Properties properties) {
      super(properties.stacksTo(1).rarity(Rarity.EPIC));
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
      super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
      if (stack.is((Item)ModItems.GOLDEN_LOCK.get())) {
         tooltipComponents.add(Component.translatable("tooltip.locksmith.golden_lock").withStyle(ChatFormatting.GRAY));
      }

      if (stack.is((Item)ModItems.TRIAL_LOCK.get())) {
         tooltipComponents.add(Component.translatable("tooltip.locksmith.trial_lock").withStyle(ChatFormatting.GRAY));
      }
   }
}

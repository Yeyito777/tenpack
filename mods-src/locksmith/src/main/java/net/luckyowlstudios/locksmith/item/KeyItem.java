package net.luckyowlstudios.locksmith.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.LockCode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class KeyItem extends Item implements ICurioItem {
   public KeyItem(Properties properties) {
      super(properties.stacksTo(1));
   }

   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
      super.inventoryTick(stack, level, entity, slotId, isSelected);
      if (!stack.getComponents().has(DataComponents.LOCK)) {
         LockCode lockCode = generateRandomCode();
         stack.set(DataComponents.LOCK, lockCode);
      }
   }

   private static LockCode generateRandomCode() {
      String code = String.valueOf((int)(Math.random() * 90000.0 + 10000.0));
      return new LockCode(code);
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
      super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
      LockCode lockCode = (LockCode)stack.get(DataComponents.LOCK);
      if (Screen.hasShiftDown()) {
         if (lockCode != null && !lockCode.key().isEmpty()) {
            tooltipComponents.add(Component.translatable("tooltip.locksmith.code", new Object[]{lockCode.key()}).withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable("tooltip.locksmith.key_duplicate", new Object[]{lockCode.key()}).withStyle(ChatFormatting.GRAY));
         } else {
            tooltipComponents.add(Component.translatable("tooltip.locksmith.no_code").withStyle(ChatFormatting.GRAY));
         }
      } else {
         tooltipComponents.add(
            Component.translatable("tooltip.locksmith.reveal_code", new Object[]{Component.keybind(Minecraft.getInstance().options.keyShift.getName())})
               .withStyle(ChatFormatting.GRAY)
         );
      }
   }

   public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
      // Tenpack patch: right-clicking with a key is used to lock/open
      // containers. Do not let Curios consume that same right-click and move
      // the key into the hidden key slot, which makes the key look like it
      // disappeared and bypasses the intended "carry it in inventory" rule.
      return false;
   }
}

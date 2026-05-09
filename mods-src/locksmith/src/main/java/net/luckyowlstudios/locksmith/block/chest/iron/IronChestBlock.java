package net.luckyowlstudios.locksmith.block.chest.iron;

import net.luckyowlstudios.locksmith.init.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;

public class IronChestBlock extends ChestBlock {
   public IronChestBlock(Properties properties) {
      super(properties, ModBlockEntityTypes.IRON_CHEST_BLOCK_ENTITY::get);
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new IronChestBlockEntity(pos, state);
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (stack.getItem() instanceof DyeItem dyeItem) {
         if (level.getBlockEntity(pos) instanceof IronChestBlockEntity containerBlockEntity) {
            DataComponentMap var12 = DataComponentMap.builder()
               .addAll(containerBlockEntity.components())
               .set(DataComponents.DYED_COLOR, new DyedItemColor(dyeItem.getDyeColor().getTextureDiffuseColor(), true))
               .build();
            containerBlockEntity.setComponents(var12);
            containerBlockEntity.setChanged();
            return ItemInteractionResult.SUCCESS;
         } else {
            return ItemInteractionResult.CONSUME_PARTIAL;
         }
      } else {
         return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
      }
   }
}

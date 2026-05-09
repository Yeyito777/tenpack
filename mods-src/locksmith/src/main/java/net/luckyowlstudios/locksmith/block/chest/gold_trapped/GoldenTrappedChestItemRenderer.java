package net.luckyowlstudios.locksmith.block.chest.gold_trapped;

import com.mojang.blaze3d.vertex.PoseStack;
import net.luckyowlstudios.locksmith.init.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class GoldenTrappedChestItemRenderer extends BlockEntityWithoutLevelRenderer {
   public GoldenTrappedChestItemRenderer() {
      super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
   }

   public void renderByItem(
      ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay
   ) {
      Minecraft.getInstance()
         .getBlockEntityRenderDispatcher()
         .renderItem(
            new GoldenTrappedChestBlockEntity(BlockPos.ZERO, ((Block)ModBlocks.GOLDEN_TRAPPED_CHEST.get()).defaultBlockState()),
            poseStack,
            buffer,
            packedLight,
            packedOverlay
         );
   }
}

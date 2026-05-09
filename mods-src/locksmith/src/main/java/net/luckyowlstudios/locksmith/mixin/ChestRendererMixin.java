package net.luckyowlstudios.locksmith.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.Objects;
import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.init.ModDataComponents;
import net.luckyowlstudios.locksmith.overrides.LockModel;
import net.luckyowlstudios.locksmith.util.LockHandler;
import net.luckyowlstudios.locksmith.util.LockType;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ChestRenderer.class})
public abstract class ChestRendererMixin<T extends BlockEntity & LidBlockEntity> implements BlockEntityRenderer<T> {
   @Unique
   private ModelPart locksmith$lock;

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   private void locksmith$initRenderer(Context context, CallbackInfo ci) {
      ModelPart modelPart = context.bakeLayer(LockModel.LAYER_LOCATION);
      this.locksmith$lock = modelPart.getChild("lock");
   }

   @Inject(
      method = {"render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"},
      at = {@At("TAIL")}
   )
   private void locksmith$renderLock(
      T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, CallbackInfo ci
   ) {
      if (blockEntity instanceof ChestBlockEntity chestBlockEntity) {
         if (LockHandler.containerHasLock(chestBlockEntity)) {
            BlockState blockState = blockEntity.getBlockState();
            ChestType chestType = (ChestType)blockState.getValue(ChestBlock.TYPE);
            if (chestType == ChestType.LEFT) {
               return;
            }

            poseStack.pushPose();
            poseStack.translate(0.5F, 1.5F, 0.5F);
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            float f = ((Direction)blockEntity.getBlockState().getValue(ChestBlock.FACING)).toYRot();
            poseStack.mulPose(Axis.YP.rotationDegrees(f));
            if (chestType != ChestType.SINGLE) {
               poseStack.translate(0.5F, 0.0F, 0.0F);
            }

            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(Objects.requireNonNull(luckys_locksmith$getTexture(chestBlockEntity))));
            this.locksmith$lock.render(poseStack, consumer, packedLight, packedOverlay);
            poseStack.popPose();
         }
      }
   }

   @Unique
   private static ResourceLocation luckys_locksmith$getTexture(BaseContainerBlockEntity containerBlockEntity) {
      if (containerBlockEntity.components().has((DataComponentType)ModDataComponents.LOCK_TYPE.get())) {
         LockType var1 = (LockType)containerBlockEntity.components().get((DataComponentType)ModDataComponents.LOCK_TYPE.get());

         return switch (var1) {
            case null -> null;
            case GOLDEN -> Locksmith.id("textures/entity/lock/chest/golden_lock.png");
            case TRIAL -> Locksmith.id("textures/entity/lock/chest/trial_lock.png");
            default -> Locksmith.id("textures/entity/lock/chest/lock.png");
         };
      } else {
         return Locksmith.id("textures/entity/lock/chest/lock.png");
      }
   }
}

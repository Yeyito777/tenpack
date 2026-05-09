package net.luckyowlstudios.locksmith.overrides;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.init.ModDataComponents;
import net.luckyowlstudios.locksmith.util.LockHandler;
import net.luckyowlstudios.locksmith.util.LockType;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AddBarrelRenderer<T extends BlockEntity> implements BlockEntityRenderer<BarrelBlockEntity> {
   public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Locksmith.id("barrel_lock"), "main");
   private final ModelPart lock;

   public AddBarrelRenderer(Context context) {
      ModelPart modelpart = context.bakeLayer(LAYER_LOCATION);
      this.lock = modelpart.getChild("lock");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition lock = partdefinition.addOrReplaceChild(
         "lock",
         CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -16.0F, -8.0F, 5.0F, 16.0F, 16.0F, new CubeDeformation(0.1F)),
         PartPose.offset(0.0F, 24.0F, 0.0F)
      );
      PartDefinition cube_r1 = lock.addOrReplaceChild(
         "cube_r1",
         CubeListBuilder.create()
            .texOffs(12, 32)
            .addBox(-2.0F, -4.0F, -9.5F, 4.0F, 2.0F, 0.0F, new CubeDeformation(0.0F))
            .texOffs(0, 32)
            .addBox(-2.5F, -2.0F, -10.0F, 5.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, -7.0F, -2.0F, -1.5708F, 0.0F, 0.0F)
      );
      PartDefinition cube_r2 = lock.addOrReplaceChild(
         "cube_r2",
         CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -8.0F, -8.0F, 5.0F, 16.0F, 16.0F, new CubeDeformation(0.1F)),
         PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.0F, 1.5708F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public void render(BarrelBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
      if (LockHandler.containerHasLock(blockEntity)) {
         poseStack.pushPose();
         BlockState state = blockEntity.getBlockState();
         Direction facing = (Direction)state.getValue(BarrelBlock.FACING);
         Axis axis = facing.getAxis();
         boolean isUpsideDown = facing == Direction.DOWN;
         poseStack.translate(0.5F, isUpsideDown ? -0.5F : 1.5F, 0.5F);
         poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(isUpsideDown ? 0.0F : 180.0F));
         if (axis != Axis.Y) {
            if (facing == Direction.NORTH) {
               poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
               poseStack.translate(0.0F, -1.0F, 1.0F);
            } else if (facing == Direction.SOUTH) {
               poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
               poseStack.translate(0.0F, -1.0F, -1.0F);
            } else if (facing == Direction.WEST) {
               poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90.0F));
               poseStack.translate(-1.0F, -1.0F, 0.0F);
            } else if (facing == Direction.EAST) {
               poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90.0F));
               poseStack.translate(1.0F, -1.0F, 0.0F);
            }
         }

         float f = ((Direction)blockEntity.getBlockState().getValue(BarrelBlock.FACING)).toYRot();
         poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(f));
         VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(getTexture(blockEntity)));
         int light = this.getFrontLightLevel(blockEntity);
         this.lock.render(poseStack, consumer, light, packedOverlay);
         poseStack.popPose();
      }
   }

   public static ResourceLocation getTexture(BaseContainerBlockEntity containerBlockEntity) {
      if (containerBlockEntity.components().has((DataComponentType)ModDataComponents.LOCK_TYPE.get())) {
         return switch ((LockType)containerBlockEntity.components().get((DataComponentType)ModDataComponents.LOCK_TYPE.get())) {
            case GOLDEN -> Locksmith.id("textures/entity/lock/barrel/golden_lock.png");
            case TRIAL -> Locksmith.id("textures/entity/lock/barrel/trial_lock.png");
            default -> Locksmith.id("textures/entity/lock/barrel/lock.png");
         };
      } else {
         return Locksmith.id("textures/entity/lock/barrel/lock.png");
      }
   }

   private int getFrontLightLevel(BarrelBlockEntity blockEntity) {
      Level level = blockEntity.getLevel();
      if (level == null) {
         return 15728880;
      } else {
         BlockState state = blockEntity.getBlockState();
         Direction facing = (Direction)state.getValue(BarrelBlock.FACING);
         BlockPos frontPos = blockEntity.getBlockPos().relative(facing);
         BlockState frontState = level.getBlockState(frontPos);
         boolean frontOpaque = frontState.isSolidRender(level, frontPos);
         BlockPos lightSamplePos = frontOpaque ? blockEntity.getBlockPos() : frontPos;
         int blockLight = level.getBrightness(LightLayer.BLOCK, lightSamplePos);
         int skyLight = level.getBrightness(LightLayer.SKY, lightSamplePos);
         return blockLight | skyLight << 20;
      }
   }
}

package net.luckyowlstudios.locksmith;

import net.luckyowlstudios.locksmith.block.chest.gold.GoldenChestItemRenderer;
import net.luckyowlstudios.locksmith.block.chest.gold.GoldenChestRenderer;
import net.luckyowlstudios.locksmith.block.chest.gold_trapped.GoldenTrappedChestItemRenderer;
import net.luckyowlstudios.locksmith.block.chest.gold_trapped.GoldenTrappedChestRenderer;
import net.luckyowlstudios.locksmith.block.chest.iron.IronChestItemRenderer;
import net.luckyowlstudios.locksmith.block.chest.iron.IronChestRenderer;
import net.luckyowlstudios.locksmith.block.chest.iron_trapped.IronTrappedChestItemRenderer;
import net.luckyowlstudios.locksmith.block.chest.iron_trapped.IronTrappedChestRenderer;
import net.luckyowlstudios.locksmith.init.ModBlockEntityTypes;
import net.luckyowlstudios.locksmith.init.ModBlocks;
import net.luckyowlstudios.locksmith.overrides.AddBarrelRenderer;
import net.luckyowlstudios.locksmith.overrides.LockModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(
   modid = "locksmith",
   value = {Dist.CLIENT}
)
public class LocksmithClient {
   public LocksmithClient(ModContainer container) {
      container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
   }

   @SubscribeEvent
   public static void onClientSetup(FMLClientSetupEvent event) {
   }

   @SubscribeEvent
   public static void registerBER(RegisterRenderers event) {
      event.registerBlockEntityRenderer(ModBlockEntityTypes.IRON_CHEST_BLOCK_ENTITY.get(), IronChestRenderer::new);
      event.registerBlockEntityRenderer(ModBlockEntityTypes.GOLDEN_CHEST_BLOCK_ENTITY.get(), GoldenChestRenderer::new);
      event.registerBlockEntityRenderer(ModBlockEntityTypes.IRON_TRAPPED_CHEST_BLOCK_ENTITY.get(), IronTrappedChestRenderer::new);
      event.registerBlockEntityRenderer(ModBlockEntityTypes.GOLDEN_TRAPPED_CHEST_BLOCK_ENTITY.get(), GoldenTrappedChestRenderer::new);
      event.registerBlockEntityRenderer(BlockEntityType.BARREL, AddBarrelRenderer::new);
   }

   @SubscribeEvent
   public static void registerBERI(RegisterClientExtensionsEvent event) {
      event.registerItem(new IClientItemExtensions() {
         @NotNull
         public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return new IronChestItemRenderer();
         }
      }, new Item[]{ModBlocks.IRON_CHEST.asItem()});
      event.registerItem(new IClientItemExtensions() {
         @NotNull
         public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return new GoldenChestItemRenderer();
         }
      }, new Item[]{ModBlocks.GOLDEN_CHEST.asItem()});
      event.registerItem(new IClientItemExtensions() {
         @NotNull
         public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return new IronTrappedChestItemRenderer();
         }
      }, new Item[]{ModBlocks.IRON_TRAPPED_CHEST.asItem()});
      event.registerItem(new IClientItemExtensions() {
         @NotNull
         public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return new GoldenTrappedChestItemRenderer();
         }
      }, new Item[]{ModBlocks.GOLDEN_TRAPPED_CHEST.asItem()});
   }

   @SubscribeEvent
   public static void registerLayerDefinitions(RegisterLayerDefinitions event) {
      event.registerLayerDefinition(LockModel.LAYER_LOCATION, LockModel::createLock);
      event.registerLayerDefinition(AddBarrelRenderer.LAYER_LOCATION, AddBarrelRenderer::createBodyLayer);
   }
}

package net.luckyowlstudios.locksmith;

import com.mojang.logging.LogUtils;
import java.util.List;
import net.luckyowlstudios.locksmith.init.ModBlockEntityTypes;
import net.luckyowlstudios.locksmith.init.ModBlocks;
import net.luckyowlstudios.locksmith.init.ModCreativeModeTabs;
import net.luckyowlstudios.locksmith.init.ModDataComponents;
import net.luckyowlstudios.locksmith.init.ModItems;
import net.luckyowlstudios.locksmith.init.ModLootModifiers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

@Mod("locksmith")
public class Locksmith {
   public static final String MOD_ID = "locksmith";
   private static final Logger LOGGER = LogUtils.getLogger();

   public Locksmith(IEventBus modEventBus, ModContainer modContainer) {
      modEventBus.addListener(this::commonSetup);
      NeoForge.EVENT_BUS.register(this);
      ModItems.register(modEventBus);
      ModBlocks.register(modEventBus);
      ModBlockEntityTypes.register(modEventBus);
      ModCreativeModeTabs.register(modEventBus);
      modEventBus.addListener(ModCreativeModeTabs::addCreative);
      ModDataComponents.register(modEventBus);
      ModLootModifiers.register(modEventBus);
      modContainer.registerConfig(Type.COMMON, LocksmithConfig.SPEC);
   }

   private void commonSetup(FMLCommonSetupEvent event) {
      LOGGER.info("HELLO FROM COMMON SETUP");
      if (LocksmithConfig.LOG_DIRT_BLOCK.getAsBoolean()) {
         LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
      }

      LOGGER.info("{}{}", LocksmithConfig.MAGIC_NUMBER_INTRODUCTION.get(), LocksmithConfig.MAGIC_NUMBER.getAsInt());
      ((List)LocksmithConfig.ITEM_STRINGS.get()).forEach(item -> LOGGER.info("ITEM >> {}", item));
   }

   public static ResourceLocation id(String name) {
      return ResourceLocation.fromNamespaceAndPath("locksmith", name);
   }

   @SubscribeEvent
   public void onServerStarting(ServerStartingEvent event) {
      LOGGER.info("HELLO from server starting");
   }
}

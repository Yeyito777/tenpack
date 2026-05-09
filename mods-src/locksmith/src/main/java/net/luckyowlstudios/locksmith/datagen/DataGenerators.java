package net.luckyowlstudios.locksmith.datagen;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.luckyowlstudios.locksmith.datagen.blocks.ModBlockTagProvider;
import net.luckyowlstudios.locksmith.datagen.items.ModItemTagProvider;
import net.luckyowlstudios.locksmith.datagen.types.ModBlockLootTableProvider;
import net.luckyowlstudios.locksmith.datagen.types.ModBlockStateProvider;
import net.luckyowlstudios.locksmith.datagen.types.ModDamageTypeTagsProvider;
import net.luckyowlstudios.locksmith.datagen.types.ModDataMapProvider;
import net.luckyowlstudios.locksmith.datagen.types.ModDataPackProvider;
import net.luckyowlstudios.locksmith.datagen.types.ModGlobalLootModifierProvider;
import net.luckyowlstudios.locksmith.datagen.types.ModItemModelProvider;
import net.luckyowlstudios.locksmith.datagen.types.ModRecipeProvider;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableProvider.SubProviderEntry;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(
   modid = "locksmith"
)
public class DataGenerators {
   @SubscribeEvent
   public static void gatherData(GatherDataEvent event) {
      DataGenerator generator = event.getGenerator();
      PackOutput packOutput = generator.getPackOutput();
      ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
      CompletableFuture<Provider> lookupProvider = event.getLookupProvider();
      generator.addProvider(
         event.includeServer(),
         new LootTableProvider(
            packOutput, Collections.emptySet(), List.of(new SubProviderEntry(ModBlockLootTableProvider::new, LootContextParamSets.BLOCK)), lookupProvider
         )
      );
      generator.addProvider(event.includeServer(), new ModRecipeProvider(packOutput, lookupProvider));
      BlockTagsProvider blockTagsProvider = new ModBlockTagProvider(packOutput, lookupProvider, existingFileHelper);
      generator.addProvider(event.includeServer(), blockTagsProvider);
      generator.addProvider(event.includeServer(), new ModItemTagProvider(packOutput, lookupProvider, blockTagsProvider.contentsGetter(), existingFileHelper));
      generator.addProvider(event.includeServer(), new ModDataMapProvider(packOutput, lookupProvider));
      generator.addProvider(event.includeClient(), new ModItemModelProvider(packOutput, existingFileHelper));
      generator.addProvider(event.includeClient(), new ModBlockStateProvider(packOutput, existingFileHelper));
      generator.addProvider(event.includeServer(), new ModDamageTypeTagsProvider(packOutput, lookupProvider, existingFileHelper));
      generator.addProvider(event.includeServer(), new ModDataPackProvider(packOutput, lookupProvider));
      generator.addProvider(event.includeServer(), new ModGlobalLootModifierProvider(packOutput, lookupProvider));
   }
}

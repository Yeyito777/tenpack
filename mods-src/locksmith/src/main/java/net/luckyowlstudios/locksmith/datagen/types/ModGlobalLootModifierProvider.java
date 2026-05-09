package net.luckyowlstudios.locksmith.datagen.types;

import java.util.concurrent.CompletableFuture;
import net.luckyowlstudios.locksmith.datagen.loot.AddItemModifier;
import net.luckyowlstudios.locksmith.init.ModItems;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.LootTableIdCondition.Builder;

public class ModGlobalLootModifierProvider extends GlobalLootModifierProvider {
   public ModGlobalLootModifierProvider(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries, "locksmith");
   }

   protected void start() {
      this.add(
         "golden_key_to_piglin_brute",
         new AddItemModifier(
            new LootItemCondition[]{new Builder(ResourceLocation.withDefaultNamespace("entities/piglin_brute")).build()},
            ((Item)ModItems.GOLDEN_KEY.get()).asItem()
         ),
         new ICondition[0]
      );
      this.add(
         "golden_key_to_nether_fortress_chest",
         new AddItemModifier(
            new LootItemCondition[]{new Builder(ResourceLocation.withDefaultNamespace("chests/nether_bridge")).build()},
            ((Item)ModItems.GOLDEN_KEY.get()).asItem()
         ),
         new ICondition[0]
      );
   }
}

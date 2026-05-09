package net.luckyowlstudios.locksmith.datagen.types;

import java.util.Set;
import net.luckyowlstudios.locksmith.init.ModBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class ModBlockLootTableProvider extends BlockLootSubProvider {
   public ModBlockLootTableProvider(Provider registries) {
      super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
   }

   protected void generate() {
      this.dropSelf((Block)ModBlocks.IRON_CHEST.get());
      this.dropSelf((Block)ModBlocks.GOLDEN_CHEST.get());
      this.dropSelf((Block)ModBlocks.IRON_TRAPPED_CHEST.get());
      this.dropSelf((Block)ModBlocks.GOLDEN_TRAPPED_CHEST.get());
   }

   @NotNull
   protected Iterable<Block> getKnownBlocks() {
      return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
   }
}

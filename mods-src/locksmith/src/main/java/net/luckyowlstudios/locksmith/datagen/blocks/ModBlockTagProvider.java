package net.luckyowlstudios.locksmith.datagen.blocks;

import java.util.concurrent.CompletableFuture;
import net.luckyowlstudios.locksmith.init.ModBlocks;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags.Blocks;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

public class ModBlockTagProvider extends BlockTagsProvider {
   public ModBlockTagProvider(PackOutput output, CompletableFuture<Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
      super(output, lookupProvider, "locksmith", existingFileHelper);
   }

   protected void addTags(Provider provider) {
      this.tag(BlockTags.LAVA_POOL_STONE_CANNOT_REPLACE)
         .add((Block)ModBlocks.IRON_CHEST.get())
         .add((Block)ModBlocks.GOLDEN_CHEST.get())
         .add((Block)ModBlocks.IRON_TRAPPED_CHEST.get())
         .add((Block)ModBlocks.GOLDEN_TRAPPED_CHEST.get());
      this.tag(Blocks.CHESTS).add((Block)ModBlocks.IRON_CHEST.get()).add((Block)ModBlocks.GOLDEN_CHEST.get());
      this.tag(Blocks.CHESTS_TRAPPED).add((Block)ModBlocks.IRON_TRAPPED_CHEST.get()).add((Block)ModBlocks.GOLDEN_TRAPPED_CHEST.get());
      this.tag(BlockTags.FEATURES_CANNOT_REPLACE)
         .add((Block)ModBlocks.IRON_CHEST.get())
         .add((Block)ModBlocks.GOLDEN_CHEST.get())
         .add((Block)ModBlocks.IRON_TRAPPED_CHEST.get())
         .add((Block)ModBlocks.GOLDEN_TRAPPED_CHEST.get());
      this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
         .add((Block)ModBlocks.IRON_CHEST.get())
         .add((Block)ModBlocks.GOLDEN_CHEST.get())
         .add((Block)ModBlocks.IRON_TRAPPED_CHEST.get())
         .add((Block)ModBlocks.GOLDEN_TRAPPED_CHEST.get());
      this.tag(BlockTags.MINEABLE_WITH_AXE)
         .add((Block)ModBlocks.IRON_CHEST.get())
         .add((Block)ModBlocks.GOLDEN_CHEST.get())
         .add((Block)ModBlocks.IRON_TRAPPED_CHEST.get())
         .add((Block)ModBlocks.GOLDEN_TRAPPED_CHEST.get());
      this.tag(BlockTags.GUARDED_BY_PIGLINS).add((Block)ModBlocks.GOLDEN_CHEST.get()).add((Block)ModBlocks.GOLDEN_TRAPPED_CHEST.get());
   }
}

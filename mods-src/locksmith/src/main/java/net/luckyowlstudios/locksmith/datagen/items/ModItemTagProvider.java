package net.luckyowlstudios.locksmith.datagen.items;

import java.util.concurrent.CompletableFuture;
import net.luckyowlstudios.locksmith.init.ModItems;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider.TagLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags.Items;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

public class ModItemTagProvider extends ItemTagsProvider {
   public ModItemTagProvider(
      PackOutput output,
      CompletableFuture<Provider> lookupProvider,
      CompletableFuture<TagLookup<Block>> blockTags,
      @Nullable ExistingFileHelper existingFileHelper
   ) {
      super(output, lookupProvider, blockTags, "locksmith", existingFileHelper);
   }

   protected void addTags(Provider provider) {
      this.tag(Items.CHESTS);
      this.tag(Items.CHESTS_TRAPPED);
      this.tag(ModItemTags.KEYS)
         .add(
            new Item[]{
               (Item)ModItems.KEY.get(),
               (Item)ModItems.GOLDEN_KEY.get(),
               net.minecraft.world.item.Items.TRIAL_KEY,
               net.minecraft.world.item.Items.OMINOUS_TRIAL_KEY
            }
         );
   }
}

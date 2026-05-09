package net.luckyowlstudios.locksmith.datagen.types;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.DamageTypeTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

public class ModDamageTypeTagsProvider extends DamageTypeTagsProvider {
   public ModDamageTypeTagsProvider(PackOutput output, CompletableFuture<Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
      super(output, lookupProvider, "locksmith", existingFileHelper);
   }

   protected void addTags(Provider provider) {
      super.addTags(provider);
   }
}

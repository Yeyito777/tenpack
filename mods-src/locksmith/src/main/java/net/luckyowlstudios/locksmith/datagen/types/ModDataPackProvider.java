package net.luckyowlstudios.locksmith.datagen.types;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.luckyowlstudios.locksmith.init.ModDamageTypes;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

public class ModDataPackProvider extends DatapackBuiltinEntriesProvider {
   public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder().add(Registries.DAMAGE_TYPE, ModDamageTypes::bootstrap);

   public ModDataPackProvider(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries, BUILDER, Set.of("locksmith"));
   }
}

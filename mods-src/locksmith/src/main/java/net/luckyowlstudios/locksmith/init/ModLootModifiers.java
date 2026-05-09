package net.luckyowlstudios.locksmith.init;

import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.luckyowlstudios.locksmith.datagen.loot.AddItemModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries.Keys;

public class ModLootModifiers {
   public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS = DeferredRegister.create(
      Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, "locksmith"
   );
   public static final Supplier<MapCodec<? extends IGlobalLootModifier>> ADD_ITEM = LOOT_MODIFIER_SERIALIZERS.register("add_item", () -> AddItemModifier.CODEC);

   public static void register(IEventBus eventBus) {
      LOOT_MODIFIER_SERIALIZERS.register(eventBus);
   }
}

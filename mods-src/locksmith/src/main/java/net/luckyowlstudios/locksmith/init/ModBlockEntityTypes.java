package net.luckyowlstudios.locksmith.init;

import java.util.function.Supplier;
import net.luckyowlstudios.locksmith.block.chest.gold.GoldenChestBlockEntity;
import net.luckyowlstudios.locksmith.block.chest.gold_trapped.GoldenTrappedChestBlockEntity;
import net.luckyowlstudios.locksmith.block.chest.iron.IronChestBlockEntity;
import net.luckyowlstudios.locksmith.block.chest.iron_trapped.IronTrappedChestBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntityTypes {
   public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, "locksmith");
   public static final Supplier<BlockEntityType<IronChestBlockEntity>> IRON_CHEST_BLOCK_ENTITY = BLOCK_ENTITIES.register(
      "iron_chest", () -> Builder.of(IronChestBlockEntity::new, new Block[]{(Block)ModBlocks.IRON_CHEST.get()}).build(null)
   );
   public static final Supplier<BlockEntityType<GoldenChestBlockEntity>> GOLDEN_CHEST_BLOCK_ENTITY = BLOCK_ENTITIES.register(
      "golden_chest", () -> Builder.of(GoldenChestBlockEntity::new, new Block[]{(Block)ModBlocks.GOLDEN_CHEST.get()}).build(null)
   );
   public static final Supplier<BlockEntityType<IronTrappedChestBlockEntity>> IRON_TRAPPED_CHEST_BLOCK_ENTITY = BLOCK_ENTITIES.register(
      "iron_trapped_chest", () -> Builder.of(IronTrappedChestBlockEntity::new, new Block[]{(Block)ModBlocks.IRON_TRAPPED_CHEST.get()}).build(null)
   );
   public static final Supplier<BlockEntityType<GoldenTrappedChestBlockEntity>> GOLDEN_TRAPPED_CHEST_BLOCK_ENTITY = BLOCK_ENTITIES.register(
      "golden_trapped_chest", () -> Builder.of(GoldenTrappedChestBlockEntity::new, new Block[]{(Block)ModBlocks.GOLDEN_TRAPPED_CHEST.get()}).build(null)
   );

   public static void register(IEventBus eventBus) {
      BLOCK_ENTITIES.register(eventBus);
   }
}

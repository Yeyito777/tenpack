package net.luckyowlstudios.locksmith.init;

import java.util.function.Supplier;
import net.luckyowlstudios.locksmith.block.chest.gold.GoldenChestBlock;
import net.luckyowlstudios.locksmith.block.chest.gold_trapped.GoldenTrappedChestBlock;
import net.luckyowlstudios.locksmith.block.chest.iron.IronChestBlock;
import net.luckyowlstudios.locksmith.block.chest.iron_trapped.IronTrappedChestBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.Blocks;

public class ModBlocks {
   public static final Blocks BLOCKS = DeferredRegister.createBlocks("locksmith");
   public static final DeferredBlock<Block> IRON_CHEST = registerBlock(
      "iron_chest", () -> new IronChestBlock(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.IRON_BLOCK).sound(SoundType.WOOD))
   );
   public static final DeferredBlock<Block> GOLDEN_CHEST = registerBlock(
      "golden_chest", () -> new GoldenChestBlock(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.GOLD_BLOCK).sound(SoundType.WOOD))
   );
   public static final DeferredBlock<Block> IRON_TRAPPED_CHEST = registerBlock(
      "iron_trapped_chest", () -> new IronTrappedChestBlock(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.IRON_BLOCK).sound(SoundType.WOOD))
   );
   public static final DeferredBlock<Block> GOLDEN_TRAPPED_CHEST = registerBlock(
      "golden_trapped_chest", () -> new GoldenTrappedChestBlock(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.GOLD_BLOCK).sound(SoundType.WOOD))
   );

   private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
      DeferredBlock<T> toReturn = BLOCKS.register(name, block);
      registerBlockItem(name, toReturn);
      return toReturn;
   }

   private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
      ModItems.ITEMS.register(name, () -> new BlockItem((Block)block.get(), new net.minecraft.world.item.Item.Properties()));
   }

   public static void register(IEventBus eventBus) {
      BLOCKS.register(eventBus);
   }
}

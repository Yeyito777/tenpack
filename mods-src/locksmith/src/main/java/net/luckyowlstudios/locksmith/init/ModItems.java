package net.luckyowlstudios.locksmith.init;

import net.luckyowlstudios.locksmith.item.GoldenKey;
import net.luckyowlstudios.locksmith.item.KeyItem;
import net.luckyowlstudios.locksmith.item.LockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.Items;

public class ModItems {
   public static final Items ITEMS = DeferredRegister.createItems("locksmith");
   public static final DeferredItem<Item> KEY = ITEMS.register("key", () -> new KeyItem(new Properties()));
   public static final DeferredItem<Item> GOLDEN_KEY = ITEMS.register("golden_key", () -> new GoldenKey(new Properties()));
   public static final DeferredItem<Item> GOLDEN_LOCK = ITEMS.register("golden_lock", () -> new LockItem(new Properties()));
   public static final DeferredItem<Item> TRIAL_LOCK = ITEMS.register("trial_lock", () -> new LockItem(new Properties()));

   public static void register(IEventBus eventBus) {
      ITEMS.register(eventBus);
   }
}

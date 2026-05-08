package dev.yeyito.tenpackdeath;

import com.mojang.logging.LogUtils;
import de.maxhenkel.corpse.corelib.death.Death;
import de.maxhenkel.corpse.entities.CorpseEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mod(TenpackDeath.MODID)
public class TenpackDeath {
    public static final String MODID = "tenpackdeath";
    private static final Logger LOGGER = LogUtils.getLogger();

    // Tenpack design numbers. Corpse's skeleton/public-loot timer remains in corpse-server.toml.
    private static final int DECAY_START_TICKS = 5 * 60 * 20;
    private static final int DECAY_INTERVAL_TICKS = 30 * 20;
    private static final String DATA_KEY = "tenpackdeath";
    private static final String REMOVED_STACKS_KEY = "removed_stacks";
    private static final String WARNED_KEY = "decay_warned";
    private static final String ERROR_KEY = "decay_error";

    private static final Field CORPSE_AGE_FIELD = findCorpseAgeField();

    public TenpackDeath() {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof CorpseEntity corpse)) {
            return;
        }
        if (!(corpse.level() instanceof ServerLevel level)) {
            return;
        }
        // Only process once per second, and let Corpse's own tick run first.
        if (corpse.tickCount % 20 != 0) {
            return;
        }
        try {
            processCorpse(level, corpse);
        } catch (RuntimeException | LinkageError e) {
            disableDecayAfterError(level, corpse, e);
        }
    }

    private static void processCorpse(ServerLevel level, CorpseEntity corpse) {
        if (corpse.isEmpty()) {
            return;
        }

        int age = getCorpseAge(corpse);
        if (age < DECAY_START_TICKS) {
            return;
        }

        CompoundTag state = getTenpackState(corpse);
        if (state.getBoolean(ERROR_KEY)) {
            return;
        }
        if (!state.getBoolean(WARNED_KEY)) {
            state.putBoolean(WARNED_KEY, true);
            notifyOwner(level.getServer(), corpse,
                    "Your corpse has started decomposing. It will lose one item stack every 30 seconds until looted.");
        }

        int dueRemovals = 1 + (age - DECAY_START_TICKS) / DECAY_INTERVAL_TICKS;
        int alreadyRemoved = state.getInt(REMOVED_STACKS_KEY);

        while (alreadyRemoved < dueRemovals && !corpse.isEmpty()) {
            ItemStack removed = removeOneStack(corpse);
            if (removed.isEmpty()) {
                break;
            }
            alreadyRemoved++;
            state.putInt(REMOVED_STACKS_KEY, alreadyRemoved);
            notifyOwner(level.getServer(), corpse,
                    "Your decomposing corpse lost " + describe(removed) + ".");
        }
    }

    private static CompoundTag getTenpackState(CorpseEntity corpse) {
        CompoundTag persistent = corpse.getPersistentData();
        if (!persistent.contains(DATA_KEY)) {
            persistent.put(DATA_KEY, new CompoundTag());
        }
        return persistent.getCompound(DATA_KEY);
    }

    private static ItemStack removeOneStack(CorpseEntity corpse) {
        Death death = corpse.getDeath();

        List<SlotRef> occupied = new ArrayList<>();
        collectOccupied(occupied, death.getMainInventory());
        collectOccupied(occupied, death.getArmorInventory());
        collectOccupied(occupied, death.getOffHandInventory());
        collectOccupied(occupied, death.getAdditionalItems());

        if (occupied.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // Randomize decay target so players cannot trivially predict which slot is safest.
        int index = corpse.getRandom().nextInt(occupied.size());
        SlotRef slot = occupied.get(index);
        ItemStack removed = slot.list.get(slot.index).copy();
        slot.list.set(slot.index, ItemStack.EMPTY);

        // Death is a mutable object. Mutating its inventory lists is enough for
        // Corpse's saving and GUI code, which read from corpse.getDeath(). Do
        // not call CorpseEntity#setDeath here: Corpse marks that method as
        // client-only in 1.21.1, so NeoForge can strip it on dedicated servers,
        // causing NoSuchMethodError when this server-side tick code runs.
        return removed;
    }

    private static void disableDecayAfterError(ServerLevel level, CorpseEntity corpse, Throwable error) {
        try {
            CompoundTag state = getTenpackState(corpse);
            if (state.getBoolean(ERROR_KEY)) {
                return;
            }
            state.putBoolean(ERROR_KEY, true);
            notifyOwner(level.getServer(), corpse,
                    "Corpse decomposition hit an internal error and has been paused for this corpse. Please loot it manually.");
            LOGGER.error("Paused Tenpack corpse decomposition for corpse {} at {}", corpse.getUUID(), corpse.blockPosition(), error);
        } catch (RuntimeException loggingError) {
            LOGGER.error("Failed while handling Tenpack corpse decomposition error for corpse {}", corpse.getUUID(), error);
            LOGGER.error("Secondary error while marking corpse decomposition failed", loggingError);
        }
    }

    private static void collectOccupied(List<SlotRef> occupied, NonNullList<ItemStack> list) {
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = list.get(i);
            if (!stack.isEmpty()) {
                occupied.add(new SlotRef(list, i));
            }
        }
    }

    private static void notifyOwner(MinecraftServer server, CorpseEntity corpse, String message) {
        Optional<UUID> ownerId = corpse.getCorpseUUID();
        if (ownerId.isEmpty()) {
            return;
        }
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId.get());
        if (owner == null) {
            return;
        }
        owner.sendSystemMessage(Component.literal("[Tenpack Death] " + message));
    }

    private static String describe(ItemStack stack) {
        String name = stack.getHoverName().getString();
        if (stack.getCount() <= 1) {
            return name;
        }
        return stack.getCount() + "x " + name;
    }

    private static int getCorpseAge(CorpseEntity corpse) {
        if (CORPSE_AGE_FIELD != null) {
            try {
                return CORPSE_AGE_FIELD.getInt(corpse);
            } catch (IllegalAccessException ignored) {
            }
        }
        // Fallback is not persisted, but prevents total failure if Corpse changes internals.
        return corpse.tickCount;
    }

    private static Field findCorpseAgeField() {
        try {
            Field field = CorpseEntity.class.getDeclaredField("age");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException ignored) {
            return null;
        }
    }

    private record SlotRef(NonNullList<ItemStack> list, int index) {
    }
}

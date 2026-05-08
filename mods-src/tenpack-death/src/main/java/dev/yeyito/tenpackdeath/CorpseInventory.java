package dev.yeyito.tenpackdeath;

import de.maxhenkel.corpse.corelib.death.Death;
import de.maxhenkel.corpse.entities.CorpseEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

final class CorpseInventory {
    private CorpseInventory() {
    }

    static void dropAll(ServerLevel level, CorpseEntity corpse, TenpackDeathConfig config) {
        Death death = corpse.getDeath();
        dropInventory(level, corpse, death.getMainInventory());
        dropInventory(level, corpse, death.getArmorInventory());
        dropInventory(level, corpse, death.getOffHandInventory());
        dropInventory(level, corpse, death.getAdditionalItems());
        if (config.breakCorpseDropsExperience) {
            int points = CorpseExperience.storedExperiencePoints(corpse, death, config);
            if (points > 0) {
                ExperienceOrb.award(level, corpse.position(), points);
            }
        }
        // Death is a mutable object. Mutating its inventory lists is enough for
        // Corpse's saving and GUI code, which read from corpse.getDeath(). Do
        // not call CorpseEntity#setDeath here: Corpse marks that method as
        // client-only in 1.21.1, so NeoForge can strip it on dedicated servers,
        // causing NoSuchMethodError when server-side code runs.
        corpse.discard();
    }

    static ItemStack removeOneStack(CorpseEntity corpse, boolean randomDecay) {
        Death death = corpse.getDeath();

        List<SlotRef> occupied = new ArrayList<>();
        collectOccupied(occupied, death.getMainInventory());
        collectOccupied(occupied, death.getArmorInventory());
        collectOccupied(occupied, death.getOffHandInventory());
        collectOccupied(occupied, death.getAdditionalItems());

        if (occupied.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int index = randomDecay ? corpse.getRandom().nextInt(occupied.size()) : occupied.size() - 1;
        SlotRef slot = occupied.get(index);
        ItemStack removed = slot.list.get(slot.index).copy();
        slot.list.set(slot.index, ItemStack.EMPTY);
        return removed;
    }

    private static void dropInventory(ServerLevel level, CorpseEntity corpse, NonNullList<ItemStack> list) {
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = list.get(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, corpse.getX(), corpse.getY(), corpse.getZ(), stack.copy());
                list.set(i, ItemStack.EMPTY);
            }
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

    private record SlotRef(NonNullList<ItemStack> list, int index) {
    }
}

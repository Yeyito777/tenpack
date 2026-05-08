package dev.yeyito.tenpackdeath;

import de.maxhenkel.corpse.entities.CorpseEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

final class CorpseDecay {
    private CorpseDecay() {
    }

    static void process(ServerLevel level, CorpseEntity corpse, TenpackDeathConfig config) {
        CompoundTag state = CorpseState.of(corpse);
        if (state.getBoolean(CorpseState.ERROR_KEY)) {
            return;
        }
        CorpseExperience.attachStoredExperience(corpse);
        if (!config.decayEnabled || corpse.isEmpty()) {
            return;
        }

        int age = CorpseState.ageTicks(corpse);
        int decayStartTicks = config.decayStartSeconds * 20;
        int decayIntervalTicks = Math.max(1, config.decayIntervalSeconds * 20);
        if (age < decayStartTicks) {
            return;
        }

        state.putBoolean(CorpseState.DECAY_STARTED_KEY, true);
        if (config.notifyDecayStarted && !state.getBoolean(CorpseState.WARNED_KEY)) {
            state.putBoolean(CorpseState.WARNED_KEY, true);
            PlayerNotifier.notifyOwner(level.getServer(), corpse,
                    "Your corpse has started decomposing. It will lose one item stack every " + config.decayIntervalSeconds + " seconds until looted.");
        }

        int dueRemovals = 1 + (age - decayStartTicks) / decayIntervalTicks;
        int alreadyRemoved = state.getInt(CorpseState.REMOVED_STACKS_KEY);

        while (alreadyRemoved < dueRemovals && !corpse.isEmpty()) {
            ItemStack removed = CorpseInventory.removeOneStack(corpse, config.randomDecay);
            if (removed.isEmpty()) {
                break;
            }
            alreadyRemoved++;
            state.putInt(CorpseState.REMOVED_STACKS_KEY, alreadyRemoved);
        }
    }
}

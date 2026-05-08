package dev.yeyito.tenpackdeath;

import de.maxhenkel.corpse.corelib.death.Death;
import de.maxhenkel.corpse.entities.CorpseEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class CorpseExperience {
    private static final Map<UUID, ArrayDeque<Integer>> RECENT_DEATH_XP_POINTS = new HashMap<>();

    private CorpseExperience() {
    }

    static void rememberDeathExperience(ServerPlayer player) {
        RECENT_DEATH_XP_POINTS
                .computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>())
                .addLast(Math.max(0, currentSpendableXpPoints(player)));
    }

    static void attachStoredExperience(CorpseEntity corpse) {
        CompoundTag state = CorpseState.of(corpse);
        if (state.contains(CorpseState.XP_POINTS_KEY)) {
            return;
        }
        Optional<UUID> ownerId = corpse.getCorpseUUID();
        if (ownerId.isEmpty()) {
            return;
        }
        ArrayDeque<Integer> queuedXp = RECENT_DEATH_XP_POINTS.get(ownerId.get());
        if (queuedXp == null || queuedXp.isEmpty()) {
            return;
        }
        state.putInt(CorpseState.XP_POINTS_KEY, queuedXp.removeFirst());
        if (queuedXp.isEmpty()) {
            RECENT_DEATH_XP_POINTS.remove(ownerId.get());
        }
    }

    static int storedExperiencePoints(CorpseEntity corpse, Death death, TenpackDeathConfig config) {
        CompoundTag state = CorpseState.of(corpse);
        attachStoredExperience(corpse);
        if (state.contains(CorpseState.XP_POINTS_KEY)) {
            return Math.max(0, state.getInt(CorpseState.XP_POINTS_KEY));
        }
        // Fallback for old corpses created before TenpackDeath started recording total XP.
        return config.dropExperienceAsLevels ? xpPointsForLevels(death.getExperience()) : Math.max(0, death.getExperience());
    }

    private static int currentSpendableXpPoints(Player player) {
        // Do not use Player#totalExperience here. In vanilla it is not a reliable
        // "current spendable XP" balance after enchanting/level manipulation. The
        // level + progress fields are the source used by the XP bar/enchanting UI.
        return xpPointsForLevels(player.experienceLevel)
                + Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
    }

    private static int xpPointsForLevels(int levels) {
        int total = 0;
        for (int level = 0; level < levels; level++) {
            if (level >= 30) {
                total += 112 + (level - 30) * 9;
            } else if (level >= 15) {
                total += 37 + (level - 15) * 5;
            } else {
                total += 7 + level * 2;
            }
        }
        return total;
    }
}

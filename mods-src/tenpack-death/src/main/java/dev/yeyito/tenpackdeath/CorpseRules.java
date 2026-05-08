package dev.yeyito.tenpackdeath;

import de.maxhenkel.corpse.entities.CorpseEntity;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

final class CorpseRules {
    private CorpseRules() {
    }

    static boolean canAccessCorpse(ServerPlayer player, CorpseEntity corpse, TenpackDeathConfig config) {
        return !config.ownerProtectionEnabled
                || isOwner(player, corpse)
                || (config.opsBypassProtection && isOp(player))
                || isPubliclyLootable(corpse, config);
    }

    static boolean isPubliclyLootable(CorpseEntity corpse, TenpackDeathConfig config) {
        if (config.publicLootRequiresSkeleton) {
            return corpse.isSkeleton();
        }
        int protectedTicks = config.publicLootAfterSeconds * 20;
        return CorpseState.ageTicks(corpse) >= protectedTicks;
    }

    static boolean canBreakCorpse(ServerPlayer player, CorpseEntity corpse, TenpackDeathConfig config) {
        if (isOwner(player, corpse)) {
            return true;
        }
        if (!config.requireDecayStartedToBreakCorpse) {
            return true;
        }
        if (config.breakCorpseAtSkeleton && corpse.isSkeleton()) {
            return true;
        }
        return hasDecayStarted(corpse, config);
    }

    static boolean hasDecayStarted(CorpseEntity corpse, TenpackDeathConfig config) {
        if (!config.requireDecayStartedToBreakCorpse) {
            return true;
        }
        if (CorpseState.of(corpse).getBoolean(CorpseState.DECAY_STARTED_KEY)) {
            return true;
        }
        return CorpseState.ageTicks(corpse) >= config.decayStartSeconds * 20;
    }

    private static boolean isOwner(ServerPlayer player, CorpseEntity corpse) {
        Optional<UUID> ownerId = corpse.getCorpseUUID();
        return ownerId.isPresent() && player.getUUID().equals(ownerId.get());
    }

    private static boolean isOp(ServerPlayer player) {
        return player.hasPermissions(player.server.getOperatorUserPermissionLevel());
    }
}

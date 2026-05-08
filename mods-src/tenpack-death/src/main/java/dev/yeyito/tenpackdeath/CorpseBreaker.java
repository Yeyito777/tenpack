package dev.yeyito.tenpackdeath;

import de.maxhenkel.corpse.entities.CorpseEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

final class CorpseBreaker {
    private CorpseBreaker() {
    }

    static boolean tryBreakWithShiftClick(ServerPlayer player, CorpseEntity corpse, TenpackDeathConfig config, Logger logger) {
        if (!config.breakCorpseDropsAfterDecay || !player.isShiftKeyDown()) {
            return false;
        }
        if (!(corpse.level() instanceof ServerLevel level)) {
            return false;
        }
        if (!CorpseRules.canBreakCorpse(player, corpse, config)) {
            return false;
        }
        breakCorpse(level, corpse, config, logger);
        return true;
    }

    static boolean handleAttack(ServerPlayer player, CorpseEntity corpse, ServerLevel level, TenpackDeathConfig config, Logger logger) {
        if (!config.breakCorpseDropsAfterDecay) {
            return false;
        }
        if (!CorpseRules.canBreakCorpse(player, corpse, config)) {
            if (config.notifyDeniedAccess) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "[Tenpack Death] This corpse is not decomposed enough to break open."));
            }
            return true;
        }
        breakCorpse(level, corpse, config, logger);
        return true;
    }

    static void breakCorpse(ServerLevel level, CorpseEntity corpse, TenpackDeathConfig config, Logger logger) {
        try {
            CorpseInventory.dropAll(level, corpse, config);
        } catch (RuntimeException | LinkageError e) {
            markError(level, corpse, config, logger, e);
        }
    }

    static void markError(ServerLevel level, CorpseEntity corpse, TenpackDeathConfig config, Logger logger, Throwable error) {
        try {
            var state = CorpseState.of(corpse);
            if (state.getBoolean(CorpseState.ERROR_KEY)) {
                return;
            }
            state.putBoolean(CorpseState.ERROR_KEY, true);
            if (config.notifyInternalErrors) {
                PlayerNotifier.notifyOwner(level.getServer(), corpse,
                        "Corpse decomposition hit an internal error and has been paused for this corpse. Please loot it manually.");
            }
            logger.error("Paused Tenpack corpse decomposition for corpse {} at {}", corpse.getUUID(), corpse.blockPosition(), error);
        } catch (RuntimeException loggingError) {
            logger.error("Failed while handling Tenpack corpse decomposition error for corpse {}", corpse.getUUID(), error);
            logger.error("Secondary error while marking corpse decomposition failed", loggingError);
        }
    }
}

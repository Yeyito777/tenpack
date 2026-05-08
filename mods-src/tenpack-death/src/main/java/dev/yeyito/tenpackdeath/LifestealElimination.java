package dev.yeyito.tenpackdeath;

import mc.mian.lifesteal.data.LSData;
import mc.mian.lifesteal.util.LSConstants;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

final class LifestealElimination {
    private LifestealElimination() {
    }

    /**
     * Lifesteal's ban=false path marks eliminated players with TIME_KILLED and
     * puts them in spectator. Re-apply spectator after respawn/login because
     * other respawn handling can put the new player instance back in survival.
     */
    static void enforceSpectatorIfEliminated(ServerPlayer player) {
        LSData.get(player).ifPresent(data -> {
            if (isEliminated(data) && !player.isSpectator()) {
                player.setGameMode(GameType.SPECTATOR);
            }
        });
    }

    private static boolean isEliminated(LSData data) {
        Long timeKilled = data.getValue(LSConstants.TIME_KILLED);
        return timeKilled != null && timeKilled > 0L;
    }
}

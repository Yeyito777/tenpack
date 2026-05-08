package dev.yeyito.tenpackdeath;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

final class DeathSounds {
    private DeathSounds() {
    }

    static void playReturnByDeathCue(ServerPlayer player, float volume) {
        player.playNotifySound(TenpackDeath.RETURN_BY_DEATH_SOUND.get(), SoundSource.PLAYERS, volume, 1.0F);
    }
}

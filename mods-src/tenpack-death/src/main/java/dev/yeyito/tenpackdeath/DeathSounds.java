package dev.yeyito.tenpackdeath;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

final class DeathSounds {
    private DeathSounds() {
    }

    /**
     * Return-by-Death-inspired cue built from vanilla sounds.
     *
     * <p>The exact YouTube/Re:Zero clip should not be ripped into the pack unless
     * we have a licensed/authorized .ogg. Once such a file exists, this is the
     * right place to swap the vanilla cue for a custom SoundEvent.</p>
     */
    static void playReturnByDeathCue(ServerPlayer player, float volume) {
        player.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, volume, 0.55F);
        player.playNotifySound(SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, volume * 0.65F, 0.55F);
        player.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, volume * 0.35F, 0.45F);
    }
}

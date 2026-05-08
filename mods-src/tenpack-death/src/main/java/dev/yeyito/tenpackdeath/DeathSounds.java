package dev.yeyito.tenpackdeath;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

final class DeathSounds {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, TenpackDeath.MODID);
    private static final DeferredHolder<SoundEvent, SoundEvent> RETURN_BY_DEATH = SOUND_EVENTS.register(
            "return_by_death", SoundEvent::createVariableRangeEvent);

    private DeathSounds() {
    }

    static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }

    static void playReturnByDeathCue(ServerPlayer player, float volume) {
        player.playNotifySound(RETURN_BY_DEATH.get(), SoundSource.PLAYERS, volume, 1.0F);
    }
}

package dev.yeyito.tenpackdeath;

import com.mojang.logging.LogUtils;
import de.maxhenkel.corpse.entities.CorpseEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.slf4j.Logger;

@Mod(TenpackDeath.MODID)
public class TenpackDeath {
    public static final String MODID = "tenpackdeath";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final TenpackDeathConfig CONFIG = new TenpackDeathConfig();

    public TenpackDeath(IEventBus modEventBus) {
        DeathSounds.register(modEventBus);
        CONFIG.loadIfNeeded();
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CONFIG.loadIfNeeded();
        CorpseExperience.rememberDeathExperience(player);
        if (CONFIG.deathSoundEnabled) {
            DeathSounds.playReturnByDeathCue(player, CONFIG.deathSoundVolume);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onExperienceDrop(LivingExperienceDropEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            // Vanilla only drops a capped fraction of player XP on death. Tenpack
            // stores the full value and releases it through the corpse instead.
            event.setDroppedExperience(0);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LifestealElimination.enforceSpectatorIfEliminated(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LifestealElimination.enforceSpectatorIfEliminated(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof CorpseEntity corpse)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(corpse.level() instanceof ServerLevel level)) {
            return;
        }

        CONFIG.loadIfNeeded();
        if (CorpseBreaker.handleAttack(player, corpse, level, CONFIG, LOGGER)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        protectCorpse(event.getEntity(), event.getTarget(), event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        protectCorpse(event.getEntity(), event.getTarget(), event);
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
        CONFIG.loadIfNeeded();
        try {
            CorpseDecay.process(level, corpse, CONFIG);
        } catch (RuntimeException | LinkageError e) {
            CorpseBreaker.markError(level, corpse, CONFIG, LOGGER, e);
        }
    }

    private static void protectCorpse(Player player, Entity target, PlayerInteractEvent.EntityInteract event) {
        CorpseInteractionResult result = handleCorpseInteraction(player, target);
        if (result == CorpseInteractionResult.ALLOW) {
            return;
        }
        event.setCancellationResult(result.interactionResult);
        event.setCanceled(true);
    }

    private static void protectCorpse(Player player, Entity target, PlayerInteractEvent.EntityInteractSpecific event) {
        CorpseInteractionResult result = handleCorpseInteraction(player, target);
        if (result == CorpseInteractionResult.ALLOW) {
            return;
        }
        event.setCancellationResult(result.interactionResult);
        event.setCanceled(true);
    }

    private static CorpseInteractionResult handleCorpseInteraction(Player player, Entity target) {
        if (!(target instanceof CorpseEntity corpse)) {
            return CorpseInteractionResult.ALLOW;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return CorpseInteractionResult.ALLOW;
        }
        if (!(corpse.level() instanceof ServerLevel)) {
            return CorpseInteractionResult.ALLOW;
        }

        CONFIG.loadIfNeeded();
        if (CorpseBreaker.tryBreakWithShiftClick(serverPlayer, corpse, CONFIG, LOGGER)) {
            return CorpseInteractionResult.CONSUME;
        }
        if (CorpseRules.canAccessCorpse(serverPlayer, corpse, CONFIG)) {
            return CorpseInteractionResult.ALLOW;
        }

        if (CONFIG.notifyDeniedAccess) {
            serverPlayer.sendSystemMessage(Component.literal("[Tenpack Death] This corpse is protected until it becomes a skeleton."));
        }
        return CorpseInteractionResult.DENY;
    }

    private enum CorpseInteractionResult {
        ALLOW(null),
        DENY(InteractionResult.FAIL),
        CONSUME(InteractionResult.SUCCESS);

        private final InteractionResult interactionResult;

        CorpseInteractionResult(InteractionResult interactionResult) {
            this.interactionResult = interactionResult;
        }
    }

}

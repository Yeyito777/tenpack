package dev.yeyito.tenpackdeath;

import com.mojang.logging.LogUtils;
import de.maxhenkel.corpse.corelib.death.Death;
import de.maxhenkel.corpse.entities.CorpseEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Mod(TenpackDeath.MODID)
public class TenpackDeath {
    public static final String MODID = "tenpackdeath";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<UUID, ArrayDeque<Integer>> RECENT_DEATH_XP_POINTS = new HashMap<>();
    private static final TenpackDeathConfig CONFIG = new TenpackDeathConfig();

    public TenpackDeath() {
        CONFIG.loadIfNeeded();
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CONFIG.loadIfNeeded();
        rememberDeathExperience(player);
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
        if (!CONFIG.breakCorpseDropsAfterDecay) {
            return;
        }
        if (!canBreakCorpse(corpse)) {
            if (CONFIG.notifyDeniedAccess) {
                player.sendSystemMessage(Component.literal("[Tenpack Death] This corpse is not decomposed enough to break open."));
            }
            event.setCanceled(true);
            return;
        }

        try {
            dropCorpseContents(level, corpse);
        } catch (RuntimeException | LinkageError e) {
            disableDecayAfterError(level, corpse, e);
        }
        event.setCanceled(true);
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
            processCorpse(level, corpse);
        } catch (RuntimeException | LinkageError e) {
            disableDecayAfterError(level, corpse, e);
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
        if (tryBreakDecayedCorpse(serverPlayer, corpse)) {
            return CorpseInteractionResult.CONSUME;
        }
        if (canAccessCorpse(serverPlayer, corpse)) {
            return CorpseInteractionResult.ALLOW;
        }

        if (CONFIG.notifyDeniedAccess) {
            serverPlayer.sendSystemMessage(Component.literal("[Tenpack Death] This corpse is protected until it becomes a skeleton."));
        }
        return CorpseInteractionResult.DENY;
    }

    private static boolean canAccessCorpse(ServerPlayer player, CorpseEntity corpse) {
        return !CONFIG.ownerProtectionEnabled
                || isOwner(player, corpse)
                || (CONFIG.opsBypassProtection && isOp(player))
                || isPubliclyLootable(corpse);
    }

    private static boolean isPubliclyLootable(CorpseEntity corpse) {
        if (CONFIG.publicLootRequiresSkeleton) {
            return corpse.isSkeleton();
        }
        int protectedTicks = CONFIG.publicLootAfterSeconds * 20;
        return CorpseState.ageTicks(corpse) >= protectedTicks;
    }

    private static void processCorpse(ServerLevel level, CorpseEntity corpse) {
        CompoundTag state = CorpseState.of(corpse);
        if (state.getBoolean(CorpseState.ERROR_KEY)) {
            return;
        }
        attachStoredExperience(corpse, state);
        if (!CONFIG.decayEnabled) {
            return;
        }
        if (corpse.isEmpty()) {
            return;
        }

        int age = CorpseState.ageTicks(corpse);
        int decayStartTicks = CONFIG.decayStartSeconds * 20;
        int decayIntervalTicks = Math.max(1, CONFIG.decayIntervalSeconds * 20);
        if (age < decayStartTicks) {
            return;
        }

        state.putBoolean(CorpseState.DECAY_STARTED_KEY, true);
        if (CONFIG.notifyDecayStarted && !state.getBoolean(CorpseState.WARNED_KEY)) {
            state.putBoolean(CorpseState.WARNED_KEY, true);
            notifyOwner(level.getServer(), corpse,
                    "Your corpse has started decomposing. It will lose one item stack every " + CONFIG.decayIntervalSeconds + " seconds until looted.");
        }

        int dueRemovals = 1 + (age - decayStartTicks) / decayIntervalTicks;
        int alreadyRemoved = state.getInt(CorpseState.REMOVED_STACKS_KEY);

        while (alreadyRemoved < dueRemovals && !corpse.isEmpty()) {
            ItemStack removed = removeOneStack(corpse);
            if (removed.isEmpty()) {
                break;
            }
            alreadyRemoved++;
            state.putInt(CorpseState.REMOVED_STACKS_KEY, alreadyRemoved);
        }
    }

    private static void rememberDeathExperience(ServerPlayer player) {
        RECENT_DEATH_XP_POINTS
                .computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>())
                .addLast(Math.max(0, player.totalExperience));
    }

    private static void attachStoredExperience(CorpseEntity corpse, CompoundTag state) {
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

    private static int getStoredExperiencePoints(CorpseEntity corpse, Death death) {
        CompoundTag state = CorpseState.of(corpse);
        attachStoredExperience(corpse, state);
        if (state.contains(CorpseState.XP_POINTS_KEY)) {
            return Math.max(0, state.getInt(CorpseState.XP_POINTS_KEY));
        }
        // Fallback for old corpses created before TenpackDeath started recording total XP.
        return CONFIG.dropExperienceAsLevels ? xpPointsForLevels(death.getExperience()) : Math.max(0, death.getExperience());
    }

    private static boolean tryBreakDecayedCorpse(ServerPlayer player, CorpseEntity corpse) {
        if (!CONFIG.breakCorpseDropsAfterDecay || !player.isShiftKeyDown()) {
            return false;
        }
        if (!(corpse.level() instanceof ServerLevel level)) {
            return false;
        }
        if (!canBreakCorpse(corpse)) {
            return false;
        }
        try {
            dropCorpseContents(level, corpse);
        } catch (RuntimeException | LinkageError e) {
            disableDecayAfterError(level, corpse, e);
        }
        return true;
    }

    private static boolean canBreakCorpse(CorpseEntity corpse) {
        if (!CONFIG.requireDecayStartedToBreakCorpse) {
            return true;
        }
        if (CONFIG.breakCorpseAtSkeleton && corpse.isSkeleton()) {
            return true;
        }
        return hasDecayStarted(corpse);
    }

    private static boolean hasDecayStarted(CorpseEntity corpse) {
        if (!CONFIG.requireDecayStartedToBreakCorpse) {
            return true;
        }
        if (CorpseState.of(corpse).getBoolean(CorpseState.DECAY_STARTED_KEY)) {
            return true;
        }
        return CorpseState.ageTicks(corpse) >= CONFIG.decayStartSeconds * 20;
    }

    private static void dropCorpseContents(ServerLevel level, CorpseEntity corpse) {
        Death death = corpse.getDeath();
        dropInventory(level, corpse, death.getMainInventory());
        dropInventory(level, corpse, death.getArmorInventory());
        dropInventory(level, corpse, death.getOffHandInventory());
        dropInventory(level, corpse, death.getAdditionalItems());
        if (CONFIG.breakCorpseDropsExperience) {
            int points = getStoredExperiencePoints(corpse, death);
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

    private static void dropInventory(ServerLevel level, CorpseEntity corpse, NonNullList<ItemStack> list) {
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = list.get(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, corpse.getX(), corpse.getY(), corpse.getZ(), stack.copy());
                list.set(i, ItemStack.EMPTY);
            }
        }
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


    private static ItemStack removeOneStack(CorpseEntity corpse) {
        Death death = corpse.getDeath();

        List<SlotRef> occupied = new ArrayList<>();
        collectOccupied(occupied, death.getMainInventory());
        collectOccupied(occupied, death.getArmorInventory());
        collectOccupied(occupied, death.getOffHandInventory());
        collectOccupied(occupied, death.getAdditionalItems());

        if (occupied.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int index = CONFIG.randomDecay ? corpse.getRandom().nextInt(occupied.size()) : occupied.size() - 1;
        SlotRef slot = occupied.get(index);
        ItemStack removed = slot.list.get(slot.index).copy();
        slot.list.set(slot.index, ItemStack.EMPTY);

        // Death is a mutable object. Mutating its inventory lists is enough for
        // Corpse's saving and GUI code, which read from corpse.getDeath(). Do
        // not call CorpseEntity#setDeath here: Corpse marks that method as
        // client-only in 1.21.1, so NeoForge can strip it on dedicated servers,
        // causing NoSuchMethodError when this server-side tick code runs.
        return removed;
    }

    private static void disableDecayAfterError(ServerLevel level, CorpseEntity corpse, Throwable error) {
        try {
            CompoundTag state = CorpseState.of(corpse);
            if (state.getBoolean(CorpseState.ERROR_KEY)) {
                return;
            }
            state.putBoolean(CorpseState.ERROR_KEY, true);
            if (CONFIG.notifyInternalErrors) {
                notifyOwner(level.getServer(), corpse,
                        "Corpse decomposition hit an internal error and has been paused for this corpse. Please loot it manually.");
            }
            LOGGER.error("Paused Tenpack corpse decomposition for corpse {} at {}", corpse.getUUID(), corpse.blockPosition(), error);
        } catch (RuntimeException loggingError) {
            LOGGER.error("Failed while handling Tenpack corpse decomposition error for corpse {}", corpse.getUUID(), error);
            LOGGER.error("Secondary error while marking corpse decomposition failed", loggingError);
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

    private static boolean isOwner(ServerPlayer player, CorpseEntity corpse) {
        Optional<UUID> ownerId = corpse.getCorpseUUID();
        return ownerId.isPresent() && player.getUUID().equals(ownerId.get());
    }

    private static boolean isOp(ServerPlayer player) {
        return player.hasPermissions(player.server.getOperatorUserPermissionLevel());
    }

    private static void notifyOwner(MinecraftServer server, CorpseEntity corpse, String message) {
        Optional<UUID> ownerId = corpse.getCorpseUUID();
        if (ownerId.isEmpty()) {
            return;
        }
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId.get());
        if (owner == null) {
            return;
        }
        owner.sendSystemMessage(Component.literal("[Tenpack Death] " + message));
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

    private record SlotRef(NonNullList<ItemStack> list, int index) {
    }

}

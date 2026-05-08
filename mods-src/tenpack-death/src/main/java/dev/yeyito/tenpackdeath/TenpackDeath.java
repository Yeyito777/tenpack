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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

@Mod(TenpackDeath.MODID)
public class TenpackDeath {
    public static final String MODID = "tenpackdeath";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String DATA_KEY = "tenpackdeath";
    private static final String REMOVED_STACKS_KEY = "removed_stacks";
    private static final String WARNED_KEY = "decay_warned";
    private static final String DECAY_STARTED_KEY = "decay_started";
    private static final String ERROR_KEY = "decay_error";
    private static final String XP_POINTS_KEY = "stored_xp_points";

    private static final Field CORPSE_AGE_FIELD = findCorpseAgeField();
    private static final Map<UUID, ArrayDeque<Integer>> RECENT_DEATH_XP_POINTS = new HashMap<>();
    private static final Config CONFIG = new Config();

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
            playReturnByDeathSound(player);
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
        if (!(target instanceof CorpseEntity corpse)) {
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!(corpse.level() instanceof ServerLevel)) {
            return;
        }

        CONFIG.loadIfNeeded();
        if (tryBreakDecayedCorpse(serverPlayer, corpse)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }
        if (!CONFIG.ownerProtectionEnabled) {
            return;
        }
        if (isOwner(serverPlayer, corpse)) {
            return;
        }
        if (CONFIG.opsBypassProtection && isOp(serverPlayer)) {
            return;
        }
        if (isPubliclyLootable(corpse)) {
            return;
        }

        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);

        if (CONFIG.notifyDeniedAccess) {
            serverPlayer.sendSystemMessage(Component.literal("[Tenpack Death] This corpse is protected until it becomes a skeleton."));
        }
    }

    private static void protectCorpse(Player player, Entity target, PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(target instanceof CorpseEntity corpse)) {
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!(corpse.level() instanceof ServerLevel)) {
            return;
        }

        CONFIG.loadIfNeeded();
        if (tryBreakDecayedCorpse(serverPlayer, corpse)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }
        if (!CONFIG.ownerProtectionEnabled) {
            return;
        }
        if (isOwner(serverPlayer, corpse)) {
            return;
        }
        if (CONFIG.opsBypassProtection && isOp(serverPlayer)) {
            return;
        }
        if (isPubliclyLootable(corpse)) {
            return;
        }

        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);

        if (CONFIG.notifyDeniedAccess) {
            serverPlayer.sendSystemMessage(Component.literal("[Tenpack Death] This corpse is protected until it becomes a skeleton."));
        }
    }

    private static boolean isPubliclyLootable(CorpseEntity corpse) {
        if (CONFIG.publicLootRequiresSkeleton) {
            return corpse.isSkeleton();
        }
        int protectedTicks = CONFIG.publicLootAfterSeconds * 20;
        return getCorpseAge(corpse) >= protectedTicks;
    }

    private static void playReturnByDeathSound(ServerPlayer player) {
        float volume = CONFIG.deathSoundVolume;
        player.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, volume, 0.55F);
        player.playNotifySound(SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, volume * 0.65F, 0.55F);
        player.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, volume * 0.35F, 0.45F);
    }

    private static void processCorpse(ServerLevel level, CorpseEntity corpse) {
        CompoundTag state = getTenpackState(corpse);
        if (state.getBoolean(ERROR_KEY)) {
            return;
        }
        attachStoredExperience(corpse, state);
        if (!CONFIG.decayEnabled) {
            return;
        }
        if (corpse.isEmpty()) {
            return;
        }

        int age = getCorpseAge(corpse);
        int decayStartTicks = CONFIG.decayStartSeconds * 20;
        int decayIntervalTicks = Math.max(1, CONFIG.decayIntervalSeconds * 20);
        if (age < decayStartTicks) {
            return;
        }

        state.putBoolean(DECAY_STARTED_KEY, true);
        if (CONFIG.notifyDecayStarted && !state.getBoolean(WARNED_KEY)) {
            state.putBoolean(WARNED_KEY, true);
            notifyOwner(level.getServer(), corpse,
                    "Your corpse has started decomposing. It will lose one item stack every " + CONFIG.decayIntervalSeconds + " seconds until looted.");
        }

        int dueRemovals = 1 + (age - decayStartTicks) / decayIntervalTicks;
        int alreadyRemoved = state.getInt(REMOVED_STACKS_KEY);

        while (alreadyRemoved < dueRemovals && !corpse.isEmpty()) {
            ItemStack removed = removeOneStack(corpse);
            if (removed.isEmpty()) {
                break;
            }
            alreadyRemoved++;
            state.putInt(REMOVED_STACKS_KEY, alreadyRemoved);
        }
    }

    private static void rememberDeathExperience(ServerPlayer player) {
        RECENT_DEATH_XP_POINTS
                .computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>())
                .addLast(Math.max(0, player.totalExperience));
    }

    private static void attachStoredExperience(CorpseEntity corpse, CompoundTag state) {
        if (state.contains(XP_POINTS_KEY)) {
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
        state.putInt(XP_POINTS_KEY, queuedXp.removeFirst());
        if (queuedXp.isEmpty()) {
            RECENT_DEATH_XP_POINTS.remove(ownerId.get());
        }
    }

    private static int getStoredExperiencePoints(CorpseEntity corpse, Death death) {
        CompoundTag state = getTenpackState(corpse);
        attachStoredExperience(corpse, state);
        if (state.contains(XP_POINTS_KEY)) {
            return Math.max(0, state.getInt(XP_POINTS_KEY));
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
        if (getTenpackState(corpse).getBoolean(DECAY_STARTED_KEY)) {
            return true;
        }
        return getCorpseAge(corpse) >= CONFIG.decayStartSeconds * 20;
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

    private static CompoundTag getTenpackState(CorpseEntity corpse) {
        CompoundTag persistent = corpse.getPersistentData();
        if (!persistent.contains(DATA_KEY)) {
            persistent.put(DATA_KEY, new CompoundTag());
        }
        return persistent.getCompound(DATA_KEY);
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
            CompoundTag state = getTenpackState(corpse);
            if (state.getBoolean(ERROR_KEY)) {
                return;
            }
            state.putBoolean(ERROR_KEY, true);
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

    private static String describe(ItemStack stack) {
        String name = stack.getHoverName().getString();
        if (stack.getCount() <= 1) {
            return name;
        }
        return stack.getCount() + "x " + name;
    }

    private static int getCorpseAge(CorpseEntity corpse) {
        if (CORPSE_AGE_FIELD != null) {
            try {
                return CORPSE_AGE_FIELD.getInt(corpse);
            } catch (IllegalAccessException ignored) {
            }
        }
        // Fallback is not persisted, but prevents total failure if Corpse changes internals.
        return corpse.tickCount;
    }

    private static Field findCorpseAgeField() {
        try {
            Field field = CorpseEntity.class.getDeclaredField("age");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException ignored) {
            return null;
        }
    }

    private record SlotRef(NonNullList<ItemStack> list, int index) {
    }

    private static class Config {
        private static final Path PATH = Path.of("config", "tenpackdeath.properties");

        private long lastModified = Long.MIN_VALUE;

        boolean ownerProtectionEnabled = true;
        boolean publicLootRequiresSkeleton = true;
        int publicLootAfterSeconds = 60;
        boolean opsBypassProtection = false;
        boolean notifyDeniedAccess = false;

        boolean deathSoundEnabled = true;
        float deathSoundVolume = 1.0F;

        boolean decayEnabled = true;
        int decayStartSeconds = 300;
        int decayIntervalSeconds = 30;
        boolean randomDecay = true;
        boolean notifyDecayStarted = false;
        boolean notifyInternalErrors = false;

        boolean breakCorpseDropsAfterDecay = true;
        boolean requireDecayStartedToBreakCorpse = true;
        boolean breakCorpseAtSkeleton = true;
        boolean breakCorpseDropsExperience = true;
        boolean dropExperienceAsLevels = true;

        void loadIfNeeded() {
            try {
                if (!Files.exists(PATH)) {
                    writeDefaultConfig();
                }
                long modified = Files.getLastModifiedTime(PATH).toMillis();
                if (modified == lastModified) {
                    return;
                }
                Properties properties = new Properties();
                try (var reader = Files.newBufferedReader(PATH)) {
                    properties.load(reader);
                }

                ownerProtectionEnabled = bool(properties, "ownerProtectionEnabled", ownerProtectionEnabled);
                publicLootRequiresSkeleton = bool(properties, "publicLootRequiresSkeleton", publicLootRequiresSkeleton);
                publicLootAfterSeconds = positiveInt(properties, "publicLootAfterSeconds", publicLootAfterSeconds);
                opsBypassProtection = bool(properties, "opsBypassProtection", opsBypassProtection);
                notifyDeniedAccess = bool(properties, "notifyDeniedAccess", notifyDeniedAccess);

                deathSoundEnabled = bool(properties, "deathSoundEnabled", deathSoundEnabled);
                deathSoundVolume = positiveFloat(properties, "deathSoundVolume", deathSoundVolume);

                decayEnabled = bool(properties, "decayEnabled", decayEnabled);
                decayStartSeconds = positiveInt(properties, "decayStartSeconds", decayStartSeconds);
                decayIntervalSeconds = positiveInt(properties, "decayIntervalSeconds", decayIntervalSeconds);
                randomDecay = bool(properties, "randomDecay", randomDecay);
                notifyDecayStarted = bool(properties, "notifyDecayStarted", notifyDecayStarted);
                notifyInternalErrors = bool(properties, "notifyInternalErrors", notifyInternalErrors);

                breakCorpseDropsAfterDecay = bool(properties, "breakCorpseDropsAfterDecay", breakCorpseDropsAfterDecay);
                requireDecayStartedToBreakCorpse = bool(properties, "requireDecayStartedToBreakCorpse", requireDecayStartedToBreakCorpse);
                breakCorpseAtSkeleton = bool(properties, "breakCorpseAtSkeleton", breakCorpseAtSkeleton);
                breakCorpseDropsExperience = bool(properties, "breakCorpseDropsExperience", breakCorpseDropsExperience);
                dropExperienceAsLevels = bool(properties, "dropExperienceAsLevels", dropExperienceAsLevels);

                lastModified = modified;
            } catch (IOException ignored) {
                // Keep current/default config if the file cannot be read.
            }
        }

        private void writeDefaultConfig() throws IOException {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, """
                    # Tenpack Death config. Times are in real seconds.
                    # Owner protection means only the dead player can loot their corpse.
                    # If publicLootRequiresSkeleton=true, non-owners cannot loot until Corpse marks it as a skeleton.
                    # If false, publicLootAfterSeconds is used instead.
                    ownerProtectionEnabled=true
                    publicLootRequiresSkeleton=true
                    publicLootAfterSeconds=60
                    opsBypassProtection=false
                    notifyDeniedAccess=false

                    # Re:Zero-style Return by Death inspired sound cue. Uses vanilla ominous sounds, not a bundled copyrighted clip.
                    deathSoundEnabled=true
                    deathSoundVolume=1.0

                    # Decay starts after decayStartSeconds and removes one stored item stack every decayIntervalSeconds.
                    decayEnabled=true
                    decayStartSeconds=300
                    decayIntervalSeconds=30
                    randomDecay=true
                    notifyDecayStarted=false
                    notifyInternalErrors=false

                    # After decay has started, attacking/breaking a corpse spills everything remaining.
                    # XP is derived from Corpse's stored death experience level and dropped without vanilla XP-loss limits.
                    breakCorpseDropsAfterDecay=true
                    requireDecayStartedToBreakCorpse=true
                    breakCorpseAtSkeleton=true
                    breakCorpseDropsExperience=true
                    dropExperienceAsLevels=true
                    """);
        }

        private static boolean bool(Properties properties, String key, boolean fallback) {
            String value = properties.getProperty(key);
            if (value == null) {
                return fallback;
            }
            return Boolean.parseBoolean(value.trim());
        }

        private static int positiveInt(Properties properties, String key, int fallback) {
            String value = properties.getProperty(key);
            if (value == null) {
                return fallback;
            }
            try {
                return Math.max(1, Integer.parseInt(value.trim()));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }

        private static float positiveFloat(Properties properties, String key, float fallback) {
            String value = properties.getProperty(key);
            if (value == null) {
                return fallback;
            }
            try {
                return Math.max(0.0F, Float.parseFloat(value.trim()));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
    }
}

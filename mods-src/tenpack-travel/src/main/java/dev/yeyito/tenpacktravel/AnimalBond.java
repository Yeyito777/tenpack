package dev.yeyito.tenpacktravel;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistent per-player animal trust for Tenpack Travel.
 *
 * This intentionally models Red Dead-style familiarity rather than a magic claim item: ownership, when it
 * exists, comes from vanilla/modded ownership or a saddled primary mount marker; bond is per player and
 * grows from use/care. Later whistle/eagle systems can read this without adding teleport recall.
 */
public final class AnimalBond {
    private static final String DATA_KEY = "tenpack_travel_bond";
    private static final String PRIMARY_OWNER_KEY = "primary_owner";
    private static final String PLAYERS_KEY = "players";
    private static final String XP_KEY = "xp";
    private static final String LAST_BRUSH_KEY = "last_brush_day";
    private static final String LAST_FEED_KEY = "last_feed_day";
    private static final String LAST_RIDE_XP_TICK_KEY = "last_ride_xp_tick";
    private static final String LAST_RIDE_X_KEY = "last_ride_x";
    private static final String LAST_RIDE_Z_KEY = "last_ride_z";
    private static final String LAST_DRAFT_X_KEY = "last_draft_x";
    private static final String LAST_DRAFT_Z_KEY = "last_draft_z";
    private static final String LAST_ROLE_XP_TICK_PREFIX = "last_role_xp_tick_";

    private static final int BRUSH_XP = 4;
    private static final int FEED_XP = 3;
    private static final int RIDE_XP = 2;
    private static final int DRAFT_WORK_XP = 3;
    private static final int RIDE_XP_INTERVAL_TICKS = 20 * 20;
    private static final int DRAFT_WORK_XP_INTERVAL_TICKS = 20 * 45;
    private static final double RIDE_XP_MIN_DISTANCE_SQR = 8.0D * 8.0D;
    private static final double DRAFT_WORK_XP_MIN_DISTANCE_SQR = 16.0D * 16.0D;
    private static final int MAX_XP = 999;

    private AnimalBond() {
    }

    /** Brushing is care, not claiming. It only grows an existing relationship or a real owned mount. */
    public static boolean brush(Player player, LivingEntity animal) {
        if (!canCareFor(player, animal)) {
            return false;
        }
        CompoundTag bond = playerBond(animal, player.getUUID(), true);
        long day = day(animal);
        if (bond.getLong(LAST_BRUSH_KEY) == day) {
            return false;
        }
        bond.putLong(LAST_BRUSH_KEY, day);
        addXp(bond, BRUSH_XP);
        return true;
    }

    /** Feeding/healing is care, not claiming. It only grows an existing relationship or a real owned mount. */
    public static boolean feed(Player player, LivingEntity animal) {
        if (!canCareFor(player, animal)) {
            return false;
        }
        CompoundTag bond = playerBond(animal, player.getUUID(), true);
        long day = day(animal);
        if (bond.getLong(LAST_FEED_KEY) == day) {
            return false;
        }
        bond.putLong(LAST_FEED_KEY, day);
        addXp(bond, FEED_XP);
        return true;
    }

    /** Mounting can establish the relationship baseline, but does not award XP by itself. */
    public static boolean mount(Player player, LivingEntity animal) {
        if (snapshot(animal, player).ownedByOther()) {
            return false;
        }
        CompoundTag bond = playerBond(animal, player.getUUID(), true);
        markRidePosition(bond, animal);
        if (!bond.contains(LAST_RIDE_XP_TICK_KEY)) {
            bond.putLong(LAST_RIDE_XP_TICK_KEY, animal.level().getGameTime());
        }
        markPrimaryOwnerIfSaddled(player, animal);
        return true;
    }

    /** Sustained riding/use is the main way a relationship grows. Saddled horse-like mounts also mark primary owner. */
    public static boolean ride(Player player, LivingEntity animal) {
        if (snapshot(animal, player).ownedByOther()) {
            return false;
        }
        long gameTime = animal.level().getGameTime();
        CompoundTag bond = playerBond(animal, player.getUUID(), true);
        if (!bond.contains(LAST_RIDE_X_KEY) || !bond.contains(LAST_RIDE_Z_KEY)) {
            markRidePosition(bond, animal);
            if (!bond.contains(LAST_RIDE_XP_TICK_KEY)) {
                bond.putLong(LAST_RIDE_XP_TICK_KEY, gameTime);
            }
            markPrimaryOwnerIfSaddled(player, animal);
            return false;
        }
        long lastRideXp = bond.getLong(LAST_RIDE_XP_TICK_KEY);
        if (gameTime - lastRideXp < RIDE_XP_INTERVAL_TICKS) {
            markPrimaryOwnerIfSaddled(player, animal);
            return false;
        }
        if (distanceSqrFromLastRideXp(bond, animal) < RIDE_XP_MIN_DISTANCE_SQR) {
            markPrimaryOwnerIfSaddled(player, animal);
            return false;
        }
        bond.putLong(LAST_RIDE_XP_TICK_KEY, gameTime);
        markRidePosition(bond, animal);
        addXp(bond, RIDE_XP);
        markPrimaryOwnerIfSaddled(player, animal);
        return true;
    }

    /** Active animal roles can award small use XP without claiming unrelated animals. */
    public static boolean roleUse(Player player, LivingEntity animal, String roleKey, int xp, int cooldownTicks) {
        if (!canCareFor(player, animal)) {
            return false;
        }
        long gameTime = animal.level().getGameTime();
        CompoundTag bond = playerBond(animal, player.getUUID(), true);
        String lastKey = LAST_ROLE_XP_TICK_PREFIX + roleKey;
        long lastRoleXp = bond.getLong(lastKey);
        if (lastRoleXp != 0L && gameTime - lastRoleXp < cooldownTicks) {
            return false;
        }
        bond.putLong(lastKey, gameTime);
        addXp(bond, xp);
        return true;
    }

    /** Draft work is real travel/logistics use: cart work deepens bond slowly only over meaningful hauling distance. */
    public static boolean draftWork(Player player, LivingEntity animal) {
        if (!isBondable(animal) || snapshot(animal, player).ownedByOther()) {
            return false;
        }
        CompoundTag bond = playerBond(animal, player.getUUID(), true);
        markPrimaryOwnerIfSaddled(player, animal);
        long gameTime = animal.level().getGameTime();
        String lastKey = LAST_ROLE_XP_TICK_PREFIX + "draft";
        if (!bond.contains(LAST_DRAFT_X_KEY) || !bond.contains(LAST_DRAFT_Z_KEY)) {
            markDraftPosition(bond, animal);
            if (!bond.contains(lastKey)) {
                bond.putLong(lastKey, gameTime);
            }
            return false;
        }
        long lastDraftXp = bond.getLong(lastKey);
        if (gameTime - lastDraftXp < DRAFT_WORK_XP_INTERVAL_TICKS) {
            return false;
        }
        if (distanceSqrFromLastDraftXp(bond, animal) < DRAFT_WORK_XP_MIN_DISTANCE_SQR) {
            return false;
        }
        bond.putLong(lastKey, gameTime);
        markDraftPosition(bond, animal);
        addXp(bond, DRAFT_WORK_XP);
        return true;
    }

    public static Snapshot snapshot(LivingEntity animal, Player viewer) {
        int xp = dataIfPresent(animal)
                .map(data -> players(data, false))
                .map(players -> players.getCompound(viewer.getUUID().toString()).getInt(XP_KEY))
                .orElse(0);
        Optional<UUID> owner = primaryOwner(animal);
        boolean viewerOwns = owner.map(uuid -> uuid.equals(viewer.getUUID())).orElse(false);
        boolean ownedByOther = owner.isPresent() && !viewerOwns;
        return new Snapshot(labelFor(xp), xp, viewerOwns, ownedByOther);
    }

    public static Optional<UUID> primaryOwner(LivingEntity animal) {
        UUID vanilla = vanillaOwner(animal);
        if (vanilla != null) {
            return Optional.of(vanilla);
        }
        Optional<CompoundTag> maybeData = dataIfPresent(animal);
        if (maybeData.isEmpty() || !maybeData.get().contains(PRIMARY_OWNER_KEY)) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(maybeData.get().getString(PRIMARY_OWNER_KEY)));
        } catch (IllegalArgumentException ignored) {
            maybeData.get().remove(PRIMARY_OWNER_KEY);
            return Optional.empty();
        }
    }

    private static boolean canCareFor(Player player, LivingEntity animal) {
        return AnimalEligibility.canGainCareBond(player, animal);
    }

    private static void markPrimaryOwnerIfSaddled(Player player, LivingEntity animal) {
        if (primaryOwner(animal).isPresent()) {
            return;
        }
        if (animal instanceof AbstractHorse horse && horse.isSaddled()) {
            data(animal).putString(PRIMARY_OWNER_KEY, player.getUUID().toString());
        }
    }

    private static void markRidePosition(CompoundTag bond, LivingEntity animal) {
        bond.putDouble(LAST_RIDE_X_KEY, animal.getX());
        bond.putDouble(LAST_RIDE_Z_KEY, animal.getZ());
    }

    private static double distanceSqrFromLastRideXp(CompoundTag bond, LivingEntity animal) {
        double dx = animal.getX() - bond.getDouble(LAST_RIDE_X_KEY);
        double dz = animal.getZ() - bond.getDouble(LAST_RIDE_Z_KEY);
        return dx * dx + dz * dz;
    }

    private static void markDraftPosition(CompoundTag bond, LivingEntity animal) {
        bond.putDouble(LAST_DRAFT_X_KEY, animal.getX());
        bond.putDouble(LAST_DRAFT_Z_KEY, animal.getZ());
    }

    private static double distanceSqrFromLastDraftXp(CompoundTag bond, LivingEntity animal) {
        double dx = animal.getX() - bond.getDouble(LAST_DRAFT_X_KEY);
        double dz = animal.getZ() - bond.getDouble(LAST_DRAFT_Z_KEY);
        return dx * dx + dz * dz;
    }

    private static UUID vanillaOwner(LivingEntity animal) {
        if (animal instanceof OwnableEntity ownable) {
            return ownable.getOwnerUUID();
        }
        return null;
    }

    public static boolean isBondable(LivingEntity animal) {
        return AnimalEligibility.isBondable(animal);
    }

    private static CompoundTag playerBond(LivingEntity animal, UUID playerId, boolean create) {
        CompoundTag players = players(data(animal), true);
        String key = playerId.toString();
        if (!players.contains(key) && create) {
            players.put(key, new CompoundTag());
        }
        return players.getCompound(key);
    }

    private static CompoundTag players(CompoundTag data, boolean create) {
        if (!data.contains(PLAYERS_KEY) && create) {
            data.put(PLAYERS_KEY, new CompoundTag());
        }
        return data.getCompound(PLAYERS_KEY);
    }

    private static CompoundTag data(LivingEntity animal) {
        CompoundTag persistent = animal.getPersistentData();
        if (!persistent.contains(DATA_KEY)) {
            persistent.put(DATA_KEY, new CompoundTag());
        }
        return persistent.getCompound(DATA_KEY);
    }

    private static Optional<CompoundTag> dataIfPresent(LivingEntity animal) {
        CompoundTag persistent = animal.getPersistentData();
        if (!persistent.contains(DATA_KEY)) {
            return Optional.empty();
        }
        return Optional.of(persistent.getCompound(DATA_KEY));
    }

    private static long day(LivingEntity animal) {
        if (animal.level() instanceof ServerLevel serverLevel) {
            return serverLevel.getDayTime() / 24000L;
        }
        return animal.level().getGameTime() / 24000L;
    }

    private static void addXp(CompoundTag bond, int amount) {
        int next = Math.min(MAX_XP, bond.getInt(XP_KEY) + amount);
        bond.putInt(XP_KEY, next);
    }

    private static String labelFor(int xp) {
        if (xp >= 220) return translated("message.tenpack_travel.animal_bond.companion");
        if (xp >= 120) return translated("message.tenpack_travel.animal_bond.loyal");
        if (xp >= 60) return translated("message.tenpack_travel.animal_bond.trusted");
        if (xp >= 20) return translated("message.tenpack_travel.animal_bond.familiar");
        if (xp > 0) return translated("message.tenpack_travel.animal_bond.new_bond");
        return translated("message.tenpack_travel.animal_bond.unbonded");
    }

    private static String translated(String key) {
        return Component.translatable(key).getString();
    }

    public record Snapshot(String label, int xp, boolean ownedByViewer, boolean ownedByOther) {
    }
}

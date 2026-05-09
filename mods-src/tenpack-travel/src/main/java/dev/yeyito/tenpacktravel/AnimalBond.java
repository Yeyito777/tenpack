package dev.yeyito.tenpacktravel;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.camel.Camel;
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
    private static final String LAST_ROLE_XP_TICK_PREFIX = "last_role_xp_tick_";

    private static final int BRUSH_XP = 4;
    private static final int FEED_XP = 3;
    private static final int RIDE_XP = 2;
    private static final int RIDE_XP_INTERVAL_TICKS = 20 * 20;
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

    /** Riding/use is the main way a relationship starts. Saddled horse-like mounts also mark primary owner. */
    public static boolean ride(Player player, LivingEntity animal) {
        long gameTime = animal.level().getGameTime();
        CompoundTag bond = playerBond(animal, player.getUUID(), true);
        long lastRideXp = bond.getLong(LAST_RIDE_XP_TICK_KEY);
        if (lastRideXp != 0L && gameTime - lastRideXp < RIDE_XP_INTERVAL_TICKS) {
            markPrimaryOwnerIfSaddled(player, animal);
            return false;
        }
        bond.putLong(LAST_RIDE_XP_TICK_KEY, gameTime);
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
        Snapshot snapshot = snapshot(animal, player);
        return snapshot.xp() > 0 || snapshot.ownedByViewer();
    }

    private static void markPrimaryOwnerIfSaddled(Player player, LivingEntity animal) {
        if (primaryOwner(animal).isPresent()) {
            return;
        }
        if (animal instanceof AbstractHorse horse && horse.isSaddled()) {
            data(animal).putString(PRIMARY_OWNER_KEY, player.getUUID().toString());
        }
    }

    private static UUID vanillaOwner(LivingEntity animal) {
        if (animal instanceof OwnableEntity ownable) {
            return ownable.getOwnerUUID();
        }
        return null;
    }

    public static boolean isBondable(LivingEntity animal) {
        return animal instanceof Animal || animal instanceof Camel || animal instanceof AbstractHorse;
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
        if (xp >= 220) return "Companion";
        if (xp >= 120) return "Loyal";
        if (xp >= 60) return "Trusted";
        if (xp >= 20) return "Familiar";
        if (xp > 0) return "New bond";
        return "Unbonded";
    }

    public record Snapshot(String label, int xp, boolean ownedByViewer, boolean ownedByOther) {
    }
}

package dev.yeyito.tenpacktravel;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** Persistent local command mode for Tenpack Travel companions. No teleport, no virtual storage. */
final class AnimalCommand {
    private static final String DATA_KEY = "tenpack_travel_command";
    private static final String MODE_KEY = "mode";
    private static final String OWNER_KEY = "owner";
    private static final String ANCHOR_X_KEY = "anchor_x";
    private static final String ANCHOR_Y_KEY = "anchor_y";
    private static final String ANCHOR_Z_KEY = "anchor_z";
    private static final String LAST_SET_TICK_KEY = "last_set_tick";

    private AnimalCommand() {
    }

    static boolean canCommand(Player player, LivingEntity animal) {
        if (!AnimalEligibility.isBondable(animal)) {
            return false;
        }
        if (player.isCreative() || player.isSpectator()) {
            return true;
        }
        AnimalBond.Snapshot bond = AnimalBond.snapshot(animal, player);
        return !bond.ownedByOther() && (bond.xp() > 0 || bond.ownedByViewer());
    }

    static Snapshot snapshot(LivingEntity animal) {
        CompoundTag command = dataIfPresent(animal);
        if (command == null) {
            return Snapshot.free();
        }
        Mode mode = Mode.fromId(command.getString(MODE_KEY));
        if (mode == Mode.FREE) {
            return Snapshot.free();
        }
        UUID owner = null;
        if (command.contains(OWNER_KEY)) {
            try {
                owner = UUID.fromString(command.getString(OWNER_KEY));
            } catch (IllegalArgumentException ignored) {
                owner = null;
            }
        }
        BlockPos anchor = null;
        if (command.contains(ANCHOR_X_KEY) && command.contains(ANCHOR_Y_KEY) && command.contains(ANCHOR_Z_KEY)) {
            anchor = new BlockPos(command.getInt(ANCHOR_X_KEY), command.getInt(ANCHOR_Y_KEY), command.getInt(ANCHOR_Z_KEY));
        }
        return new Snapshot(mode, Optional.ofNullable(owner), Optional.ofNullable(anchor));
    }

    static void setMode(Player player, LivingEntity animal, Mode mode) {
        setModeAtAnchor(player, animal, mode, anchorFor(animal, player, mode));
    }

    static void setModeAtAnchor(Player player, LivingEntity animal, Mode mode, BlockPos anchor) {
        if (mode == Mode.FREE) {
            clear(animal);
            applyImmediate(animal, mode, player);
            return;
        }
        CompoundTag command = data(animal);
        command.putString(MODE_KEY, mode.id());
        command.putString(OWNER_KEY, player.getUUID().toString());
        command.putInt(ANCHOR_X_KEY, anchor.getX());
        command.putInt(ANCHOR_Y_KEY, anchor.getY());
        command.putInt(ANCHOR_Z_KEY, anchor.getZ());
        command.putLong(LAST_SET_TICK_KEY, animal.level().getGameTime());
        if (animal instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        applyImmediate(animal, mode, player, anchor);
    }

    static void clear(LivingEntity animal) {
        animal.getPersistentData().remove(DATA_KEY);
        if (animal instanceof Mob mob) {
            mob.clearRestriction();
            mob.getNavigation().stop();
        }
        if (animal instanceof TamableAnimal tameable) {
            tameable.setOrderedToSit(false);
        }
    }

    static void applyImmediate(LivingEntity animal, Mode mode, Player player) {
        applyImmediate(animal, mode, player, anchorFor(animal, player, mode));
    }

    private static void applyImmediate(LivingEntity animal, Mode mode, Player player, BlockPos anchor) {
        if (!(animal instanceof Mob mob)) {
            return;
        }
        switch (mode) {
            case FREE -> {
                mob.clearRestriction();
                mob.getNavigation().stop();
                if (animal instanceof TamableAnimal tameable) {
                    tameable.setOrderedToSit(false);
                }
            }
            case FOLLOW -> {
                mob.clearRestriction();
                if (animal instanceof TamableAnimal tameable) {
                    tameable.setOrderedToSit(false);
                }
                if (animal.distanceToSqr(player) > 9.0D) {
                    mob.getNavigation().moveTo(player, followSpeed(animal, player));
                }
            }
            case STAY -> {
                mob.restrictTo(anchor, 2);
                mob.getNavigation().stop();
                if (animal instanceof TamableAnimal tameable) {
                    tameable.setOrderedToSit(true);
                }
            }
            case ROAM -> {
                mob.restrictTo(anchor, 8);
                if (animal instanceof TamableAnimal tameable) {
                    tameable.setOrderedToSit(false);
                }
            }
        }
    }

    static double followSpeed(LivingEntity animal, Player player) {
        int xp = AnimalBond.snapshot(animal, player).xp();
        if (xp >= 220) return 1.28D;
        if (xp >= 120) return 1.20D;
        if (xp >= 60) return 1.12D;
        return 1.05D;
    }

    private static BlockPos anchorFor(LivingEntity animal, Player player, Mode mode) {
        return switch (mode) {
            case FOLLOW -> player.blockPosition();
            case STAY, ROAM -> animal.blockPosition();
            case FREE -> animal.blockPosition();
        };
    }

    private static CompoundTag data(LivingEntity animal) {
        CompoundTag persistent = animal.getPersistentData();
        if (!persistent.contains(DATA_KEY)) {
            persistent.put(DATA_KEY, new CompoundTag());
        }
        return persistent.getCompound(DATA_KEY);
    }

    private static CompoundTag dataIfPresent(LivingEntity animal) {
        CompoundTag persistent = animal.getPersistentData();
        return persistent.contains(DATA_KEY) ? persistent.getCompound(DATA_KEY) : null;
    }

    enum Mode {
        FREE("free", "screen.tenpack_travel.command.free"),
        FOLLOW("follow", "screen.tenpack_travel.command.follow"),
        STAY("stay", "screen.tenpack_travel.command.stay"),
        ROAM("roam", "screen.tenpack_travel.command.roam");

        private final String id;
        private final String translationKey;

        Mode(String id, String translationKey) {
            this.id = id;
            this.translationKey = translationKey;
        }

        String id() {
            return id;
        }

        String label() {
            return labelComponent().getString();
        }

        Component labelComponent() {
            return Component.translatable(translationKey);
        }

        static Mode fromId(String id) {
            if (id == null || id.isBlank()) {
                return FREE;
            }
            String normalized = id.toLowerCase(Locale.ROOT);
            for (Mode mode : values()) {
                if (mode.id.equals(normalized)) {
                    return mode;
                }
            }
            return FREE;
        }
    }

    record Snapshot(Mode mode, Optional<UUID> owner, Optional<BlockPos> anchor) {
        static Snapshot free() {
            return new Snapshot(Mode.FREE, Optional.empty(), Optional.empty());
        }

        String label() {
            return mode.label();
        }
    }
}

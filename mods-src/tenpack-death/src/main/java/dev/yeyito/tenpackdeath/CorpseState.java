package dev.yeyito.tenpackdeath;

import de.maxhenkel.corpse.entities.CorpseEntity;
import net.minecraft.nbt.CompoundTag;

import java.lang.reflect.Field;

final class CorpseState {
    static final String REMOVED_STACKS_KEY = "removed_stacks";
    static final String WARNED_KEY = "decay_warned";
    static final String DECAY_STARTED_KEY = "decay_started";
    static final String ERROR_KEY = "decay_error";
    static final String XP_POINTS_KEY = "stored_xp_points";

    private static final String DATA_KEY = "tenpackdeath";
    private static final Field CORPSE_AGE_FIELD = findCorpseAgeField();

    private CorpseState() {
    }

    static CompoundTag of(CorpseEntity corpse) {
        CompoundTag persistent = corpse.getPersistentData();
        if (!persistent.contains(DATA_KEY)) {
            persistent.put(DATA_KEY, new CompoundTag());
        }
        return persistent.getCompound(DATA_KEY);
    }

    static int ageTicks(CorpseEntity corpse) {
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
}

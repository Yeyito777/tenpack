package dev.yeyito.tenpacktravel;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/** Persistent, visible care state for Tenpack Travel animals. */
final class AnimalCare {
    private static final String DATA_KEY = "tenpack_travel_care";
    private static final String LAST_GROOMED_DAY_KEY = "last_groomed_day";
    private static final String LAST_FED_DAY_KEY = "last_fed_day";
    private static final String LAST_GROOMED_TICK_KEY = "last_groomed_tick";
    private static final String LAST_FED_TICK_KEY = "last_fed_tick";
    private static final String LAST_WORK_TICK_KEY = "last_work_tick";
    private static final String LAST_WORK_DAY_KEY = "last_work_day";
    private static final String LAST_KEEPER_KEY = "last_keeper";

    private AnimalCare() {
    }

    static CareUpdate groom(Player player, LivingEntity animal) {
        CompoundTag care = data(animal);
        long today = day(animal);
        boolean firstToday = care.getLong(LAST_GROOMED_DAY_KEY) != today;
        care.putLong(LAST_GROOMED_DAY_KEY, today);
        care.putLong(LAST_GROOMED_TICK_KEY, animal.level().getGameTime());
        care.putString(LAST_KEEPER_KEY, player.getUUID().toString());

        if (firstToday) {
            if (animal.getHealth() < animal.getMaxHealth()) {
                animal.heal(1.0F);
            }
            if (animal instanceof AbstractHorse horse && !horse.isTamed()) {
                horse.modifyTemper(2);
            }
        }
        return new CareUpdate(firstToday, mood(animal), careSummary(animal));
    }

    static CareUpdate feed(Player player, LivingEntity animal) {
        return markFed(player.getUUID().toString(), animal);
    }

    static CareUpdate feedFromTrough(LivingEntity animal) {
        return markFed("trough", animal);
    }

    static CareUpdate draftWork(Player player, LivingEntity animal) {
        CompoundTag care = data(animal);
        long today = day(animal);
        boolean firstToday = care.getLong(LAST_WORK_DAY_KEY) != today;
        care.putLong(LAST_WORK_DAY_KEY, today);
        care.putLong(LAST_WORK_TICK_KEY, animal.level().getGameTime());
        care.putString(LAST_KEEPER_KEY, player.getUUID().toString());
        return new CareUpdate(firstToday, mood(animal), careSummary(animal));
    }

    private static CareUpdate markFed(String keeper, LivingEntity animal) {
        CompoundTag care = data(animal);
        long today = day(animal);
        boolean firstToday = care.getLong(LAST_FED_DAY_KEY) != today;
        care.putLong(LAST_FED_DAY_KEY, today);
        care.putLong(LAST_FED_TICK_KEY, animal.level().getGameTime());
        care.putString(LAST_KEEPER_KEY, keeper);
        return new CareUpdate(firstToday, mood(animal), careSummary(animal));
    }

    static boolean shouldTakeTroughFeed(LivingEntity animal) {
        return animal.getHealth() < animal.getMaxHealth() || !fedToday(animal);
    }

    static String mood(LivingEntity animal) {
        if (!animal.isAlive()) {
            return translated("message.tenpack_travel.animal_mood.gone");
        }
        float healthRatio = animal.getHealth() / Math.max(1.0F, animal.getMaxHealth());
        if (healthRatio <= 0.35F) {
            return translated("message.tenpack_travel.animal_mood.hurt");
        }
        boolean groomed = groomedToday(animal);
        boolean fed = fedToday(animal);
        if (workedRecently(animal) && !(groomed && fed)) {
            return fed ? translated("message.tenpack_travel.animal_mood.worked") : translated("message.tenpack_travel.animal_mood.tired");
        }
        if (groomed && fed) {
            return translated("message.tenpack_travel.animal_mood.content");
        }
        if (groomed) {
            return translated("message.tenpack_travel.animal_mood.calm");
        }
        if (fed) {
            return translated("message.tenpack_travel.animal_mood.settled");
        }
        if (hasCareHistory(animal) && daysSinceFed(animal) >= 2) {
            return translated("message.tenpack_travel.animal_mood.hungry");
        }
        return translated("message.tenpack_travel.animal_mood.watchful");
    }

    static String careSummary(LivingEntity animal) {
        boolean groomed = groomedToday(animal);
        boolean fed = fedToday(animal);
        boolean worked = workedRecently(animal);
        if (groomed && fed) {
            return worked ? translated("message.tenpack_travel.animal_care.worked_groomed_fed") : translated("message.tenpack_travel.animal_care.groomed_fed");
        }
        if (groomed) {
            return worked ? translated("message.tenpack_travel.animal_care.worked_groomed_needs_feed") : translated("message.tenpack_travel.animal_care.groomed_needs_feed");
        }
        if (fed) {
            return worked ? translated("message.tenpack_travel.animal_care.worked_fed_needs_grooming") : translated("message.tenpack_travel.animal_care.fed_needs_grooming");
        }
        if (worked) {
            return translated("message.tenpack_travel.animal_care.worked_needs_care");
        }
        if (hasCareHistory(animal)) {
            return translated("message.tenpack_travel.animal_care.needs_daily_care");
        }
        return translated("message.tenpack_travel.animal_care.no_care_recorded");
    }

    private static String translated(String key) {
        return Component.translatable(key).getString();
    }

    static void playGroomingReaction(ServerLevel level, LivingEntity animal, Player player, boolean firstToday) {
        if (animal instanceof Mob mob) {
            mob.getLookControl().setLookAt(player, 30.0F, 30.0F);
        }
        level.playSound(null, animal.getX(), animal.getY(), animal.getZ(), SoundEvents.BRUSH_GENERIC, SoundSource.NEUTRAL, 0.75F, firstToday ? 0.95F : 1.15F);
        level.sendParticles(firstToday ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.POOF,
                animal.getX(), animal.getY() + animal.getBbHeight() * 0.75D, animal.getZ(),
                firstToday ? 5 : 2, 0.25D, 0.18D, 0.25D, 0.02D);
        if (firstToday && animal instanceof Mob mob) {
            mob.playAmbientSound();
        }
    }

    static void playFeedingReaction(ServerLevel level, LivingEntity animal, boolean firstToday) {
        level.playSound(null, animal.getX(), animal.getY(), animal.getZ(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.7F, 0.9F + level.random.nextFloat() * 0.2F);
        level.sendParticles(firstToday ? ParticleTypes.HEART : ParticleTypes.HAPPY_VILLAGER,
                animal.getX(), animal.getY() + animal.getBbHeight() + 0.1D, animal.getZ(),
                firstToday ? 2 : 1, 0.25D, 0.15D, 0.25D, 0.02D);
    }

    private static boolean groomedToday(LivingEntity animal) {
        CompoundTag care = dataIfPresent(animal);
        return care != null && care.getLong(LAST_GROOMED_DAY_KEY) == day(animal);
    }

    private static boolean fedToday(LivingEntity animal) {
        CompoundTag care = dataIfPresent(animal);
        return care != null && care.getLong(LAST_FED_DAY_KEY) == day(animal);
    }

    private static boolean workedRecently(LivingEntity animal) {
        CompoundTag care = dataIfPresent(animal);
        if (care == null || !care.contains(LAST_WORK_TICK_KEY)) {
            return false;
        }
        return animal.level().getGameTime() - care.getLong(LAST_WORK_TICK_KEY) < 20L * 60L * 10L;
    }

    private static int daysSinceFed(LivingEntity animal) {
        CompoundTag care = dataIfPresent(animal);
        if (care == null || !care.contains(LAST_FED_DAY_KEY)) {
            return 999;
        }
        return (int) Math.max(0L, day(animal) - care.getLong(LAST_FED_DAY_KEY));
    }

    private static boolean hasCareHistory(LivingEntity animal) {
        CompoundTag care = dataIfPresent(animal);
        return care != null && (care.contains(LAST_GROOMED_TICK_KEY) || care.contains(LAST_FED_TICK_KEY));
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

    private static long day(LivingEntity animal) {
        if (animal.level() instanceof ServerLevel serverLevel) {
            return serverLevel.getDayTime() / 24000L;
        }
        return animal.level().getGameTime() / 24000L;
    }

    record CareUpdate(boolean firstToday, String mood, String careSummary) {
    }
}

package dev.yeyito.tenpacktravel;

import java.util.Locale;

import net.minecraft.network.chat.Component;

final class AnimalStatBands {
    private AnimalStatBands() {
    }

    static String currentHealth(double health, double maxHealth) {
        if (maxHealth <= 0.0) return translated("message.tenpack_travel.animal_stat.health_state.unknown");
        double ratio = health / maxHealth;
        if (ratio < 0.25) return translated("message.tenpack_travel.animal_stat.health_state.hurt");
        if (ratio < 0.6) return translated("message.tenpack_travel.animal_stat.health_state.worn");
        if (ratio < 0.9) return translated("message.tenpack_travel.animal_stat.health_state.healthy");
        return translated("message.tenpack_travel.animal_stat.health_state.fresh");
    }

    static String maxHealth(double maxHealth) {
        if (maxHealth < 15.0) return translated("message.tenpack_travel.animal_stat.max_health.frail");
        if (maxHealth < 25.0) return translated("message.tenpack_travel.animal_stat.max_health.healthy");
        if (maxHealth < 40.0) return translated("message.tenpack_travel.animal_stat.max_health.sturdy");
        return translated("message.tenpack_travel.animal_stat.max_health.massive");
    }

    static String speed(double speed) {
        if (speed < 0.18) return translated("message.tenpack_travel.animal_stat.speed.slow");
        if (speed < 0.25) return translated("message.tenpack_travel.animal_stat.speed.steady");
        if (speed < 0.32) return translated("message.tenpack_travel.animal_stat.speed.swift");
        return translated("message.tenpack_travel.animal_stat.speed.exceptional");
    }

    static String jump(double jump) {
        if (jump < 0.45) return translated("message.tenpack_travel.animal_stat.jump.poor");
        if (jump < 0.65) return translated("message.tenpack_travel.animal_stat.jump.fair");
        if (jump < 0.85) return translated("message.tenpack_travel.animal_stat.jump.strong");
        return translated("message.tenpack_travel.animal_stat.jump.remarkable");
    }

    static String rounded(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static String translated(String key) {
        return Component.translatable(key).getString();
    }
}

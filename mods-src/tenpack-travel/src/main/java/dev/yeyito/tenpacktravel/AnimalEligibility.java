package dev.yeyito.tenpacktravel;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * Central policy surface for Tenpack Travel animal interactions.
 *
 * <p>The mod deliberately avoids pretending that every mob supports every travel behavior. Inspection, bonding,
 * whistle response, trough care, leash policy, and active roles are related but not identical. Keeping the first
 * gatekeeping decisions here makes future any-mob taming / Doggy-Talents-style command work safer than scattering
 * broad {@code instanceof Animal} checks across handlers.</p>
 */
final class AnimalEligibility {
    private AnimalEligibility() {
    }

    static boolean isInspectable(Entity entity) {
        if (entity instanceof AbstractHorse || entity instanceof Camel) {
            return true;
        }
        return alexRole(entity) != null;
    }

    static boolean isBondable(LivingEntity entity) {
        return entity instanceof Animal || entity instanceof Camel || entity instanceof AbstractHorse;
    }

    static boolean canGainCareBond(Player player, LivingEntity entity) {
        if (!isBondable(entity)) {
            return false;
        }
        AnimalBond.Snapshot snapshot = AnimalBond.snapshot(entity, player);
        return !snapshot.ownedByOther() && (snapshot.xp() > 0 || snapshot.ownedByViewer());
    }

    static boolean canUseTrough(LivingEntity entity) {
        if (!entity.isAlive() || entity.isBaby()) {
            return false;
        }
        if (entity instanceof AbstractHorse horse) {
            return horse.isTamed();
        }
        if (entity instanceof TamableAnimal tameable) {
            return tameable.isTame();
        }
        if (entity instanceof Camel) {
            return true;
        }
        if (entity instanceof Animal) {
            Entity leashHolder = entity instanceof net.minecraft.world.entity.Leashable leashable ? leashable.getLeashHolder() : null;
            return leashHolder != null;
        }
        return false;
    }

    static AlexAnimalRole alexRole(Entity entity) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (!id.getNamespace().equals("alexsmobs")) {
            return null;
        }
        return AlexAnimalRole.forPath(id.getPath());
    }
}

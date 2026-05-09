package dev.yeyito.tenpacktravel;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

record AnimalInspectionReport(String name, String species, double health, double maxHealth, Double speed, Double jump,
                              AnimalBond.Snapshot bond,
                              String temperament, String role, String notes) {
    static AnimalInspectionReport from(Entity entity, Player viewer) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String species = id.toString();
        String name = entity.getDisplayName().getString();

        if (entity instanceof AbstractHorse horse) {
            return fromHorse(name, species, horse, viewer);
        }

        if (entity instanceof Camel camel) {
            return new AnimalInspectionReport(name, species, camel.getHealth(), camel.getMaxHealth(),
                    camel.getAttributeValue(Attributes.MOVEMENT_SPEED), attributeValue(camel, Attributes.JUMP_STRENGTH),
                    AnimalBond.snapshot(camel, viewer), "Steady", "Passenger mount", "two riders; poor cargo, good road travel comfort");
        }

        if (entity instanceof LivingEntity living && id.getNamespace().equals("alexsmobs")) {
            AlexAnimalRole role = AlexAnimalRole.forPath(id.getPath());
            if (role == null) {
                return null;
            }
            String temperament = living instanceof TamableAnimal tameable
                    ? (tameable.isTame() ? "Loyal" : "Wild")
                    : role.temperament();
            return new AnimalInspectionReport(name, species, living.getHealth(), living.getMaxHealth(),
                    attributeValue(living, Attributes.MOVEMENT_SPEED), attributeValue(living, Attributes.JUMP_STRENGTH),
                    AnimalBond.snapshot(living, viewer), temperament, role.role(), role.notes());
        }

        return null;
    }

    private static AnimalInspectionReport fromHorse(String name, String species, AbstractHorse horse, Player viewer) {
        double health = horse.getHealth();
        double maxHealth = horse.getMaxHealth();
        double speed = horse.getAttributeValue(Attributes.MOVEMENT_SPEED);
        Double jump = attributeValue(horse, Attributes.JUMP_STRENGTH);
        String role = vanillaHorseRole(horse);
        String temperament = horse.isTamed() ? "Calm" : temperamentFromTemper(horse.getTemper());
        String notes = horse.isTamed() ? "tamed" : "untamed; ride/training still matters";
        return new AnimalInspectionReport(name, species, health, maxHealth, speed, jump,
                AnimalBond.snapshot(horse, viewer), temperament, role, notes);
    }

    List<Component> lines(boolean exact) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("§6" + name + "§7 (" + species + ")"));
        lines.add(Component.literal("§7Health: §f" + bandHealth(maxHealth) + "§8 (currently " + bandCurrentHealth(health, maxHealth) + ")"));
        if (speed != null) {
            lines.add(Component.literal("§7Speed: §f" + bandSpeed(speed)));
        }
        if (jump != null && jump > 0.0) {
            lines.add(Component.literal("§7Jump: §f" + bandJump(jump)));
        }
        lines.add(Component.literal("§7Bond: §f" + bond.label()));
        lines.add(Component.literal("§7Temperament: §f" + temperament));
        lines.add(Component.literal("§7Role: §f" + role));
        if (notes != null && !notes.isBlank()) {
            lines.add(Component.literal("§7Notes: §f" + notes));
        }
        if (exact) {
            lines.add(Component.literal("§8Debug: health " + round(health) + "/" + round(maxHealth)
                    + (speed == null ? "" : ", speed " + round(speed))
                    + (jump == null ? "" : ", jump " + round(jump))
                    + ", bond xp " + bond.xp()));
        }
        return lines;
    }

    private static String vanillaHorseRole(AbstractHorse horse) {
        if (horse instanceof Llama llama) {
            return llama.hasChest() ? "Pack — " + llama.getInventoryColumns() * 3 + " slots" : "Potential pack — add chest";
        }
        if (horse instanceof AbstractChestedHorse chestedHorse) {
            return chestedHorse.hasChest() ? "Pack — 15 slots" : "Potential pack — add chest";
        }
        return "None";
    }

    private static String temperamentFromTemper(int temper) {
        if (temper < 20) return "Skittish";
        if (temper < 60) return "Learning";
        if (temper < 90) return "Trusting";
        return "Nearly tamed";
    }

    private static Double attributeValue(LivingEntity entity, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute) {
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance == null ? null : instance.getValue();
    }

    private static String bandCurrentHealth(double health, double maxHealth) {
        if (maxHealth <= 0.0) return "unknown";
        double ratio = health / maxHealth;
        if (ratio < 0.25) return "hurt";
        if (ratio < 0.6) return "worn";
        if (ratio < 0.9) return "healthy";
        return "fresh";
    }

    private static String bandHealth(double maxHealth) {
        if (maxHealth < 15.0) return "Frail";
        if (maxHealth < 25.0) return "Healthy";
        if (maxHealth < 40.0) return "Sturdy";
        return "Massive";
    }

    private static String bandSpeed(double speed) {
        if (speed < 0.18) return "Slow";
        if (speed < 0.25) return "Steady";
        if (speed < 0.32) return "Swift";
        return "Exceptional";
    }

    private static String bandJump(double jump) {
        if (jump < 0.45) return "Poor";
        if (jump < 0.65) return "Fair";
        if (jump < 0.85) return "Strong";
        return "Remarkable";
    }

    private static String round(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}

package dev.yeyito.tenpacktravel;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.ChatFormatting;
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

record AnimalInspectionReport(String name, String species, double health, double maxHealth, Double speed, Double jump,
                              AnimalBond.Snapshot bond,
                              String temperament, String mood, String care, String role, String notes) {
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
                    AnimalBond.snapshot(camel, viewer), translated("message.tenpack_travel.animal_temperament.camel_steady"), AnimalCare.mood(camel), AnimalCare.careSummary(camel),
                    translated("message.tenpack_travel.animal_role.passenger_mount"), translated("message.tenpack_travel.animal_notes.camel_two_riders"));
        }

        if (entity instanceof LivingEntity living) {
            AlexAnimalRole role = AnimalEligibility.alexRole(entity);
            if (role == null) {
                return null;
            }
            String temperament = living instanceof TamableAnimal tameable
                    ? (tameable.isTame() ? translated("message.tenpack_travel.animal_temperament.tame_loyal") : translated("message.tenpack_travel.animal_temperament.tame_wild"))
                    : role.temperament();
            return new AnimalInspectionReport(name, species, living.getHealth(), living.getMaxHealth(),
                    attributeValue(living, Attributes.MOVEMENT_SPEED), attributeValue(living, Attributes.JUMP_STRENGTH),
                    AnimalBond.snapshot(living, viewer), temperament, AnimalCare.mood(living), AnimalCare.careSummary(living),
                    role.role(), role.notes());
        }

        return null;
    }

    private static AnimalInspectionReport fromHorse(String name, String species, AbstractHorse horse, Player viewer) {
        double health = horse.getHealth();
        double maxHealth = horse.getMaxHealth();
        double speed = horse.getAttributeValue(Attributes.MOVEMENT_SPEED);
        Double jump = attributeValue(horse, Attributes.JUMP_STRENGTH);
        String role = vanillaHorseRole(horse);
        String temperament = horse.isTamed() ? translated("message.tenpack_travel.animal_temperament.horse_calm") : temperamentFromTemper(horse.getTemper());
        String notes = horse.isTamed() ? translated("message.tenpack_travel.animal_notes.tamed") : translated("message.tenpack_travel.animal_notes.horse_untamed_training");
        return new AnimalInspectionReport(name, species, health, maxHealth, speed, jump,
                AnimalBond.snapshot(horse, viewer), temperament, AnimalCare.mood(horse), AnimalCare.careSummary(horse), role, notes);
    }

    List<Component> lines(boolean exact) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("message.tenpack_travel.animal_inspection.header", name, species).withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("message.tenpack_travel.animal_inspection.health", AnimalStatBands.maxHealth(maxHealth), AnimalStatBands.currentHealth(health, maxHealth)).withStyle(ChatFormatting.GRAY));
        if (speed != null) {
            lines.add(Component.translatable("message.tenpack_travel.animal_inspection.speed", AnimalStatBands.speed(speed)).withStyle(ChatFormatting.GRAY));
        }
        if (jump != null && jump > 0.0) {
            lines.add(Component.translatable("message.tenpack_travel.animal_inspection.jump", AnimalStatBands.jump(jump)).withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable("message.tenpack_travel.animal_inspection.bond", bond.label()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("message.tenpack_travel.animal_inspection.temperament", temperament).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("message.tenpack_travel.animal_inspection.mood", mood).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("message.tenpack_travel.animal_inspection.care", care).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("message.tenpack_travel.animal_inspection.role", role).withStyle(ChatFormatting.GRAY));
        if (notes != null && !notes.isBlank()) {
            lines.add(Component.translatable("message.tenpack_travel.animal_inspection.notes", notes).withStyle(ChatFormatting.GRAY));
        }
        if (exact) {
            lines.add(Component.translatable("message.tenpack_travel.animal_inspection.debug",
                    AnimalStatBands.rounded(health),
                    AnimalStatBands.rounded(maxHealth),
                    speed == null ? "" : Component.translatable("message.tenpack_travel.animal_inspection.debug_speed", AnimalStatBands.rounded(speed)),
                    jump == null ? "" : Component.translatable("message.tenpack_travel.animal_inspection.debug_jump", AnimalStatBands.rounded(jump)),
                    bond.xp()).withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    private static String vanillaHorseRole(AbstractHorse horse) {
        if (horse instanceof Llama llama) {
            return llama.hasChest()
                    ? translated("message.tenpack_travel.animal_role.pack_slots", llama.getInventoryColumns() * 3)
                    : translated("message.tenpack_travel.animal_role.pack_candidate");
        }
        if (horse instanceof AbstractChestedHorse chestedHorse) {
            return chestedHorse.hasChest()
                    ? translated("message.tenpack_travel.animal_role.pack_slots", 15)
                    : translated("message.tenpack_travel.animal_role.pack_candidate");
        }
        return AstikorIntegration.draftRole(horse);
    }

    private static String temperamentFromTemper(int temper) {
        if (temper < 20) return translated("message.tenpack_travel.animal_temperament.horse_skittish");
        if (temper < 60) return translated("message.tenpack_travel.animal_temperament.horse_learning");
        if (temper < 90) return translated("message.tenpack_travel.animal_temperament.horse_trusting");
        return translated("message.tenpack_travel.animal_temperament.horse_nearly_tamed");
    }

    private static String translated(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    private static Double attributeValue(LivingEntity entity, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute) {
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance == null ? null : instance.getValue();
    }

}

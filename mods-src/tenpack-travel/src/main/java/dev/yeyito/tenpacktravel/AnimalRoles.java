package dev.yeyito.tenpacktravel;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public final class AnimalRoles {
    public enum ActiveRole {
        SCOUT
    }

    private AnimalRoles() {
    }

    public static boolean hasRole(Entity entity, ActiveRole role) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return switch (role) {
            case SCOUT -> id.getNamespace().equals("alexsmobs") && id.getPath().equals("bald_eagle");
        };
    }
}

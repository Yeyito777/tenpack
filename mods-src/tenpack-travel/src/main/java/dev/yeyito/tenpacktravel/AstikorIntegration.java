package dev.yeyito.tenpacktravel;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

/** Soft AstikorCarts Redux bridge. Keep Tenpack Travel runnable without Astikor installed. */
final class AstikorIntegration {
    private static final String ASTIKOR_DRAWN_ENTITY = "com.jusipat.astikorcartsredux.entity.AbstractDrawnEntity";
    private static final double ACTIVE_DRAFT_SCAN_RADIUS = 8.0D;

    private static Class<?> drawnEntityClass;
    private static Method getPullingMethod;
    private static boolean lookupAttempted;

    private AstikorIntegration() {
    }

    static boolean isDrawnEntity(Entity entity) {
        Class<?> clazz = drawnEntityClass();
        return clazz != null && clazz.isInstance(entity);
    }

    static Entity getPulling(Entity cart) {
        Method method = getPullingMethod();
        if (method == null) {
            return null;
        }
        try {
            Object result = method.invoke(cart);
            return result instanceof Entity entity ? entity : null;
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            return null;
        }
    }

    static Optional<Entity> activeCartFor(LivingEntity animal) {
        Class<?> clazz = drawnEntityClass();
        if (clazz == null) {
            return Optional.empty();
        }
        return animal.level()
                .getEntitiesOfClass(Entity.class, animal.getBoundingBox().inflate(ACTIVE_DRAFT_SCAN_RADIUS), AstikorIntegration::isDrawnEntity)
                .stream()
                .filter(Entity::isAlive)
                .filter(cart -> getPulling(cart) == animal)
                .findFirst();
    }

    static boolean isActiveDraftAnimal(LivingEntity animal) {
        return activeCartFor(animal).isPresent();
    }

    static String draftRole(LivingEntity animal) {
        return activeCartFor(animal)
                .map(cart -> Component.translatable("message.tenpack_travel.animal_role.draft_active", cart.getType().getDescription()).getString())
                .orElse(Component.translatable("message.tenpack_travel.animal_role.draft_candidate").getString());
    }

    private static Class<?> drawnEntityClass() {
        if (!lookupAttempted) {
            lookupAttempted = true;
            try {
                drawnEntityClass = Class.forName(ASTIKOR_DRAWN_ENTITY);
            } catch (ClassNotFoundException ignored) {
                drawnEntityClass = null;
            }
        }
        return drawnEntityClass;
    }

    private static Method getPullingMethod() {
        if (getPullingMethod != null) {
            return getPullingMethod;
        }
        Class<?> clazz = drawnEntityClass();
        if (clazz == null) {
            return null;
        }
        try {
            getPullingMethod = clazz.getMethod("getPulling");
        } catch (NoSuchMethodException ignored) {
            getPullingMethod = null;
        }
        return getPullingMethod;
    }
}

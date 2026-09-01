package com.zenith.udl.manager;


import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class DeathTargetManager {
    // @TODO: いつかやる
    private static final Set<Entity> DEATH_TARGET_ENTITIES =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    public static void addTarget(Entity entity) {
        if (entity != null && !entity.isRemoved()) {
            DEATH_TARGET_ENTITIES.add(entity);
        }
    }

    public static void removeTarget(Entity entity) {
        if (entity != null) {
            DEATH_TARGET_ENTITIES.remove(entity);
        }
    }

    public static boolean isTarget(Entity entity) {
        // null またはワールドから除去されている（isRemoved）場合は即座に false を返す
        if (entity == null || entity.isRemoved()) {
            return false;
        }
        return DEATH_TARGET_ENTITIES.contains(entity);
    }

    public static Set<Entity> getTargetEntities() {
        return DEATH_TARGET_ENTITIES;
    }
}

package com.zenith.udl.manager;

import net.minecraft.world.entity.LivingEntity;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class TargetManager {
    // WeakHashMap をベースにした Set。
    // エンティティへの強参照を持たないため、Minecraft 側で破棄されると自動的にリストから回収されます。
    // マルチスレッドアクセスに対応するため Synchronized化 しています。
    private static final Set<LivingEntity> TARGET_ENTITIES =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    public static void addTarget(LivingEntity entity) {
        if (entity != null && !entity.isRemoved()) {
            TARGET_ENTITIES.add(entity);
        }
    }

    public static void removeTarget(LivingEntity entity) {
        if (entity != null) {
            TARGET_ENTITIES.remove(entity);
        }
    }

    public static boolean isTarget(LivingEntity entity) {
        // null またはワールドから除去されている（isRemoved）場合は即座に false を返す
        if (entity == null || entity.isRemoved()) {
            return false;
        }
        return TARGET_ENTITIES.contains(entity);
    }

    public static Set<LivingEntity> getTargetEntities() {
        return TARGET_ENTITIES;
    }
}
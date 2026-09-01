package com.zenith.udl.manager;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class TargetManager {
    // 1. 体力を変更するエンティティのリスト（LivingEntity専用）
    private static final Set<LivingEntity> HEALTH_TARGETS =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    // 2. 殺害対象のエンティティのリスト（LivingEntity だけでなく一般的な Entity 全般）
    private static final Set<Entity> KILL_TARGETS =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    // --- 体力変更リスト用のメソッド ---
    public static void addHealthTarget(LivingEntity entity) {
        if (entity != null && !entity.isRemoved()) {
            HEALTH_TARGETS.add(entity);
        }
    }

    public static void removeHealthTarget(LivingEntity entity) {
        if (entity != null) {
            HEALTH_TARGETS.remove(entity);
        }
    }

    public static boolean isHealthTarget(LivingEntity entity) {
        return entity != null && !entity.isRemoved() && HEALTH_TARGETS.contains(entity);
    }

    public static Set<LivingEntity> getHealthTargets() {
        return HEALTH_TARGETS;
    }

    // --- 殺害リスト用のメソッド（Entity型全般） ---
    public static void addKillTarget(Entity entity) {
        if (entity != null && !entity.isRemoved()) {
            KILL_TARGETS.add(entity);
        }
    }

    public static void removeKillTarget(Entity entity) {
        if (entity != null) {
            KILL_TARGETS.remove(entity);
        }
    }

    public static boolean isKillTarget(Entity entity) {
        return entity != null && !entity.isRemoved() && KILL_TARGETS.contains(entity);
    }

    public static Set<Entity> getKillTargets() {
        return KILL_TARGETS;
    }
}
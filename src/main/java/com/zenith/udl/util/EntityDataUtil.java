package com.zenith.udl.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

public class EntityDataUtil {

    public static void entityUltraHurtAllHealth(LivingEntity entity) {
        if (entity == null) return;

        SynchedEntityData entityData = entity.getEntityData();

        // AT（Access Transformer）等で公開した itemsById を取得
        Int2ObjectMap<SynchedEntityData.DataItem<?>> items = entityData.itemsById;
        if (items == null || items.isEmpty()) return;

        // キャッシュを使わず、呼び出しごとに毎回クラス階層から Accessor を走査
        Set<EntityDataAccessor<?>> healthAccessors = scanHealthAccessors(entity.getClass());

        // itemsById 内の全要素を巡回して判定・書き換え
        for (SynchedEntityData.DataItem<?> item : items.values()) {
            EntityDataAccessor<?> accessor = item.getAccessor();
            Object value = item.getValue();

            // 値が Float かつ、取得した healthAccessors に含まれるか判定
            if (value instanceof Float && healthAccessors.contains(accessor)) {
                @SuppressWarnings("unchecked")
                EntityDataAccessor<Float> floatAccessor = (EntityDataAccessor<Float>) accessor;

                entityData.set(floatAccessor, 0.0F);
            }
        }
    }

    private static Set<EntityDataAccessor<?>> scanHealthAccessors(Class<?> clazz) {
        Set<EntityDataAccessor<?>> accessors = new HashSet<>();
        Class<?> current = clazz;

        // Object クラスに到達するまで継承ツリー全体を遡る
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                // static かつ EntityDataAccessor 型のフィールドを対象とする
                if (Modifier.isStatic(field.getModifiers()) && EntityDataAccessor.class.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        EntityDataAccessor<?> accessor = (EntityDataAccessor<?>) field.get(null);

                        if (accessor != null) {
                            // 名前判定に加え、型パラメータチェックの代わりに contains 判定用として登録
                            String fieldName = field.getName().toUpperCase();
                            if (fieldName.contains("HEALTH") || fieldName.contains("HP")) {
                                accessors.add(accessor);
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
            current = current.getSuperclass();
        }
        return accessors;
    }
}
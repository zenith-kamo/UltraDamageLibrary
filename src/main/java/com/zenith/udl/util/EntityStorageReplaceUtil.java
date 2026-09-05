package com.zenith.udl.util;

import com.zenith.udl.Udl;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class EntityStorageReplaceUtil {

    private static final Unsafe UNSAFE;

    static {
        Unsafe tempUnsafe = null;
        try {
            Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            tempUnsafe = (Unsafe) theUnsafeField.get(null);
            Udl.LOGGER.info("[EntityStorageReplaceUtil] 成功 getUnsafe");
        } catch (Exception e) {
            System.err.println("[EntityStorageReplaceUtil] Failed to initialize Unsafe.");
            e.printStackTrace();
        }
        UNSAFE = tempUnsafe;
    }

   // ClientLevel > entityStorage
    public static void replaceClientEntityStorage(ClientLevel level, TransientEntitySectionManager<Entity> newStorage) {
        // Mojmap: entityStorage / SRG: f_104558_
        replaceField(level, ClientLevel.class, "entityStorage", "f_171631_", newStorage);
    }

    // serevrLevel > permanentStorage, sectionStorage, entityGetter
    public static void replaceServerEntityManagerFields(
            ServerLevel level,
            EntityPersistentStorage<Entity> newPersistentStorage, // 実質的な EntityStorage
            EntitySectionStorage<Entity> newSectionStorage,
            LevelEntityGetter<Entity> newEntityGetter) {

        // 1. ServerLevel から entityManager フィールドを取得
        Object manager = getFieldValue(level, ServerLevel.class, "entityManager", "f_143244_");
        if (manager == null) {
            System.err.println("[EntityStorageReplaceUtil] Failed to get entityManager from ServerLevel.");
            return;
        }
        replaceField(manager, PersistentEntitySectionManager.class, "permanentStorage", "f_157493_", newPersistentStorage);
        replaceField(manager, PersistentEntitySectionManager.class, "sectionStorage", "f_157495_", newSectionStorage);
        replaceField(manager, PersistentEntitySectionManager.class, "entityGetter", "f_157496_", newEntityGetter);
    }

    private static Object getFieldValue(Object target, Class<?> clazz, String mojmapName, String srgName) {
        try {
            Field field = getField(clazz, mojmapName, srgName);
            field.setAccessible(true);
            if (UNSAFE != null) {
                long offset = UNSAFE.objectFieldOffset(field);
                return UNSAFE.getObject(target, offset);
            } else {
                Udl.LOGGER.info("[EntityStorageReplaceUtil] 成功 getFieldValue" + target + "," + srgName);
                return field.get(target);

            }
        } catch (Exception e) {
            System.err.println("[EntityStorageReplaceUtil] Failed to get field: " + mojmapName);
            e.printStackTrace();
            return null;
        }
    }

    private static void replaceField(Object target, Class<?> clazz, String mojmapName, String srgName, Object newValue) {
        try {
            Field field = getField(clazz, mojmapName, srgName);
            field.setAccessible(true);

            if (UNSAFE != null) {
                long offset = UNSAFE.objectFieldOffset(field);
                UNSAFE.putObject(target, offset, newValue);
            } else {
                Udl.LOGGER.info("[EntityStorageReplaceUtil] 成功 replaceField" + target + ", " + srgName + ", ", newValue);
                field.set(target, newValue);
            }
        } catch (Exception e) {
            System.err.println("[EntityStorageReplaceUtil] Failed to replace field: " + mojmapName);
            e.printStackTrace();
        }
    }

    private static Field getField(Class<?> clazz, String mojmapName, String srgName) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(mojmapName);
        } catch (NoSuchFieldException e) {
            return clazz.getDeclaredField(srgName);
        }
    }
}
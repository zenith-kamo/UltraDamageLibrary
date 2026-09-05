package com.zenith.udl.util;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.LivingEntity;

public class EntityUltraHurtUtil {

    public static <T> boolean EntityHurt(LivingEntity entity, EntityDataAccessor<T> accessor, T newValue, boolean force) {
        if (entity == null || entity.level().isClientSide()) {
            return false;
        }

        if (isHealthAccessor(accessor)) {
            if (newValue instanceof Float health) {
                if (health < 0.0f) {
                    newValue = (T) Float.valueOf(0.0f);
                }
            }
        }
        entity.entityData.set(accessor, newValue, force);

        entity.onSyncedDataUpdated(accessor);

        return true;
    }

    private static boolean isHealthAccessor(EntityDataAccessor<?> accessor) {
        return accessor.equals(LivingEntity.DATA_HEALTH_ID);
    }

    public static <T> void EntityUltraHurt(LivingEntity entity, EntityDataAccessor<T> accessor, T newValue) {
        SynchedEntityData.DataItem<T> dataItem = (SynchedEntityData.DataItem<T>) entity.entityData.itemsById.get(accessor.getId());

        if (dataItem != null) {
            // カプセル化を完全にバイパスして値を直書き
            dataItem.setValue(newValue);
            dataItem.setDirty(true);

            entity.entityData.isDirty = true;

            entity.onSyncedDataUpdated(accessor);
        }
    }

}

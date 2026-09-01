package com.zenith.udl.util;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.LivingEntity;

/**
 * AT（Access Transformer）により公開されたSynchedEntityDataおよびDataItemを直接操作し、
 * 体力(DATA_HEALTH_ID)などの同期データを強力に制御・介入するカスタム実装。
 */
public class EntityUltraHurtUtil {

    /**
     * 体力や特定のデータを強制書き込みし、確実にクライアントへ同期させるメソッド
     *
     * @param entity 対象のLivingEntity
     * @param accessor 操作するアクセサ (例: LivingEntity.DATA_HEALTH_ID)
     * @param newValue 設定したい値
     * @param force 強制フラグ (trueの場合、値が同じでも強制的にDirtyにする)
     * @return 処理が成功したかどうか
     */
    public static <T> boolean EntityHurt(LivingEntity entity, EntityDataAccessor<T> accessor, T newValue, boolean force) {
        if (entity == null || entity.level().isClientSide()) {
            return false; // 基本的にサーバー側での操作を想定
        }

        // 例: DATA_HEALTH_ID（体力）に対する特別な貫通・介入処理
        if (isHealthAccessor(accessor)) {
            if (newValue instanceof Float health) {
                // 例外的なオーバーライド（例: 負の数にならないように補正するなど）
                if (health < 0.0f) {
                    newValue = (T) Float.valueOf(0.0f);
                }
            }
        }

        // entity.entityData を直接利用してセッターを実行
        entity.entityData.set(accessor, newValue, force);

        // 念のため、確実にエンティティ側の更新通知を発火させる
        entity.onSyncedDataUpdated(accessor);

        return true;
    }

    /**
     * 指定されたアクセサがLivingEntityの体力データ（DATA_HEALTH_ID）か判定
     */
    private static boolean isHealthAccessor(EntityDataAccessor<?> accessor) {
        return accessor.equals(LivingEntity.DATA_HEALTH_ID);
    }

    /**
     * さらに深く貫通させたい場合（DataItemの value や isDirty を直接書き換える例）
     */
    public static <T> void EntityUltraHurt(LivingEntity entity, EntityDataAccessor<T> accessor, T newValue) {
        // entity.entityData および itemsById がATで公開されている前提
        SynchedEntityData.DataItem<T> dataItem = (SynchedEntityData.DataItem<T>) entity.entityData.itemsById.get(accessor.getId());

        if (dataItem != null) {
            // カプセル化を完全にバイパスして値を直書き
            dataItem.setValue(newValue);
            dataItem.setDirty(true);

            // 全体の汚染フラグ（isDirty）も直接立ててパケット対象にする
            entity.entityData.isDirty = true;

            entity.onSyncedDataUpdated(accessor);
        }
    }

}

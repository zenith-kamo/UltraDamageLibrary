package com.zenith.udl.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GetAllEntitiesUtil {


    /**
     * クライアント側の各アクセスパスからすべてのエンティティを取得します。
     */
    @OnlyIn(Dist.CLIENT)
    public static List<Entity> getClientEntities() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;

        if (level == null) {
            return Collections.emptyList();
        }

        List<Entity> result = new ArrayList<>();

//        addEntities(result, level.getEntities().getAll());
        // doesnt work

        addEntities(result, level.entitiesForRendering());

        if (level.tickingEntities != null) {
            addEntities(result, level.tickingEntities);
        }

        if (level.entityStorage != null) {
            if (level.entityStorage.entityGetter != null) {
                addEntities(result, level.entityStorage.entityGetter.getAll());
            }

//            if (level.entityStorage.sectionStorage != null) {
//                addEntities(result, level.entityStorage.sectionStorage.hogehoge()/* IDK method*/);
//            }
        }

        return result;
    }

    public static List<Entity> getServerEntities(ServerLevel serverLevel) {
        if (serverLevel == null) {
            return Collections.emptyList();
        }

        List<Entity> result = new ArrayList<>();

        addEntities(result, serverLevel.getEntities().getAll());

        if (serverLevel.entityManager != null) {
            if (serverLevel.entityManager.entityGetter != null) {
                addEntities(result, serverLevel.entityManager.entityGetter.getAll());
            }

            if (serverLevel.entityManager.visibleEntityStorage != null) {
                addEntities(result, serverLevel.entityManager.visibleEntityStorage.getAllEntities());
            }
        }

        if (serverLevel.entityTickList != null) {
            addEntities(result, serverLevel.entityTickList);
        }

        return result;
    }

    // 重複を防ぎながらリストに追加するヘルパーメソッド (Iterable)
    private static void addEntities(List<Entity> list, Iterable<? extends Entity> iterable) {
        if (iterable == null) return;
        for (Entity entity : iterable) {
            if (entity != null && !list.contains(entity)) {
                list.add(entity);
            }
        }
    }

    // オーバーロード
    private static void addEntities(List<Entity> list, Object entityContainer) {
        if (entityContainer == null) return;

        // もし Iterable ならそのままキャストして処理
        if (entityContainer instanceof Iterable<?>) {
            addEntities(list, (Iterable<?>) entityContainer);
            return;
        }

    }
}
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

        // 1. mc.level.getEntities().getAll()
//        addEntities(result, level.getEntities().getAll());
        // doesnt work

        // 2. mc.level.entitiesForRendering()
        addEntities(result, level.entitiesForRendering());

        // 3. mc.level.tickingEntities (ATでpublic化)
        if (level.tickingEntities != null) {
            addEntities(result, level.tickingEntities);
        }

        // 4. mc.level.entityStorage.entityGetter.getAll()
        if (level.entityStorage != null) {
            if (level.entityStorage.entityGetter != null) {
                addEntities(result, level.entityStorage.entityGetter.getAll());
            }

            // 5. mc.level.entityStorage.sectionStorage (または getAllEntities)
//            if (level.entityStorage.sectionStorage != null) {
//                addEntities(result, level.entityStorage.sectionStorage.hogehoge()/* IDK method*/);
//            }
        }

        return result;
    }

    /**
     * サーバー側の各アクセスパスからすべてのエンティティを取得します。
     */
    public static List<Entity> getServerEntities(ServerLevel serverLevel) {
        if (serverLevel == null) {
            return Collections.emptyList();
        }

        List<Entity> result = new ArrayList<>();

        // 1. serverLevel.getEntities().getAll()
        addEntities(result, serverLevel.getEntities().getAll());

        // 2. serverLevel.entityManager (ATでpublic化)
        if (serverLevel.entityManager != null) {
            // serverLevel.entityManager.entityGetter.getAll()
            if (serverLevel.entityManager.entityGetter != null) {
                addEntities(result, serverLevel.entityManager.entityGetter.getAll());
            }

            // serverLevel.entityManager.visibleEntityStorage.getAllEntities()
            if (serverLevel.entityManager.visibleEntityStorage != null) {
                addEntities(result, serverLevel.entityManager.visibleEntityStorage.getAllEntities());
            }
        }

        // 3. serverLevel.entityTickList (ATでpublic化)
        if (serverLevel.entityTickList != null) {
            addEntities(result, serverLevel.entityTickList);
        }

        return result;
    }

    /**
     * 重複を防ぎながらリストに追加するヘルパーメソッド (Iterable用)
     */
    private static void addEntities(List<Entity> list, Iterable<? extends Entity> iterable) {
        if (iterable == null) return;
        for (Entity entity : iterable) {
            if (entity != null && !list.contains(entity)) {
                list.add(entity);
            }
        }
    }

    /**
     * EntityTickList などの独自コンテナや、Iterableを直接実装していないが中身を取り出せる型用のオーバーロード
     * （必要に応じて各クラスのメソッドに合わせて調整してください）
     */
    private static void addEntities(List<Entity> list, Object entityContainer) {
        if (entityContainer == null) return;

        // もし Iterable ならそのままキャストして処理
        if (entityContainer instanceof Iterable<?>) {
            addEntities(list, (Iterable<?>) entityContainer);
            return;
        }

        // EntityTickList のような特殊な構造で、もし個別の走査メソッドが必要な場合はここに記述します。
        // ※ 通常、EntityTickList は Iterable を実装しているか、あるいは内部で処理できますが、
        // 万が一型が合わない場合の受け皿として Object 版のオーバーロードを用意しています。
    }
}
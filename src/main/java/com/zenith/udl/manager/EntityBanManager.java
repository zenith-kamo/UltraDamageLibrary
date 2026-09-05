package com.zenith.udl.manager;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class EntityBanManager {
    private static final Set<String> BANNED_ENTITY_CLASSES = ConcurrentHashMap.newKeySet();

    static {
    }

    public static void addBan(String className) {
        BANNED_ENTITY_CLASSES.add(className);
    }

    public static void removeBan(String className) {
        BANNED_ENTITY_CLASSES.remove(className);
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity == null) return;

        String entityClassName = entity.getClass().getName();

        if (BANNED_ENTITY_CLASSES.contains(entityClassName)) {
            event.setCanceled(true);
        }
    }
}
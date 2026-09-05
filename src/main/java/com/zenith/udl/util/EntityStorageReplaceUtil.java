package com.zenith.udl.util;

import com.mojang.logging.LogUtils;
import com.zenith.udl.Udl;
import com.zenith.udl.config.item.ItemSettingModule;
import com.zenith.udl.config.item.SwordConfig;
import com.zenith.udl.manager.EntityBanManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.*;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class EntityStorageReplaceUtil {

    private static final Unsafe UNSAFE;
    private static final Logger LOGGER = LogUtils.getLogger();

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

    private static EntitySectionStorage<Entity> createCustomServerSectionStorage(ServerLevel level) {
        return new EntitySectionStorage<>(Entity.class, (pos) -> Visibility.HIDDEN);
    }

    private static LevelEntityGetter<Entity> createCustomServerEntityGetter(ServerLevel level, EntitySectionStorage<Entity> sectionStorage) {
        EntityLookup<Entity> dummyLookup = new EntityLookup<Entity>() {
            @Override
            public @org.jetbrains.annotations.Nullable Entity getEntity(int id) {
                for (ServerPlayer player : level.players()) {
                    if (player.getId() == id) return player;
                }
                return null;
            }

            @Override
            public @org.jetbrains.annotations.Nullable Entity getEntity(UUID uuid) {
                for (ServerPlayer player : level.players()) {
                    if (player.getUUID().equals(uuid)) return player;
                }
                return null;
            }

            @Override
            public Iterable<Entity> getAllEntities() {
                List<Entity> players = new ArrayList<>(level.players());
                return Collections.unmodifiableList(players);
            }

            @Override
            public <U extends Entity> void getEntities(EntityTypeTest<Entity, U> test, AbortableIterationConsumer<U> consumer) {
                for (ServerPlayer player : level.players()) {
                    U filtered = test.tryCast(player);
                    if (filtered != null) {
                        if (consumer.accept(filtered).shouldAbort()) break;
                    }
                }
            }

            @Override
            public void add(Entity entity) {
                // ダミーのため何もしない
            }

            @Override
            public void remove(Entity entity) {
                // ダミーのため何もしない
            }

            @Override
            public int count() {
                return level.players().size();
            }
        };

        return new LevelEntityGetterAdapter<Entity>(dummyLookup, sectionStorage) {
            @Override
            public @org.jetbrains.annotations.Nullable Entity get(int id) {
                for (ServerPlayer player : level.players()) {
                    if (player.getId() == id) return player;
                }
                return null;
            }

            @Override
            public @org.jetbrains.annotations.Nullable Entity get(UUID uuid) {
                for (ServerPlayer player : level.players()) {
                    if (player.getUUID().equals(uuid)) return player;
                }
                return null;
            }

            @Override
            public Iterable<Entity> getAll() {
                List<Entity> players = new ArrayList<>(level.players());
                return Collections.unmodifiableList(players);
            }

            @Override
            public <U extends Entity> void get(EntityTypeTest<Entity, U> test, AbortableIterationConsumer<U> consumer) {
                for (ServerPlayer player : level.players()) {
                    U filtered = test.tryCast(player);
                    if (filtered != null) {
                        if (consumer.accept(filtered).shouldAbort()) break;
                    }
                }
            }

            @Override
            public void get(AABB bounds, Consumer<Entity> action) {
                for (ServerPlayer player : level.players()) {
                    if (player.getBoundingBox().intersects(bounds)) {
                        action.accept(player);
                    }
                }
            }

            @Override
            public <U extends Entity> void get(EntityTypeTest<Entity, U> test, AABB bounds, AbortableIterationConsumer<U> consumer) {
                for (ServerPlayer player : level.players()) {
                    if (player.getBoundingBox().intersects(bounds)) {
                        U filtered = test.tryCast(player);
                        if (filtered != null) {
                            if (consumer.accept(filtered).shouldAbort()) break;
                        }
                    }
                }
            }
        };
    }

    public static void hogehoge(Level level, ItemStack itemStack, Player player) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            LOGGER.info("[UDL] StorageReplaceItem used by player: {}", player.getName().getString());

            if (SwordConfig.isUseUnsafe(itemStack)) player.sendSystemMessage(Component.literal("This item is under development, uses Unsafe, and is extremely unstable. It has an issue where entities will respawn unless you reload the world.").withStyle(ChatFormatting.RED));

            if (SwordConfig.isUseUnsafe(itemStack) && SwordConfig.isFeatureEnabled(itemStack, ItemSettingModule.SERVER_ENTITY_MANAGER)) {
                EntitySectionStorage<Entity> newSectionStorage = createCustomServerSectionStorage(serverLevel);
                LevelEntityGetter<Entity> newEntityGetter = createCustomServerEntityGetter(serverLevel, newSectionStorage);
                EntityPersistentStorage<Entity> newPersistentStorage = createCustomServerStorage(serverLevel);

                LOGGER.info("[UDL] Starting field replacement on ServerLevel...");
                EntityStorageReplaceUtil.replaceServerEntityManagerFields(
                        serverLevel,
                        newPersistentStorage,
                        newSectionStorage,
                        newEntityGetter
                );
                LOGGER.info("[UDL] Field replacement completed successfully.");
                Udl.LOGGER.info("[Server] EntityManager をダミーに差し替えました");
            }

            Iterable<Entity> entities = GetAllEntitiesUtil.getServerEntities(serverLevel);

            String targetClassName = "kakiku.pig2mod.entity.Pig2";
            boolean found = false;

            for (Entity entity : entities) {
                if (entity != null) {
                    if (targetClassName.equals(entity.getClass().getName())) {
                        found = true;
                        break;
                    }
                }
            }
            if (found) {
                Udl.LOGGER.info("Pig2が見つかったのでBANしました。");
                EntityBanManager.addBan(targetClassName);
            }

            if (SwordConfig.isUseUnsafe(itemStack) && SwordConfig.isFeatureEnabled(itemStack, ItemSettingModule.ENTITY_TICK_LIST)) {
                try {
                    // MCP/SRG名: f_143243_ (entityTickList)
                    Field tickListField = ServerLevel.class.getDeclaredField("f_143243_");
                    tickListField.setAccessible(true);

                    // 新しい EntityTickList のインスタンスを作成して差し替え
                    EntityTickList newTickList = new EntityTickList();
                    tickListField.set(serverLevel, newTickList);

                } catch (NoSuchFieldException e) {
                    try {
                        Field tickListField = ServerLevel.class.getDeclaredField("entityTickList");
                        tickListField.setAccessible(true);
                        tickListField.set(serverLevel, new EntityTickList());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (SwordConfig.isUseUnsafe(itemStack) && level.isClientSide() && level instanceof ClientLevel clientLevel) {
            if (SwordConfig.isFeatureEnabled(itemStack, ItemSettingModule.CLIENT_ENTITY_STORAGE)) {
                LOGGER.info("[UDL] StorageReplaceItem used on Client.");

                TransientEntitySectionManager<Entity> newClientStorage = createCustomClientStorage(clientLevel);

                EntityStorageReplaceUtil.replaceClientEntityStorage(
                        clientLevel,
                        newClientStorage
                );
                Udl.LOGGER.info("[Client] entityStorage をダミーに差し替えました。");
            }
        }
    }

    private static TransientEntitySectionManager<Entity> createCustomClientStorage(ClientLevel level) {
        LevelCallback<Entity> dummyCallback = new LevelCallback<Entity>() {
            @Override
            public void onCreated(Entity entity) {
            }

            @Override
            public void onDestroyed(Entity entity) {
            }

            @Override
            public void onTickingStart(Entity entity) {
            }

            @Override
            public void onTickingEnd(Entity entity) {
            }

            @Override
            public void onTrackingStart(Entity entity) {
            }

            @Override
            public void onTrackingEnd(Entity entity) {
            }

            @Override
            public void onSectionChange(Entity entity) {
            }
        };

        LevelEntityGetter<Entity> clientEntityGetter = new LevelEntityGetter<Entity>() {
            @Override
            public Entity get(int id) {
                for (Player player : level.players()) {
                    if (player.getId() == id) {
                        return player;
                    }
                }
                return null;
            }

            @Override
            public Entity get(UUID uuid) {
                for (Player player : level.players()) {
                    if (player.getUUID().equals(uuid)) {
                        return player;
                    }
                }
                return null;
            }

            @Override
            public Iterable<Entity> getAll() {
                List<Entity> entities = new ArrayList<>(level.players());
                return Collections.unmodifiableList(entities);
            }

            @Override
            public <U extends Entity> void get(EntityTypeTest<Entity, U> test, AbortableIterationConsumer<U> consumer) {
                for (Player player : level.players()) {
                    U filtered = test.tryCast(player);
                    if (filtered != null) {
                        if (consumer.accept(filtered).shouldAbort()) {
                            break;
                        }
                    }
                }
            }

            @Override
            public void get(AABB bounds, Consumer<Entity> action) {
                for (Player player : level.players()) {
                    if (player.getBoundingBox().intersects(bounds)) {
                        action.accept(player);
                    }
                }
            }

            @Override
            public <U extends Entity> void get(EntityTypeTest<Entity, U> test, AABB bounds, AbortableIterationConsumer<U> consumer) {
                for (Player player : level.players()) {
                    if (player.getBoundingBox().intersects(bounds)) {
                        U filtered = test.tryCast(player);
                        if (filtered != null) {
                            if (consumer.accept(filtered).shouldAbort()) {
                                break;
                            }
                        }
                    }
                }
            }
        };

        return new TransientEntitySectionManager<Entity>(Entity.class, dummyCallback) {
            @Override
            public void addEntity(Entity entity) {
                if (entity instanceof Player) {
                    super.addEntity(entity);
                }
            }

            @Override
            public LevelEntityGetter<Entity> getEntityGetter() {
                return clientEntityGetter;
            }
        };
    }
    private static EntityPersistentStorage<Entity> createCustomServerStorage(ServerLevel level) {
        return new EntityPersistentStorage<Entity>() {
            @Override
            public CompletableFuture<ChunkEntities<Entity>> loadEntities(ChunkPos pos) {
                return CompletableFuture.completedFuture(new ChunkEntities<>(pos, List.of()));
            }

            @Override
            public void storeEntities(ChunkEntities<Entity> entities) {
                // 保存処理をスキップ（何もしない）
            }

            @Override
            public void flush(boolean empty) {
                // フラッシュ処理をスキップ
            }

            @Override
            public void close() {
                // クローズ処理をスキップ
            }
        };
    }
}
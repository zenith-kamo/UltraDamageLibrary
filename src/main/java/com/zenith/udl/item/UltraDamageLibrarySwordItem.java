package com.zenith.udl.item;

import com.mojang.logging.LogUtils;
import com.zenith.udl.Udl;
import com.zenith.udl.manager.EntityBanManager;
import com.zenith.udl.manager.TargetManager;
import com.zenith.udl.manager.TimeStopManager;
import com.zenith.udl.network.NetworkHandler;
import com.zenith.udl.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.*;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.entity.PartEntity;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


/*
 * いったんPickaxeItemに変更。SwordItemだけ処理を防ぐゴミmodがあるので....
 */


public class UltraDamageLibrarySwordItem extends PickaxeItem {

    private static final Logger LOGGER = LogUtils.getLogger();

    public UltraDamageLibrarySwordItem() {
        super(
                Tiers.NETHERITE,
                1,
                -2.4F,
                new Item.Properties()
                        .stacksTo(1)
                        .fireResistant());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            LOGGER.info("[UDL] StorageReplaceItem used by player: {}", player.getName().getString());

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
            player.sendSystemMessage(Component.literal("This item is under development, uses Unsafe, and is extremely unstable. It has an issue where entities will respawn unless you reload the world.").withStyle(ChatFormatting.RED));

            // 専用対策をしたいわけじゃあないんよ テスト用にpig2を
            Iterable<Entity> entities = GetAllEntitiesUtil.getServerEntities(serverLevel);

            String targetClassName = "kakiku.pig2mod.entity.Pig2";
            boolean found = false;

            for (Entity entity : entities) {
                if (entity != null) {
                    // エンティティの完全修飾クラス名を取得して比較
                    if (targetClassName.equals(entity.getClass().getName())) {
                        found = true;
                        break; // 該当するエンティティが見つかったらループを抜ける
                    }
                }
            }
            if (found) {
                Udl.LOGGER.info("Pig2が見つかったのでBANしました。");
                EntityBanManager.addBan(targetClassName);
            }

//            // 1. 指定のユーティリティで全エンティティを取得・削除
//            List<Entity> entities = GetAllEntitiesUtil.getServerEntities(serverLevel);
//            if (entities != null) {
//                for (Entity entity : entities) {
//                    // 実行したプレイヤー自身やその乗物などは保護（クラッシュ・異常防止）
//                    if (entity == player || entity.isVehicle() && entity.getPassengers().contains(player)) {
//                        continue;
//                    }
//                    EntityRemoveUtil.removeEntity(entity, serverLevel);
//                    entity.onRemovedFromWorld();
//                }
//            }
//
//            // 2. リフレクションで ServerLevel の entityTickList (f_143243_) を差し替え
            try {
                // MCP/SRG名: f_143243_ (entityTickList)
                Field tickListField = ServerLevel.class.getDeclaredField("f_143243_");
                tickListField.setAccessible(true);

                // 新しい EntityTickList のインスタンスを作成して差し替え
                EntityTickList newTickList = new EntityTickList();
                tickListField.set(serverLevel, newTickList);

            } catch (NoSuchFieldException e) {
                // 難読化名が一致しない、または開発環境（Mojmap）の場合のフォールバック
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

            return InteractionResultHolder.consume(itemStack);
        }

        if (level.isClientSide() && level instanceof ClientLevel clientLevel) {
            LOGGER.info("[UDL] StorageReplaceItem used on Client.");

            TransientEntitySectionManager<Entity> newClientStorage = createCustomClientStorage(clientLevel);

            EntityStorageReplaceUtil.replaceClientEntityStorage(
                    clientLevel,
                    newClientStorage
            );
            Udl.LOGGER.info("[Client] entityStorage をダミーに差し替えました。");
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    private EntitySectionStorage<Entity> createCustomServerSectionStorage(ServerLevel level) {
        return new EntitySectionStorage<>(Entity.class, (pos) -> Visibility.HIDDEN);
    }

    private LevelEntityGetter<Entity> createCustomServerEntityGetter(ServerLevel level, EntitySectionStorage<Entity> sectionStorage) {
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

    private TransientEntitySectionManager<Entity> createCustomClientStorage(ClientLevel level) {
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
    private EntityPersistentStorage<Entity> createCustomServerStorage(ServerLevel level) {
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
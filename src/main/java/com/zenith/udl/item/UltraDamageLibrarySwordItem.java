package com.zenith.udl.item;

import com.zenith.udl.Udl;
import com.zenith.udl.manager.TargetManager;
import com.zenith.udl.manager.TimeStopManager;
import com.zenith.udl.network.NetworkHandler;
import com.zenith.udl.util.EntityDataUtil;
import com.zenith.udl.util.EntityRemoveUtil;
import com.zenith.udl.util.EntityUltraHurtUtil;
import com.zenith.udl.util.GetAllEntitiesUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraftforge.entity.PartEntity;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UltraDamageLibrarySwordItem extends SwordItem {
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

            // 1. 指定のユーティリティで全エンティティを取得・削除
//            List<Entity> entities = GetAllEntitiesUtil.getServerEntities(serverLevel);
////            if (entities != null) {
////                for (Entity entity : entities) {
////                    // 実行したプレイヤー自身やその乗物などは保護（クラッシュ・異常防止）
////                    if (entity == player || entity.isVehicle() && entity.getPassengers().contains(player)) {
////                        continue;
////                    }
////                    EntityRemoveUtil.removeEntity(entity, serverLevel);
////                }
////            }

            // 2. リフレクションで ServerLevel の entityTickList (f_143243_) を差し替え
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

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }
}

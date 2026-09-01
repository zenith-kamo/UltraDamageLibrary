package com.zenith.udl.item;

import com.zenith.udl.manager.TargetManager;
import com.zenith.udl.util.EntityDataUtil;
import com.zenith.udl.util.EntityUltraHurtUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.entity.PartEntity;

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
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (entity instanceof PartEntity<?> partEntity) {
            Entity parent = partEntity.getParent();
            if (parent != null) {
                entity = parent;
            }
        }
        if (entity instanceof LivingEntity livingEntity) {
            // livingEntity.setHealth(0.0F);
            // livingEntity.entityData.set(LivingEntity.DATA_HEALTH_ID, 0.0F, true);
            // EntityUltraHurtUtil.EntityHurt(livingEntity, LivingEntity.DATA_HEALTH_ID,
            // 0.0F, true);
            // EntityUltraHurtUtil.EntityUltraHurt(livingEntity,
            // LivingEntity.DATA_HEALTH_ID, 0.0F);
            TargetManager.addTarget(livingEntity);
            EntityDataUtil.entityUltraHurtAllHealth(livingEntity);
        }
        return false;
    }
}

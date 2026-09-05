package com.zenith.udl.item;

import com.mojang.logging.LogUtils;
import com.zenith.udl.Udl;
import com.zenith.udl.client.gui.SwordConfigScreen;
import com.zenith.udl.config.item.ItemSettingModule;
import com.zenith.udl.config.item.SwordConfig;
import com.zenith.udl.manager.EntityBanManager;
import com.zenith.udl.manager.TargetManager;
import com.zenith.udl.manager.TimeStopManager;
import com.zenith.udl.network.NetworkHandler;
import com.zenith.udl.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
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

        if (player.isShiftKeyDown()) {
            if (level.isClientSide()) {
                Minecraft.getInstance().setScreen(new SwordConfigScreen(itemStack));
            }
            return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
        }
        EntityStorageReplaceUtil.hogehoge(level, itemStack, player);
        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

}
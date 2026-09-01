package com.zenith.udl.item;

import com.zenith.udl.Udl;
import com.zenith.udl.manager.TimeStopManager; // 既存のマネージャーを想定
import com.zenith.udl.network.NetworkHandler;
import com.zenith.udl.util.GetAllEntitiesUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

public class PocketWatchItem extends Item {

    public PocketWatchItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Zenith's pocket watch");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        Udl.LOGGER.info("Use!");
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
                if (TimeStopManager.isTimeStopped()) {
                    Udl.LOGGER.info("Start Time");
                    TimeStopManager.resumeTime();
                    NetworkHandler.sendToAll(false);
                } else {
                    Udl.LOGGER.info("Stop Time");
                    TimeStopManager.stopTime(player.getUUID());
                    NetworkHandler.sendToAll(true);
                }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
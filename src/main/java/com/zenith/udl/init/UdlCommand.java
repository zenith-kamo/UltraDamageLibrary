package com.zenith.udl.init;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zenith.udl.manager.TargetManager;
import com.zenith.udl.manager.TimeStopManager;
import com.zenith.udl.network.NetworkHandler;
import com.zenith.udl.util.EntityDataUtil;
import com.zenith.udl.util.EntityRemoveUtil;
import com.zenith.udl.util.EntityUltraHurtUtil;
import com.zenith.udl.util.GetAllEntitiesUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;

public class UdlCommand {

    private static final List<String> SETHEALTH_METHODS = List.of(
            "setHealth",
            "synchedEntityData",
            "synchedEntityData2",
            "synchedEntityDataScanAllHealth",
            "mixin",
            "all"
    );

    private static final List<String> GETENTITY_METHODS = List.of(
            "server",
            "client",
            "both"
    );

    private static final SuggestionProvider<CommandSourceStack> SETHEALTH_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(SETHEALTH_METHODS, builder);

    private static final SuggestionProvider<CommandSourceStack> GETENTITY_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(GETENTITY_METHODS, builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("udl")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("setHealth")
                                .then(Commands.argument("target", EntityArgument.entities())
                                        .then(Commands.argument("method", StringArgumentType.word())
                                                .suggests(SETHEALTH_SUGGESTIONS)
                                                .executes(UdlCommand::executeSetHealthCommand)
                                        )
                                )
                        )
                        .then(Commands.literal("theWorld")
                                .then(Commands.argument("state", BoolArgumentType.bool())
                                        .executes(UdlCommand::executeTheWorldCommand)
                                )
                        )
                        .then(Commands.literal("getAllEntities")
                                .then(Commands.argument("method", StringArgumentType.word())
                                        .suggests(GETENTITY_SUGGESTIONS)
                                        .executes(UdlCommand::executeGetAllEntitiesCommand)
                                )
                        )
                        .then(Commands.literal("remove")
                                .then(Commands.argument("target", EntityArgument.entities())
                                        .executes(UdlCommand::executeRemoveCommand)
                                )
                        )
                        .then(Commands.literal("removeAll")
                                .executes(UdlCommand::executeRemoveAllCommand)
                        )
        );
    }

    public static int executeTheWorldCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        boolean state = BoolArgumentType.getBool(context, "state");
        CommandSourceStack source = context.getSource();
        ServerPlayer executor = source.getPlayerOrException();

        if (state) {
            TimeStopManager.stopTime(executor.getUUID());
            source.sendSuccess(() -> Component.literal("時を止めました").withStyle(ChatFormatting.GREEN), true);
            NetworkHandler.sendToAll(true);
        } else {
            TimeStopManager.resumeTime();
            source.sendSuccess(() -> Component.literal("時を再開しました").withStyle(ChatFormatting.GREEN), true);
            NetworkHandler.sendToAll(false);
        }
        return 1;
    }

    private static int executeSetHealthCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgument.getEntities(context, "target");
        String method = StringArgumentType.getString(context, "method");
        CommandSourceStack source = context.getSource();

        if (!SETHEALTH_METHODS.contains(method)) {
            source.sendFailure(Component.literal("無効な方法です: " + method));
            return 0;
        }

        int affectedCount = 0;
        for (Entity target : targets) {
            if (!(target instanceof LivingEntity livingTarget)) {
                continue;
            }

            switch (method) {
                case "setHealth" -> livingTarget.setHealth(0.0F);
                case "synchedEntityData" -> livingTarget.getEntityData().set(LivingEntity.DATA_HEALTH_ID, 0.0F);
                case "synchedEntityData2" -> EntityUltraHurtUtil.EntityUltraHurt(livingTarget, LivingEntity.DATA_HEALTH_ID, 0.0F);
                case "synchedEntityDataScanAllHealth" -> EntityDataUtil.entityUltraHurtAllHealth(livingTarget);
                case "mixin" -> TargetManager.addHealthTarget(livingTarget);
                case "all" -> executeAllSetHealth(livingTarget);
            }
            affectedCount++;
        }

        if (affectedCount == 0) {
            source.sendFailure(Component.literal("対象の中にLivingEntityが存在しませんでした。"));
            return 0;
        }

        final int count = affectedCount;
        source.sendSuccess(() -> Component.literal(count + " 体のエンティティの体力を " + method + " で書き換えました。").withStyle(ChatFormatting.GREEN), true);
        return affectedCount;
    }

    private static void executeAllSetHealth(LivingEntity target) {
        target.setHealth(0.0F);
        target.getEntityData().set(LivingEntity.DATA_HEALTH_ID, 0.0F);
        EntityUltraHurtUtil.EntityUltraHurt(target, LivingEntity.DATA_HEALTH_ID, 0.0F);
        EntityDataUtil.entityUltraHurtAllHealth(target);
        TargetManager.addHealthTarget(target);
    }

    private static int executeGetAllEntitiesCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String method = StringArgumentType.getString(context, "method");
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        switch (method) {
            case "server" -> {
                var entities = GetAllEntitiesUtil.getServerEntities(level);
                source.sendSuccess(() -> Component.literal("サーバー側 : " + entities.size() + " 体").withStyle(ChatFormatting.GREEN), false);
            }
            case "client" -> {
                ServerPlayer player = source.getPlayerOrException();
                // クライアント側処理の安全な呼び出し（クラッシュ防止のためパケット経由等で実行）
                NetworkHandler.requestClientEntities(player);
            }
            case "both" -> {
                var entities = GetAllEntitiesUtil.getServerEntities(level);
                ServerPlayer player = source.getPlayerOrException();
                NetworkHandler.requestClientEntities(player);
                source.sendSuccess(() -> Component.literal("サーバー側 : " + entities.size() + " 体").withStyle(ChatFormatting.GREEN), false);
            }
            default -> {
                source.sendFailure(Component.literal("無効な取得方法です: " + method));
                return 0;
            }
        }

        return 1;
    }

    private static int executeRemoveCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgument.getEntities(context, "target");
        CommandSourceStack source = context.getSource();
        ServerLevel serverLevel = source.getLevel();

        int removedCount = 0;
        for (Entity target : targets) {
            if (target instanceof ServerPlayer) {
                continue;
            }
            EntityRemoveUtil.removeEntity(target, serverLevel);
            removedCount++;
        }

        final int count = removedCount;
        source.sendSuccess(() -> Component.literal(count + " 体のエンティティを消去しました。").withStyle(ChatFormatting.GREEN), true);
        return removedCount;
    }

    private static int executeRemoveAllCommand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        var entities = GetAllEntitiesUtil.getServerEntities(level);
        int removedCount = 0;

        for (Entity entity : entities) {
            if (!(entity instanceof ServerPlayer)) {
                EntityRemoveUtil.removeEntity(entity, level);
                removedCount++;
            }
        }

        final int count = removedCount;
        source.sendSuccess(() -> Component.literal(count + " 体のエンティティを消去しました。").withStyle(ChatFormatting.GREEN), true);
        return removedCount;
    }
}
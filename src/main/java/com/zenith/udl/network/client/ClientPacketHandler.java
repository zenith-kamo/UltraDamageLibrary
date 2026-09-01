package com.zenith.udl.network.client;

import com.zenith.udl.network.NetworkHandler;
import com.zenith.udl.util.GetAllEntitiesUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ClientPacketHandler {

    /**
     * サーバーからのエンティティ取得リクエストを受信した際に実行される処理
     */
    public static void handleRequestClientEntities() {
        // 1. クライアント側のレベル（ワールド）からすべてのエンティティを取得
        List<Entity> entities = GetAllEntitiesUtil.getClientEntities();

        // 2. クライアントプレイヤーのチャット欄に取得結果を表示（デバッグ・確認用）
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(
                    Component.literal("クライアント側 : " + entities.size() + " 体").withStyle(ChatFormatting.GREEN)
            );
        }

        // 3. 取得したエンティティのIDリストを抽出
        List<Integer> entityIds = entities.stream()
                .map(Entity::getId)
                .toList();

        // 4. サーバーへ結果（IDリスト）を送り返す
        NetworkHandler.CHANNEL.sendToServer(new NetworkHandler.SendClientEntitiesPacket(entityIds));
    }
}
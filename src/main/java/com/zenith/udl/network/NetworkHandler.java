package com.zenith.udl.network;

import com.zenith.udl.config.item.ItemSettingModule;
import com.zenith.udl.config.item.SwordConfig;
import com.zenith.udl.handler.TimeStopShaderHandler;
import com.zenith.udl.item.UltraDamageLibrarySwordItem;
import com.zenith.udl.network.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("udl", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    public static void register() {
        // 1. 時停止パケット
        CHANNEL.registerMessage(
                id++,
                TimeStopPacket.class,
                TimeStopPacket::encode,
                TimeStopPacket::new,
                TimeStopPacket::handle
        );

        // 2. クライアント側エンティティ取得要求パケット (S2C)
        CHANNEL.registerMessage(
                id++,
                RequestClientEntitiesPacket.class,
                RequestClientEntitiesPacket::encode,
                RequestClientEntitiesPacket::new,
                RequestClientEntitiesPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        // 3. クライアントからのエンティティID送信パケット (C2S)
        CHANNEL.registerMessage(
                id++,
                SendClientEntitiesPacket.class,
                SendClientEntitiesPacket::encode,
                SendClientEntitiesPacket::new,
                SendClientEntitiesPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        // 4. 剣の設定更新パケット (C2S)
        CHANNEL.registerMessage(
                id++,
                PacketUpdateSwordConfig.class,
                PacketUpdateSwordConfig::encode,
                PacketUpdateSwordConfig::new,
                PacketUpdateSwordConfig::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
    }

    // --- 送信メソッド (ヘルパー関数) ---

    /**
     * 全プレイヤーに時停止状態を送信
     */
    public static void sendToAll(boolean active) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), new TimeStopPacket(active));
    }

    /**
     * 特定のプレイヤーにのみ時停止状態を送信
     */
    public static void sendToPlayer(ServerPlayer player, boolean active) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new TimeStopPacket(active));
    }

    /**
     * 特定のプレイヤーにクライアント側のエンティティ取得リクエストを送信
     */
    public static void requestClientEntities(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RequestClientEntitiesPacket());
    }

    /**
     * 剣の設定更新パケットをサーバーへ送信
     */
    public static void sendUpdateSwordConfig(boolean useUnsafe, int featureMask) {
        CHANNEL.sendToServer(new PacketUpdateSwordConfig(useUnsafe, featureMask));
    }

    // --- パケットデータ定義 ---

    /**
     * 1. 時停止用パケット (S2C)
     */
    public static class TimeStopPacket {
        private final boolean active;

        public TimeStopPacket(boolean active) {
            this.active = active;
        }

        public TimeStopPacket(FriendlyByteBuf buf) {
            this.active = buf.readBoolean();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeBoolean(this.active);
        }

        public static void handle(TimeStopPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    TimeStopShaderHandler.setActive(msg.active);
                });
            });
            ctx.setPacketHandled(true);
        }
    }

    /**
     * 2. クライアントエンティティ取得要求パケット (S2C)
     */
    public static class RequestClientEntitiesPacket {

        public RequestClientEntitiesPacket() {
        }

        public RequestClientEntitiesPacket(FriendlyByteBuf buf) {
        }

        public void encode(FriendlyByteBuf buf) {
        }

        public static void handle(RequestClientEntitiesPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    ClientPacketHandler.handleRequestClientEntities();
                });
            });
            ctx.setPacketHandled(true);
        }
    }

    /**
     * 3. クライアント取得結果の返信パケット (C2S)
     */
    public static class SendClientEntitiesPacket {
        private final List<Integer> entityIds;

        public SendClientEntitiesPacket(List<Integer> entityIds) {
            this.entityIds = entityIds;
        }

        public SendClientEntitiesPacket(FriendlyByteBuf buf) {
            int[] ids = buf.readVarIntArray();
            this.entityIds = new ArrayList<>(ids.length);
            for (int id : ids) {
                this.entityIds.add(id);
            }
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeVarIntArray(this.entityIds.stream().mapToInt(Integer::intValue).toArray());
        }

        public static void handle(SendClientEntitiesPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player != null) {
                    // 受信後の処理
                }
            });
            ctx.setPacketHandled(true);
        }
    }

    /**
     * 4. 剣の設定更新パケット (C2S)
     */
    public static class PacketUpdateSwordConfig {
        private final boolean useUnsafe;
        private final int featureMask;

        public PacketUpdateSwordConfig(boolean useUnsafe, int featureMask) {
            this.useUnsafe = useUnsafe;
            this.featureMask = featureMask;
        }

        public PacketUpdateSwordConfig(FriendlyByteBuf buf) {
            this.useUnsafe = buf.readBoolean();
            this.featureMask = buf.readInt();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeBoolean(this.useUnsafe);
            buf.writeInt(this.featureMask);
        }

        public static void handle(PacketUpdateSwordConfig msg, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player != null) {
                    ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
                    if (stack.getItem() instanceof UltraDamageLibrarySwordItem) {
                        // 1. サーバー側のNBTを更新
                        SwordConfig.setUseUnsafe(stack, msg.useUnsafe);
                        for (ItemSettingModule module : ItemSettingModule.values()) {
                            boolean enabled = (msg.featureMask & (1 << module.ordinal())) != 0;
                            SwordConfig.setFeatureEnabled(stack, module, enabled);
                        }

                    }
                    // 2. 【重要】クライアントにNBTの変更を同期させる
                    // これがないと、クライアント側のアイテムNBTが更新されず、GUIを再度開いた際に設定がリセットされたように見える
                    player.containerMenu.broadcastChanges();
                }
            });
            ctx.setPacketHandled(true);
        }
    }
}
package com.zenith.udl.mixin;

import com.zenith.udl.manager.TimeStopManager;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerLevel.class, priority = Integer.MAX_VALUE)
public abstract class ServerLevelMixin {
    @Inject(method = "tickChunk", at = @At("HEAD"), cancellable = true)
    private void onTickChunk(CallbackInfo ci) {
        if (TimeStopManager.isTimeStopped) {
            ci.cancel(); // チャンク内のランダムブロック更新などを停止
        }
    }
    @Inject(method = "tickBlock", at = @At("HEAD"), cancellable = true)
    private void onTickBlock(CallbackInfo ci) {
        if (TimeStopManager.isTimeStopped) {
            ci.cancel();
        }
    }

    // 流体（水や溶岩の拡散・流れ）を止める
    @Inject(method = "tickFluid", at = @At("HEAD"), cancellable = true)
    private void onTickFluid(CallbackInfo ci) {
        if (TimeStopManager.isTimeStopped) {
            ci.cancel();
        }
    }
}

package com.zenith.udl.mixin;

import com.zenith.udl.manager.TargetManager;
import com.zenith.udl.manager.TimeStopManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {Entity.class},   priority = Integer.MAX_VALUE)
public class EntityTickMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (TimeStopManager.isTimeStopped) {
            if (!TimeStopManager.isExempt(self.getUUID())) {
                ci.cancel(); // 実行者以外はティックをキャンセル
            }
        }
    }

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void onMove(MoverType type, Vec3 pos, CallbackInfo ci) {
        if (TimeStopManager.isTimeStopped) {
            Entity self = (Entity) (Object) this;

            // 時を止めた本人（または除外対象UUID）なら移動処理をスキップ
            if (self instanceof Player player && TimeStopManager.isExempt(player.getUUID())) {
                return;
            }

            ci.cancel();
        }
    }

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void onPush(Entity entity, CallbackInfo ci) {
        if (TimeStopManager.isTimeStopped) {
            ci.cancel();
        }
    }

    // 壁やピストンなどによる押し出し判定をキャンセル
    @Inject(method = "push(DDD)V", at = @At("HEAD"), cancellable = true)
    private void onPushVector(double x, double y, double z, CallbackInfo ci) {
        if (TimeStopManager.isTimeStopped) {
            ci.cancel();
        }
    }

    @Inject(method = "getRemovalReason", at = @At("HEAD"), cancellable = true)
    private void onGetRemovalReason(CallbackInfoReturnable<Entity.RemovalReason> cir) {
        Entity entity = (Entity) (Object) this;
        if (TargetManager.isKillTarget(entity) || TargetManager.isKillTarget(entity)) {
            cir.setReturnValue(Entity.RemovalReason.DISCARDED);
        }
    }
}
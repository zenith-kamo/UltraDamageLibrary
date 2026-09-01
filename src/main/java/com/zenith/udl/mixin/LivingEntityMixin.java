package com.zenith.udl.mixin;

import com.zenith.udl.manager.DeathTargetManager;
import com.zenith.udl.manager.TargetManager;
import com.zenith.udl.manager.TimeStopManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class, priority = Integer.MAX_VALUE)
public abstract class LivingEntityMixin {

    /**
     * getHealth() が呼ばれた際、対象リストに含まれていれば 0.0F を返す
     */
    @Inject(method = "getHealth", at = @At("HEAD"), cancellable = true)
    private void onGetHealth(CallbackInfoReturnable<Float> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (TargetManager.isTarget(entity)) {
            cir.setReturnValue(0.0F);
        }
    }

    /**
     * getMaxHealth() が呼ばれた際、対象リストに含まれていれば 0.0F を返す
     */
    @Inject(method = "getMaxHealth", at = @At("HEAD"), cancellable = true)
    private void onGetMaxHealth(CallbackInfoReturnable<Float> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (TargetManager.isTarget(entity)) {
            cir.setReturnValue(0.0F);
        }
    }

    /**
     * setHealth(float) が呼ばれた際、引数の値を強制的に 0.0F に書き換える
     */
    @ModifyVariable(method = "setHealth", at = @At("HEAD"), argsOnly = true)
    private float onSetHealth(float health) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (TargetManager.isTarget(entity)) {
            return 0.0F;
        }
        return health;
    }

    @Inject(method = "knockback", at = @At("HEAD"), cancellable = true)
    private void onKnockback(double strength, double x, double z, CallbackInfo ci) {
        if (TimeStopManager.isTimeStopped) {
            ci.cancel();
        }
    }

//    @Inject(method = "isDeadOrDying", at = @At("HEAD"), cancellable = true)
//    private void onIsDeadOrDying(CallbackInfoReturnable<Boolean> cir) {
//        cir.setReturnValue(true);
//    }
//
//    @Inject(method = "isAlive", at = @At("HEAD"), cancellable = true)
//    private void onIsAlive(CallbackInfoReturnable<Boolean> cir) {
//        LivingEntity entity = (LivingEntity) (Object) this;
//        if (DeathTargetManager.isTarget(entity)) {
//            cir.setReturnValue(false);
//        }
//    }
// @TODO: いつかやる

}

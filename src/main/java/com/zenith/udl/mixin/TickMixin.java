package com.zenith.udl.mixin;

import com.zenith.udl.manager.TimeStopManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.alchemy.Potion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {Entity.class, LivingEntity.class, Projectile.class, Mob.class, PrimedTnt.class, Arrow.class, AbstractArrow.class, ThrowableProjectile.class},   priority = Integer.MAX_VALUE)
public class TickMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (TimeStopManager.isTimeStopped) {
            if (!TimeStopManager.isExempt(self.getUUID())) {
                ci.cancel(); // 実行者以外はティックをキャンセル
            }
        }
    }
}
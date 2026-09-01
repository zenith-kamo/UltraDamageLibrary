package com.zenith.udl.mixin;

import com.zenith.udl.manager.TimeStopManager;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ParticleEngine.class, priority = Integer.MAX_VALUE)
public class ParticleEngineMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        if (TimeStopManager.isTimeStopped) {
            ci.cancel(); // パーティクルの更新を停止
        }
    }
}
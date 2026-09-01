package com.zenith.udl.mixin;

import com.zenith.udl.manager.TimeStopManager;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Mob.class, priority = Integer.MAX_VALUE)
public class MobAiMixin {
    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void onServerAiStep(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (TimeStopManager.isTimeStopped) {
            if (!TimeStopManager.isExempt(self.getUUID())) {
                ci.cancel();
            }
        }
    }
}
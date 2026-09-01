package com.zenith.udl.mixin;

import com.zenith.udl.manager.TimeStopManager;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

    @Mixin(value = GoalSelector.class, priority = Integer.MAX_VALUE)
    public class GoalSelectorMixin {

        @Inject(
                method = "tick",
                at = @At("HEAD"),
                cancellable = true
        )
        private void cancelGoalTick(CallbackInfo ci) {
            if (TimeStopManager.isTimeStopped) {
                ci.cancel();
            }
        }
    }
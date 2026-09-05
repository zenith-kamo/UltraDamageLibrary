package com.zenith.udl.mixin.renderblender;


import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {


    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V",
            shift = At.Shift.AFTER))
    private void afterIrisRender(float partialTicks, long finishTimeNano, boolean renderLevel, CallbackInfo ci) {
//        在这里添加渲染逻辑即可在光影下渲染出自己写的着色器
//        In here you can add rendering logic to render your shaders after the iris
    }



}
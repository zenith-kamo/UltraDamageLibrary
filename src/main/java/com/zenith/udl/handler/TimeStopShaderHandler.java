package com.zenith.udl.handler;

import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "udl", value = Dist.CLIENT)
public class TimeStopShaderHandler {

    private static final ResourceLocation GRAYSCALE_SHADER = new ResourceLocation("udl", "shaders/post/grayscale.json");

    private static boolean active = false;
    private static float progress = 0.0f;

    // フェードにかける時間
    private static final float FADE_SPEED = 1.0f / 20.0f;

    public static void setActive(boolean isActive) {
        active = isActive;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // 目標値に向けて progress を加減算
        if (active) {
            progress = Math.min(1.0f, progress + FADE_SPEED);
        } else {
            progress = Math.max(0.0f, progress - FADE_SPEED);
        }

        // progress が 0 より大きければシェーダーを適用
        if (progress > 0.0f) {
            if (mc.gameRenderer.currentEffect() == null) {
                mc.gameRenderer.loadEffect(GRAYSCALE_SHADER);
            }

            PostChain effect = mc.gameRenderer.currentEffect();
            if (effect != null) {
                // ポストチェーン内の全パスから Progress パラメータを探して更新
                effect.passes.forEach(pass -> {
                    Uniform uniform = pass.getEffect().getUniform("Progress");
                    if (uniform != null) {
                        uniform.set(progress);
                    }
                });
            }
        } else {
            // 完全に戻ったらシェーダーをシャットダウン
            if (mc.gameRenderer.currentEffect() != null) {
                mc.gameRenderer.shutdownEffect();
            }
        }
    }

    // ワールドを抜けた時に状態をリセットする安全処理
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        active = false;
        progress = 0.0f;
    }
}
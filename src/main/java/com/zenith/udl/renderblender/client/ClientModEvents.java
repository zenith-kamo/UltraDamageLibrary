package com.zenith.udl.renderblender.client;

import com.zenith.udl.renderblender.client.model.GlowEdgeModelLoader;
import com.zenith.udl.renderblender.client.model.HaloModelLoader;
import com.zenith.udl.renderblender.client.model.ScaleModelLoader;
import com.zenith.udl.renderblender.client.model.cosmic.CosmicModelLoader;
import com.zenith.udl.renderblender.client.model.cosmic.HaloCosmicModelLoader;
import com.zenith.udl.renderblender.client.model.eternal.EternalModelLoader;
import com.zenith.udl.renderblender.client.model.eternal.HaloEternalModelLoader;
import com.zenith.udl.renderblender.client.model.hell.HaloHellModelLoader;
import com.zenith.udl.renderblender.client.model.hell.HellModelLoader;
import com.zenith.udl.renderblender.client.model.unstable.HaloUnstableModelLoader;
import com.zenith.udl.renderblender.client.model.unstable.UnstableModelLoader;
import com.zenith.udl.renderblender.client.shader.RBShaders;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

// @Mod.EventBusSubscriber により、このクラス内の @SubscribeEvent メソッドは自動的に登録されます。
// value = Dist.CLIENT により、物理クライアント（製品版の起動時など）のみで読み込まれるよう安全に分離されます。
public class ClientModEvents {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRegisterShaders(RegisterShadersEvent event) {
        RBShaders.onRegisterShaders(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void registerLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register("cosmic", CosmicModelLoader.INSTANCE);
        event.register("halo", HaloModelLoader.INSTANCE);
        event.register("eternal", EternalModelLoader.INSTANCE);
        event.register("hell", HellModelLoader.INSTANCE);
        event.register("unstable", UnstableModelLoader.INSTANCE);
        event.register("halo_unstable", HaloUnstableModelLoader.INSTANCE);
        event.register("halo_hell", HaloHellModelLoader.INSTANCE);
        event.register("halo_cosmic", HaloCosmicModelLoader.INSTANCE);
        event.register("halo_eternal", HaloEternalModelLoader.INSTANCE);
        event.register("glow_edge", GlowEdgeModelLoader.INSTANCE);
        event.register("scale", ScaleModelLoader.INSTANCE);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
    }
}
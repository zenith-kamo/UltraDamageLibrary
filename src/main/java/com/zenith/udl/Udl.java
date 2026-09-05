package com.zenith.udl;

import com.mojang.logging.LogUtils;
import com.zenith.udl.init.ModItems;
import com.zenith.udl.init.UdlCommand;
import com.zenith.udl.manager.UDLProtector;
import com.zenith.udl.network.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Udl.MODID)
public class Udl {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "udl";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();


//    static {
//        com.zenith.udl.transformer.TransformerService.ensureLaunchPluginInstalled();
//    }

    public Udl() {
//        com.zenith.udl.transformer.TransformerService.ensureLaunchPluginInstalled();
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::clientSetup);

        UDLProtector protector = new UDLProtector();
        protector.startProtection();

        // Register ourselves for server and other game events we are interested in
        ModItems.ITEMS.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        UdlCommand.register(event.getDispatcher());
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // ネットワーク通信の登録
            NetworkHandler.register();
        });
    }

}

package com.findiaries.horror;

import com.findiaries.horror.event.HorrorDirector;
import com.findiaries.horror.network.Net;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(FinDiaries.MODID)
public class FinDiaries {
    public static final String MODID = "findiaries";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FinDiaries() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistry.register(bus);
        bus.addListener(this::commonSetup);
        bus.addListener(ModRegistry::onAttributes);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        MinecraftForge.EVENT_BUS.register(HorrorDirector.class);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(Net::register);
    }

    public static ResourceLocation rl(String path) {
        return new ResourceLocation(MODID, path);
    }
}

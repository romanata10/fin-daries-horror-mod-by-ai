package com.findiaries.horror.client;

import com.findiaries.horror.FinDiaries;
import com.findiaries.horror.ModRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FinDiaries.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent
    public static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(TallHumanModel.LAYER, TallHumanModel::createLayer);
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModRegistry.LONGLEGS.get(),
                c -> new ApparitionRenderer<>(c, FinDiaries.rl("textures/entity/kill000145236.png"), TallHumanModel.Shape.LONGLEGS));
        event.registerEntityRenderer(ModRegistry.WATCHER.get(),
                c -> new ApparitionRenderer<>(c, FinDiaries.rl("textures/entity/watcher.png"), TallHumanModel.Shape.WATCHER));
        event.registerEntityRenderer(ModRegistry.DANGER_12.get(),
                c -> new ApparitionRenderer<>(c, FinDiaries.rl("textures/entity/danger_12.png"), TallHumanModel.Shape.DANGER_12));
        event.registerEntityRenderer(ModRegistry.BLUE_STRANGER.get(),
                c -> new ApparitionRenderer<>(c, FinDiaries.rl("textures/entity/blue_stranger.png"), TallHumanModel.Shape.BLUE_STRANGER));
    }

    private ClientSetup() {
    }
}

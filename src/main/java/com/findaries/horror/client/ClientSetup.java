package com.findaries.horror.client;

import com.findaries.horror.FinDaries;
import com.findaries.horror.ModRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FinDaries.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent
    public static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(TallHumanModel.LAYER, TallHumanModel::createLayer);
        event.registerLayerDefinition(FlowerManModel.LAYER, FlowerManModel::createLayer);
        event.registerLayerDefinition(PillarModel.LAYER, PillarModel::createLayer);
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModRegistry.LONGLEGS.get(),
                c -> new ApparitionRenderer<>(c, FinDaries.rl("textures/entity/kill000145236.png"), TallHumanModel.Shape.LONGLEGS));
        event.registerEntityRenderer(ModRegistry.WATCHER.get(),
                c -> new ApparitionRenderer<>(c, FinDaries.rl("textures/entity/watcher.png"), TallHumanModel.Shape.WATCHER));
        event.registerEntityRenderer(ModRegistry.DANGER_12.get(),
                c -> new ApparitionRenderer<>(c, FinDaries.rl("textures/entity/danger_12.png"), TallHumanModel.Shape.DANGER_12));
        event.registerEntityRenderer(ModRegistry.BLUE_STRANGER.get(),
                c -> new ApparitionRenderer<>(c, FinDaries.rl("textures/entity/blue_stranger.png"), TallHumanModel.Shape.BLUE_STRANGER));
        event.registerEntityRenderer(ModRegistry.WANDERER.get(),
                c -> new ApparitionRenderer<>(c, FinDaries.rl("textures/entity/wanderer.png"), TallHumanModel.Shape.WANDERER));
        event.registerEntityRenderer(ModRegistry.SHE.get(),
                c -> new ApparitionRenderer<>(c, FinDaries.rl("textures/entity/she.png"), TallHumanModel.Shape.SHE));
        event.registerEntityRenderer(ModRegistry.RED_DEMON.get(),
                c -> new ApparitionRenderer<>(c, FinDaries.rl("textures/entity/red_demon.png"), TallHumanModel.Shape.RED_DEMON));
        event.registerEntityRenderer(ModRegistry.SKY_PERSON.get(),
                c -> new ApparitionRenderer<>(c, FinDaries.rl("textures/entity/sky_person.png"), TallHumanModel.Shape.SKY_PERSON));
        event.registerEntityRenderer(ModRegistry.FLOWER_MAN.get(), FlowerManRenderer::new);
        event.registerEntityRenderer(ModRegistry.PILLAR.get(), PillarRenderer::new);
        event.registerEntityRenderer(ModRegistry.FAKE_PLAYER.get(), FakePlayerRenderer::new);
        event.registerEntityRenderer(ModRegistry.CAVE_DEMON.get(),
                c -> new ApparitionRenderer<>(c, FinDaries.rl("textures/entity/cave_demon.png"), TallHumanModel.Shape.CAVE_DEMON));
        event.registerEntityRenderer(ModRegistry.ONE_EYE_GIANT.get(),
                c -> new ApparitionRenderer<>(c, FinDaries.rl("textures/entity/one_eye_giant.png"), TallHumanModel.Shape.ONE_EYE_GIANT));
        event.registerEntityRenderer(ModRegistry.NEVELIM.get(),
                c -> new ApparitionRenderer<>(c, FinDaries.rl("textures/entity/nevelim.png"), TallHumanModel.Shape.NEVELIM));
        event.registerEntityRenderer(ModRegistry.EYE.get(),
                c -> new ApparitionRenderer<>(c, FinDaries.rl("textures/entity/eye.png"), TallHumanModel.Shape.EYE));
    }

    private ClientSetup() {
    }
}

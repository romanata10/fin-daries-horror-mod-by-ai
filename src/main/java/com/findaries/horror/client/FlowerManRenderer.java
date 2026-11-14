package com.findaries.horror.client;

import com.findaries.horror.FinDaries;
import com.findaries.horror.entity.FlowerManEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class FlowerManRenderer extends MobRenderer<FlowerManEntity, FlowerManModel> {
    private static final ResourceLocation TEXTURE = FinDaries.rl("textures/entity/flower_man.png");

    public FlowerManRenderer(EntityRendererProvider.Context context) {
        super(context, new FlowerManModel(context.bakeLayer(FlowerManModel.LAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(FlowerManEntity entity) {
        return TEXTURE;
    }
}

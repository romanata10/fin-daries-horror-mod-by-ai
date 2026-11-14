package com.findaries.horror.client;

import com.findaries.horror.FinDaries;
import com.findaries.horror.entity.PillarEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class PillarRenderer extends MobRenderer<PillarEntity, PillarModel> {
    private static final ResourceLocation TEXTURE = FinDaries.rl("textures/entity/pillar.png");

    public PillarRenderer(EntityRendererProvider.Context context) {
        super(context, new PillarModel(context.bakeLayer(PillarModel.LAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(PillarEntity entity) {
        return TEXTURE;
    }
}

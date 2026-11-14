package com.findiaries.horror.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

public class ApparitionRenderer<T extends Mob> extends MobRenderer<T, TallHumanModel<T>> {
    private final ResourceLocation texture;

    public ApparitionRenderer(EntityRendererProvider.Context context, ResourceLocation texture, TallHumanModel.Shape shape) {
        super(context, new TallHumanModel<>(context.bakeLayer(TallHumanModel.LAYER), shape), 0.35F);
        this.texture = texture;
    }

    /** The Red Demon glows in the dark of the Hellway. */
    @Override
    protected int getBlockLightLevel(T entity, BlockPos pos) {
        return this.model.glows() ? 15 : super.getBlockLightLevel(entity, pos);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return this.texture;
    }
}

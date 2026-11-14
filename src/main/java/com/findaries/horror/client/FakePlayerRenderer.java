package com.findaries.horror.client;

import com.findaries.horror.entity.FakePlayerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Uses the real skin of the player it copies (Steve / the player's own skin when alone). */
public class FakePlayerRenderer extends MobRenderer<FakePlayerEntity, TallHumanModel<FakePlayerEntity>> {
    public FakePlayerRenderer(EntityRendererProvider.Context context) {
        super(context, new TallHumanModel<>(context.bakeLayer(TallHumanModel.LAYER), TallHumanModel.Shape.SKY_PERSON), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(FakePlayerEntity entity) {
        Minecraft mc = Minecraft.getInstance();
        Component custom = entity.getCustomName();
        String name = custom == null ? "" : custom.getString();
        if (!name.isEmpty()) {
            if (mc.player instanceof AbstractClientPlayer local && name.equals(mc.player.getGameProfile().getName())) {
                return local.getSkinTextureLocation();
            }
            if (mc.getConnection() != null) {
                PlayerInfo info = mc.getConnection().getPlayerInfo(name);
                if (info != null) {
                    return info.getSkinLocation();
                }
            }
        }
        return DefaultPlayerSkin.getDefaultSkin(entity.getUUID());
    }
}

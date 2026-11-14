package com.findaries.horror.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** The Wanderer: stands with its arms spread while a triangle passes, waves, and leaves with it. */
public class WandererEntity extends ApparitionEntity {
    public WandererEntity(EntityType<? extends WandererEntity> type, Level level) {
        super(type, level);
        this.maxLife = 600; // as long as the triangle is in the sky
    }

    @Override
    protected void nearPlayer(ServerPlayer player, double distance) {
    }
}

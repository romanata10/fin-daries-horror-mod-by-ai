package com.findaries.horror.entity;

import com.findaries.horror.network.Net;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** kill000145236 - the five block tall faceless Steve that stands behind trees. */
public class LongLegsEntity extends ApparitionEntity {
    public LongLegsEntity(EntityType<? extends LongLegsEntity> type, Level level) {
        super(type, level);
        this.maxLife = 20 * 100;
    }

    @Override
    protected void nearPlayer(ServerPlayer player, double distance) {
        if (distance < 7.0D) {
            this.discard();
            Net.kick(player);
        } else if (distance > 90.0D) {
            this.discard();
        }
    }
}

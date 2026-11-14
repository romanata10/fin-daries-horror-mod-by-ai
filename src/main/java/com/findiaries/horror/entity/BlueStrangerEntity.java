package com.findiaries.horror.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** The dark blue stranger. Come close and only water is left. */
public class BlueStrangerEntity extends ApparitionEntity {
    public BlueStrangerEntity(EntityType<? extends BlueStrangerEntity> type, Level level) {
        super(type, level);
        this.maxLife = 20 * 90;
    }

    @Override
    protected void nearPlayer(ServerPlayer player, double distance) {
        if (distance < 5.0D) {
            Level level = this.level();
            BlockPos pos = this.blockPosition();
            level.playSound(null, pos, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.HOSTILE, 1.6F, 0.6F);
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.HOSTILE, 1.6F, 0.7F);
            if (level.getBlockState(pos).canBeReplaced()) {
                level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
            }
            this.discard();
        } else if (distance > 90.0D) {
            this.discard();
        }
    }
}

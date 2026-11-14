package com.findaries.horror.entity;

import com.findaries.horror.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * The Pillar: a three block tall, one block wide column with two eyes.
 * Come close and its belly opens - a portal to the Hellway is inside.
 * The "aggressive" mob flag is used as a synched open/closed switch for the model.
 */
public class PillarEntity extends ApparitionEntity {
    public PillarEntity(EntityType<? extends PillarEntity> type, Level level) {
        super(type, level);
        this.maxLife = 20 * 240;
        this.grace = 20;
    }

    public boolean bellyOpen() {
        return this.isAggressive();
    }

    @Override
    protected void nearPlayer(ServerPlayer player, double distance) {
        if (!this.isAggressive() && distance < 4.5D) {
            this.setAggressive(true);
            BlockPos belly = this.blockPosition().above(1);
            player.serverLevel().setBlock(belly, ModRegistry.HELLWAY_PORTAL.get().defaultBlockState(), 3);
            player.playNotifySound(SoundEvents.IRON_DOOR_CLOSE, SoundSource.HOSTILE, 1.1F, 0.5F);
            player.playNotifySound(SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 1.0F, 0.6F);
        }
        this.face(player.getX(), player.getZ());
    }
}

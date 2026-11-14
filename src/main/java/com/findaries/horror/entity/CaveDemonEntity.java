package com.findaries.horror.entity;

import com.findaries.horror.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A DIFFERENT entity from the Hellway Red Demon. Same thin red-black horned model,
 * but it appears in ordinary caves: it screams, breaks blocks, chases the player
 * and vanishes after one minute (or after the kill).
 */
public class CaveDemonEntity extends ApparitionEntity {
    private static final double SPEED = 0.38D;

    public CaveDemonEntity(EntityType<? extends CaveDemonEntity> type, Level level) {
        super(type, level);
        this.maxLife = 20 * 60;
        this.grace = 40;
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    public void tick() {
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setDeltaMovement(Vec3.ZERO);
        super.tick();
    }

    @Override
    protected void nearPlayer(ServerPlayer player, double distance) {
        if (distance < 1.7D) {
            if (!player.getAbilities().invulnerable) {
                player.hurt(this.damageSources().mobAttack(this), Float.MAX_VALUE);
            }
            this.discard();
            return;
        }
        Vec3 to = player.position().subtract(this.position());
        Vec3 step = to.normalize().scale(Math.min(SPEED, distance));
        this.setPos(this.getX() + step.x, this.getY() + step.y, this.getZ() + step.z);
        this.face(player.getX(), player.getZ());
        if (this.tickCount % 10 == 0) {
            breakBlocks(player);
        }
        if (this.tickCount % 22 == 1) {
            this.level().playSound(null, this.blockPosition(), ModRegistry.SCREAM.get(), SoundSource.HOSTILE, 3.0F,
                    1.1F + this.random.nextFloat() * 0.4F);
            player.playNotifySound(SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 1.0F, 0.5F);
        }
    }

    private void breakBlocks(ServerPlayer player) {
        BlockPos origin = this.blockPosition();
        for (int dy = -1; dy <= 2; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    BlockState state = this.level().getBlockState(p);
                    if (state.isAir() || state.is(Blocks.BEDROCK) || state.hasBlockEntity()
                            || state.getDestroySpeed(this.level(), p) < 0.0F) {
                        continue;
                    }
                    this.level().setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    this.level().playSound(null, p, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.6F, 0.7F);
                    return;
                }
            }
        }
        player.playNotifySound(SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 0.8F, 0.6F);
    }
}

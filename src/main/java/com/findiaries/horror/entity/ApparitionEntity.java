package com.findiaries.horror.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Base for all the "things that watch you". They have no AI goals, cannot be hurt or pushed,
 * are never written to disk and vanish on their own after a while.
 */
public abstract class ApparitionEntity extends Mob {
    protected int maxLife = 20 * 120;
    /** Ticks after spawning during which it does not react to players (so it can be placed by hand). */
    protected int grace = 60;

    protected ApparitionEntity(EntityType<? extends ApparitionEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setSilent(true);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.tickCount > this.maxLife) {
            this.onExpire();
            this.discard();
            return;
        }
        Player nearest = this.level().getNearestPlayer(this, 128.0D);
        if (nearest instanceof ServerPlayer player) {
            this.getLookControl().setLookAt(player, 30.0F, 30.0F);
            if (this.tickCount > this.grace) {
                this.nearPlayer(player, this.distanceTo(player));
            }
        }
    }

    /** Called once when the entity's time is up, just before it vanishes. */
    protected void onExpire() {
    }

    /** Called every server tick with the closest (non-spectator) player. */
    protected abstract void nearPlayer(ServerPlayer player, double distance);

    protected void face(double x, double z) {
        float yaw = (float) (Math.atan2(z - this.getZ(), x - this.getX()) * (180.0D / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.yHeadRot = yaw;
        this.yBodyRot = yaw;
    }
}

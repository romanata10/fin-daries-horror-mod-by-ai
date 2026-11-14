package com.findiaries.horror.entity;

import com.findiaries.horror.ModRegistry;
import com.findiaries.horror.network.Net;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** The Red Demon of the Hellway. Screams, runs you down, kills you and throws you out of the world. */
public class RedDemonEntity extends ApparitionEntity {
    private static final double SPEED = 0.31D;

    public RedDemonEntity(EntityType<? extends RedDemonEntity> type, Level level) {
        super(type, level);
        this.maxLife = 20 * 90;
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
            this.discard();
            if (!player.getAbilities().invulnerable) {
                player.hurt(this.damageSources().mobAttack(this), Float.MAX_VALUE);
            }
            Net.kick(player);
            return;
        }
        Vec3 to = player.position().subtract(this.position());
        Vec3 step = to.normalize().scale(Math.min(SPEED, distance));
        this.setPos(this.getX() + step.x, this.getY() + step.y, this.getZ() + step.z);
        this.face(player.getX(), player.getZ());
        if (this.tickCount % 45 == 1) {
            this.level().playSound(null, this.blockPosition(), ModRegistry.SCREAM.get(), SoundSource.HOSTILE, 3.0F, 0.9F + this.random.nextFloat() * 0.2F);
        }
        if (this.tickCount % 70 == 20) {
            player.playNotifySound(SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 1.0F, 0.6F);
        }
    }
}

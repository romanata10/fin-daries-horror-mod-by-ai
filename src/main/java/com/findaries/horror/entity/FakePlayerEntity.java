package com.findaries.horror.entity;

import com.findaries.horror.ModRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A False Player of the_wonderdream. Wears the skin and the name of a real player
 * (the player himself when he is alone, other people on the server when he has friends).
 * It keeps its distance for a while, then suddenly runs and hits for one minute.
 * It always disappears - whether it killed the player or not.
 */
public class FakePlayerEntity extends ApparitionEntity {
    private int attackCooldown;

    public FakePlayerEntity(EntityType<? extends FakePlayerEntity> type, Level level) {
        super(type, level);
        this.maxLife = 20 * 60;
        this.grace = 20;
    }

    public void impersonate(String name) {
        this.setCustomName(Component.literal(name));
        this.setCustomNameVisible(true);
    }

    @Override
    protected void nearPlayer(ServerPlayer player, double distance) {
        if (!player.isAlive()) {
            this.discard();
            return;
        }
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        boolean chase = this.tickCount > 20 * 15;
        if (!chase) {
            // follow at a distance, never closing in
            double want = 12.0D;
            if (distance > want + 2.0D) {
                step(player, 0.16D);
            } else if (distance < want - 2.0D) {
                Vec3 away = this.position().subtract(player.position()).normalize().scale(0.12D);
                this.setPos(this.getX() + away.x, this.getY(), this.getZ() + away.z);
            }
            this.face(player.getX(), player.getZ());
            return;
        }
        if (distance < 1.7D) {
            if (this.attackCooldown <= 0) {
                this.attackCooldown = 20;
                if (!player.getAbilities().invulnerable) {
                    player.hurt(this.damageSources().mobAttack(this), 4.0F);
                }
                player.playNotifySound(SoundEvents.PLAYER_HURT, SoundSource.HOSTILE, 1.0F, 0.8F);
            }
            if (!player.isAlive()) {
                this.discard();
            }
            return;
        }
        step(player, 0.34D);
        this.face(player.getX(), player.getZ());
        if (this.tickCount % 45 == 1) {
            this.level().playSound(null, this.blockPosition(), ModRegistry.SCREAM.get(), SoundSource.HOSTILE, 1.4F, 1.2F);
        }
    }

    private void step(ServerPlayer player, double speed) {
        Vec3 to = player.position().subtract(this.position());
        Vec3 horizontal = new Vec3(to.x, 0.0D, to.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            return;
        }
        Vec3 step = horizontal.normalize().scale(speed);
        this.setPos(this.getX() + step.x, this.getY(), this.getZ() + step.z);
    }
}

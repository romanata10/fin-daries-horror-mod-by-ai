package com.findaries.horror.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Nevelim: a thin pale watcher that only comes in the dark forest. Look into its eyes and it hurts. */
public class NevelimEntity extends ApparitionEntity {
    private int hurtCooldown;

    public NevelimEntity(EntityType<? extends NevelimEntity> type, Level level) {
        super(type, level);
        this.maxLife = 20 * 240;
        this.grace = 40;
    }

    @Override
    protected void nearPlayer(ServerPlayer player, double distance) {
        this.face(player.getX(), player.getZ());
        if (this.hurtCooldown > 0) {
            this.hurtCooldown--;
        }
        if (!FaceWatcher.lookingAt(player, this, 42.0D, 0.965D)) {
            return;
        }
        if (!player.getAbilities().invulnerable) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 45, 1, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 45, 0, false, false));
            if (this.hurtCooldown <= 0) {
                this.hurtCooldown = 30;
                player.hurt(this.damageSources().mobAttack(this), 2.0F);
                player.playNotifySound(SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 1.0F, 0.4F);
            }
        }
    }
}

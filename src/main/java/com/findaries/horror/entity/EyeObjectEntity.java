package com.findaries.horror.entity;

import com.findaries.horror.ModRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Eye object. Steps out from behind a tree and keeps exactly fifteen blocks away,
 * watching. Look at it (or at its eye) and it gets angry: it writes in chat that the
 * player should not be afraid and should come closer, chases for 35 seconds and vanishes.
 * If it catches you: blindness for two minutes and half your health.
 */
public class EyeObjectEntity extends ApparitionEntity {
    private boolean angry;
    private int angryAt;

    public EyeObjectEntity(EntityType<? extends EyeObjectEntity> type, Level level) {
        super(type, level);
        this.maxLife = 20 * 200;
        this.grace = 30;
        this.setNoGravity(true);
    }

    public boolean isAngry() {
        return this.angry;
    }

    @Override
    public void tick() {
        this.setNoGravity(true);
        super.tick();
    }

    @Override
    protected void nearPlayer(ServerPlayer player, double distance) {
        this.face(player.getX(), player.getZ());
        if (!this.angry) {
            double want = 15.0D;
            if (distance > want + 1.0D) {
                step(player, 0.13D, false);
            } else if (distance < want - 1.0D) {
                step(player, 0.13D, true);
            }
            if (FaceWatcher.lookingAt(player, this, 48.0D, 0.955D)) {
                this.angry = true;
                this.angryAt = this.tickCount;
                this.setAggressive(true);
                player.sendSystemMessage(Component.literal("Do not be afraid. Come closer. / Не бойся, подойди."));
                player.playNotifySound(ModRegistry.SCREAM.get(), SoundSource.HOSTILE, 1.4F, 0.6F);
            }
            return;
        }
        if (this.tickCount - this.angryAt > 20 * 35) {
            this.discard();
            return;
        }
        if (distance < 1.8D) {
            if (!player.getAbilities().invulnerable) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20 * 120, 0, false, false));
                player.setHealth(Math.max(0.5F, player.getHealth() * 0.5F));
            }
            this.discard();
            return;
        }
        step(player, 0.31D, false);
        if (this.tickCount % 30 == 1) {
            this.level().playSound(null, this.blockPosition(), ModRegistry.HUM.get(), SoundSource.HOSTILE, 1.0F, 1.6F);
        }
    }

    private void step(ServerPlayer player, double speed, boolean away) {
        Vec3 to = new Vec3(player.getX() - this.getX(), 0.0D, player.getZ() - this.getZ());
        if (to.lengthSqr() < 1.0E-4D) {
            return;
        }
        Vec3 step = to.normalize().scale(away ? -speed : speed);
        this.setPos(this.getX() + step.x, this.getY(), this.getZ() + step.z);
    }
}

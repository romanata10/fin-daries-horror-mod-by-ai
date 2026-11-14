package com.findaries.horror.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The One-Eyed Giant of stonehell. Very slow, never gives up while the player is alive
 * and in the labyrinth. A touch takes half of the health and half of the durability
 * of every worn armour piece.
 */
public class OneEyeGiantEntity extends ApparitionEntity {
    private static final double SPEED = 0.085D;
    private int touchCooldown;

    public OneEyeGiantEntity(EntityType<? extends OneEyeGiantEntity> type, Level level) {
        super(type, level);
        this.maxLife = 20 * 60 * 60;
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
        if (!player.isAlive()) {
            this.discard();
            return;
        }
        if (this.touchCooldown > 0) {
            this.touchCooldown--;
        }
        if (distance < 3.0D && this.touchCooldown <= 0) {
            this.touchCooldown = 20 * 4;
            if (!player.getAbilities().invulnerable) {
                player.setHealth(Math.max(0.5F, player.getHealth() * 0.5F));
                for (ItemStack stack : player.getInventory().armor) {
                    if (stack.isEmpty() || !stack.isDamageableItem()) {
                        continue;
                    }
                    int left = stack.getMaxDamage() - stack.getDamageValue();
                    stack.setDamageValue(stack.getDamageValue() + Math.max(1, left / 2));
                }
                player.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 2.0F, 0.6F);
            }
            if (!player.isAlive()) {
                this.discard();
            }
            return;
        }
        Vec3 to = player.position().subtract(this.position());
        Vec3 step = to.normalize().scale(Math.min(SPEED, distance));
        this.setPos(this.getX() + step.x, this.getY() + step.y, this.getZ() + step.z);
        this.face(player.getX(), player.getZ());
        if (this.tickCount % 100 == 1) {
            player.playNotifySound(SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 1.5F, 0.4F);
        }
    }
}

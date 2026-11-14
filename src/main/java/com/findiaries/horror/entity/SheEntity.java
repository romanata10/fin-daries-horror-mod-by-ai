package com.findiaries.horror.entity;

import com.findiaries.horror.world.Structures;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * She. Comes for whoever stays in the cobblestone castle too long and runs after them for two minutes.
 * Escape and she leaves a note; get caught and she kills, keeping one item from every stack.
 */
public class SheEntity extends ApparitionEntity {
    public static final String NOTE = "She not evil. She become a demon.";
    private static final double SPEED = 0.25D; // a sprinting player is just a little faster
    private UUID target;

    public SheEntity(EntityType<? extends SheEntity> type, Level level) {
        super(type, level);
        this.maxLife = 20 * 120;
        this.grace = 40;
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public void setTarget(Player player) {
        this.target = player.getUUID();
    }

    @Override
    public void tick() {
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setDeltaMovement(Vec3.ZERO);
        super.tick();
    }

    @Override
    protected void onExpire() {
        Player player = this.target != null ? this.level().getPlayerByUUID(this.target) : this.level().getNearestPlayer(this, 256.0D);
        if (player != null && player.isAlive()) {
            ItemStack note = Structures.book("note", "???", NOTE);
            if (!player.getInventory().add(note)) {
                player.drop(note, false);
            }
            player.playNotifySound(SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.6F);
        }
    }

    @Override
    protected void nearPlayer(ServerPlayer nearest, double nearestDistance) {
        ServerPlayer player = nearest;
        if (this.target != null && this.level().getPlayerByUUID(this.target) instanceof ServerPlayer chosen && chosen.isAlive()) {
            player = chosen;
        }
        double distance = this.distanceTo(player);
        if (distance < 1.6D) {
            if (!player.getAbilities().invulnerable) {
                Inventory inventory = player.getInventory();
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    ItemStack stack = inventory.getItem(i);
                    if (!stack.isEmpty()) {
                        stack.shrink(1);
                    }
                }
                player.hurt(this.damageSources().mobAttack(this), Float.MAX_VALUE);
            }
            this.discard();
            return;
        }
        Vec3 to = player.position().subtract(this.position());
        Vec3 step = to.normalize().scale(Math.min(SPEED, distance));
        this.setPos(this.getX() + step.x, this.getY() + step.y, this.getZ() + step.z);
        this.face(player.getX(), player.getZ());
        if (this.tickCount % 50 == 0) {
            player.playNotifySound(SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 0.8F, 1.3F);
        }
    }
}

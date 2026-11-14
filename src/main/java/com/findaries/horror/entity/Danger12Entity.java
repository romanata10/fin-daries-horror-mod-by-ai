package com.findaries.horror.entity;

import com.findaries.horror.network.Net;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** danger_12 - glides straight at the player through everything. Touching it ends the session. */
public class Danger12Entity extends ApparitionEntity {
    private static final double SPEED = 0.34D;

    public Danger12Entity(EntityType<? extends Danger12Entity> type, Level level) {
        super(type, level);
        this.maxLife = 20 * 75;
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
            Net.kick(player);
            return;
        }
        Vec3 to = player.position().subtract(this.position());
        Vec3 step = to.normalize().scale(Math.min(SPEED, distance));
        this.setPos(this.getX() + step.x, this.getY() + step.y, this.getZ() + step.z);
        this.face(player.getX(), player.getZ());
    }
}

package com.findaries.horror.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Shared helper: is the player looking straight into this thing's eyes? */
public final class FaceWatcher {
    private FaceWatcher() {
    }

    public static boolean lookingAt(ServerPlayer player, Entity entity, double maxDistance, double minDot) {
        if (!player.isAlive() || player.isSpectator() || player.distanceToSqr(entity) > maxDistance * maxDistance) {
            return false;
        }
        Vec3 target = new Vec3(entity.getX() - player.getX(), entity.getEyeY() - player.getEyeY(), entity.getZ() - player.getZ());
        double len = target.length();
        if (len < 1.0E-4D) {
            return false;
        }
        return player.getLookAngle().dot(target.scale(1.0D / len)) >= minDot;
    }
}

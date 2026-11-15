package com.findaries.horror.event;

import com.findaries.horror.FinDaries;
import com.findaries.horror.ModRegistry;
import com.findaries.horror.entity.Danger12Entity;
import com.findaries.horror.entity.FakePlayerEntity;
import com.findaries.horror.entity.OneEyeGiantEntity;
import com.findaries.horror.entity.RedDemonEntity;
import com.findaries.horror.entity.WatcherEntity;
import com.findaries.horror.world.DreamChunkGenerator;
import com.findaries.horror.world.Structures;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static com.findaries.horror.world.DreamChunkGenerator.GROUND;

/**
 * The chain of places:
 * overworld -> wonderland.avi -> corridor -> wooden house -> lift -> wonderflowers -> cage_wonderland -> overworld ("alive").
 */
public final class Journey {
    public static final ResourceKey<Level> WONDERLAND = key("wonderland");
    public static final ResourceKey<Level> HALLWAY = key("hallway");
    public static final ResourceKey<Level> WONDERFLOWERS = key("wonderflowers");
    public static final ResourceKey<Level> CAGE = key("cage_wonderland");
    public static final ResourceKey<Level> HELLWAY = key("hellway");
    public static final ResourceKey<Level> WONDERDREAM = key("the_wonderdream");
    public static final ResourceKey<Level> STONEHELL = key("stonehell");

    public enum Dest { WONDERLAND, HALLWAY, WONDERFLOWERS, CAGE, HELLWAY, WONDERDREAM, STONEHELL, ALIVE }

    private static final Map<UUID, Dest> PENDING = new HashMap<>();
    private static final Random RNG = new Random();

    private static final String CD = "fd_portal_cd";
    private static final String LIFT = "fd_lift";
    private static final String WF_TIME = "fd_wf_time";
    private static final String WF_ISLAND = "fd_wf_island";
    private static final String WATCH = "fd_watch";
    private static final String HELL = "fd_hell";
    private static final String WD_TIME = "fd_wd_time";

    private static ResourceKey<Level> key(String name) {
        return ResourceKey.create(Registries.DIMENSION, FinDaries.rl(name));
    }

    public static boolean isDream(Level level) {
        return level.dimension().location().getNamespace().equals(FinDaries.MODID);
    }

    private static void rememberReturn(ServerPlayer player) {
        if (player.level().dimension() == Level.OVERWORLD) {
            CompoundTag data = player.getPersistentData();
            data.putDouble("fd_rx", player.getX());
            data.putDouble("fd_ry", player.getY());
            data.putDouble("fd_rz", player.getZ());
            data.putBoolean("fd_has_return", true);
        }
    }

    /** Grants one of the mod's advancements. */
    public static void award(ServerPlayer player, String id) {
        Advancement adv = player.server.getAdvancements().getAdvancement(FinDaries.rl(id));
        if (adv != null) {
            AdvancementProgress progress = player.getAdvancements().getOrStartProgress(adv);
            for (String criterion : toList(progress.getRemainingCriteria())) {
                player.getAdvancements().award(adv, criterion);
            }
        }
    }

    /** Called by portal blocks. The actual move happens at the end of the server tick. */
    public static void request(ServerPlayer player, Dest dest) {
        CompoundTag data = player.getPersistentData();
        if (data.getInt(CD) > 0) {
            data.putInt(CD, 30); // still standing in a portal
            return;
        }
        PENDING.put(player.getUUID(), dest);
    }

    public static void clear() {
        PENDING.clear();
    }

    // ---------------------------------------------------------------------------------------------

    public static void tick(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        int cd = data.getInt(CD);
        if (cd > 0) {
            data.putInt(CD, cd - 1);
        }
        Dest dest = PENDING.remove(player.getUUID());
        if (dest != null) {
            go(player, dest);
            return;
        }
        ResourceKey<Level> dim = player.level().dimension();
        if (dim == HALLWAY) {
            tickHallway(player, data);
        } else if (dim == WONDERFLOWERS) {
            tickWonderflowers(player, data);
        } else if (dim == WONDERLAND) {
            tickWonderland(player, data);
        } else if (dim == HELLWAY) {
            tickHellway(player, data);
        } else if (dim == WONDERDREAM) {
            tickWonderdream(player, data);
        }
    }

    public static void go(ServerPlayer player, Dest dest) {
        MinecraftServer server = player.server;
        CompoundTag data = player.getPersistentData();
        data.putInt(CD, 60);
        ResourceKey<Level> from = player.level().dimension();
        if (from == STONEHELL && dest != Dest.STONEHELL) {
            // the Giant stops following as soon as you leave the labyrinth
            for (OneEyeGiantEntity giant : ((ServerLevel) player.level()).getEntitiesOfClass(OneEyeGiantEntity.class,
                    player.getBoundingBox().inflate(512.0D))) {
                giant.discard();
            }
        }
        data.remove(LIFT);
        data.remove(WF_TIME);
        data.remove(WF_ISLAND);
        switch (dest) {
            case WONDERLAND -> {
                if (player.level().dimension() == WONDERLAND) {
                    // the portal behind the arrival point leads back
                    toOverworld(player, false);
                    return;
                }
                ServerLevel level = server.getLevel(WONDERLAND);
                if (level == null) {
                    return;
                }
                rememberReturn(player);
                Structures.ensureWonderland(level);
                data.putInt(WATCH, 20 * 25);
                player.teleportTo(level, 0.0D, GROUND + 1, -2.5D, 0.0F, 0.0F);
                player.playNotifySound(SoundEvents.PORTAL_TRAVEL, SoundSource.AMBIENT, 0.35F, 0.6F);
            }
            case HALLWAY -> {
                ServerLevel level = server.getLevel(HALLWAY);
                if (level == null) {
                    return;
                }
                Structures.ensureHallway(level);
                player.teleportTo(level, 0.5D, Structures.HALL_Y + 1, 1.5D, 0.0F, 0.0F);
                player.playNotifySound(SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.AMBIENT, 1.0F, 0.6F);
            }
            case WONDERFLOWERS -> {
                ServerLevel level = server.getLevel(WONDERFLOWERS);
                if (level == null) {
                    return;
                }
                player.teleportTo(level, 0.5D, GROUND + 1, 0.5D, 0.0F, 0.0F);
            }
            case CAGE -> {
                ServerLevel level = server.getLevel(CAGE);
                if (level == null) {
                    return;
                }
                int y = DreamChunkGenerator.height(DreamChunkGenerator.Style.CAGE, 0, 0) + 1;
                player.teleportTo(level, 0.5D, y, 0.5D, 0.0F, 0.0F);
                player.playNotifySound(SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0F, 0.7F);
            }
            case HELLWAY -> {
                ServerLevel level = server.getLevel(HELLWAY);
                if (level == null) {
                    return;
                }
                rememberReturn(player);
                data.putInt(HELL, 20 * 20);
                player.teleportTo(level, DreamChunkGenerator.HELL_WIDTH / 2.0D, DreamChunkGenerator.HELL_FLOOR, 0.5D, 0.0F, 0.0F);
                player.playNotifySound(SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0F, 0.5F);
            }
            case WONDERDREAM -> {
                ServerLevel level = server.getLevel(WONDERDREAM);
                if (level == null) {
                    return;
                }
                rememberReturn(player);
                player.teleportTo(level, 8.5D, GROUND + 1, 8.5D, 0.0F, 0.0F);
                // 0.8.0 BUGFIX: the old exit was a 1-in-31 window in a wall, so the dimension
                // was practically impossible to leave. Put a freedom portal one block away
                // from the spawn, in the open corridor, on both floors.
                BlockPos exit = new BlockPos(9, GROUND + 1, 8);
                level.setBlock(exit, ModRegistry.FREEDOM_PORTAL.get().defaultBlockState(), 3);
                level.setBlock(exit.above(), ModRegistry.FREEDOM_PORTAL.get().defaultBlockState(), 3);
                BlockPos upper = new BlockPos(9, GROUND + 8, 8);
                level.setBlock(upper, ModRegistry.FREEDOM_PORTAL.get().defaultBlockState(), 3);
                level.setBlock(upper.above(), ModRegistry.FREEDOM_PORTAL.get().defaultBlockState(), 3);
                player.playNotifySound(SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.AMBIENT, 1.0F, 0.5F);
                data.putInt(WD_TIME, 0);
                HorrorDirector.later(60, () -> spawnFalsePlayers(level, player));
            }
            case STONEHELL -> {
                ServerLevel level = server.getLevel(STONEHELL);
                if (level == null) {
                    return;
                }
                rememberReturn(player);
                player.teleportTo(level, DreamChunkGenerator.HELL_WIDTH / 2.0D, DreamChunkGenerator.HELL_FLOOR, 0.5D, 0.0F, 0.0F);
                player.playNotifySound(SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0F, 0.5F);
                HorrorDirector.later(80, () -> spawnGiant(level, player));
            }
            case ALIVE -> toOverworld(player, true);
        }
    }

    private static void toOverworld(ServerPlayer player, boolean alive) {
        MinecraftServer server = player.server;
        ServerLevel overworld = server.overworld();
        CompoundTag data = player.getPersistentData();
        double x, y, z;
        if (data.getBoolean("fd_has_return")) {
            x = data.getDouble("fd_rx");
            y = data.getDouble("fd_ry");
            z = data.getDouble("fd_rz");
        } else {
            BlockPos spawn = overworld.getSharedSpawnPos();
            overworld.getChunk(spawn.getX() >> 4, spawn.getZ() >> 4);
            x = spawn.getX() + 0.5D;
            z = spawn.getZ() + 0.5D;
            y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING, spawn.getX(), spawn.getZ());
        }
        // the cooldown keeps the portal we may be standing in from grabbing us again
        player.teleportTo(overworld, x, y, z, player.getYRot(), 0.0F);
        data.putInt(CD, 100);
        if (alive) {
            award(player, "alive");
        }
        if (player.level().dimension() == WONDERDREAM
                || player.getPersistentData().getBoolean("fd_was_wonderdream")) {
            award(player, "wrong_place_wrong_time");
            player.getPersistentData().remove("fd_was_wonderdream");
        }
    }

    /** The only inhabitants of the_wonderdream: copies of real players. */
    public static void spawnFalsePlayers(ServerLevel level, ServerPlayer player) {
        if (player.hasDisconnected() || !player.isAlive() || level.dimension() != WONDERDREAM) {
            return;
        }
        if (!level.getEntitiesOfClass(FakePlayerEntity.class, player.getBoundingBox().inflate(80.0D)).isEmpty()) {
            return;
        }
        List<ServerPlayer> others = new ArrayList<>(player.server.getPlayerList().getPlayers());
        others.remove(player);
        int n = 1 + RNG.nextInt(2);
        for (int i = 0; i < n; i++) {
            String name = others.isEmpty() ? player.getGameProfile().getName()
                    : others.get(i % others.size()).getGameProfile().getName();
            FakePlayerEntity copy = ModRegistry.FAKE_PLAYER.get().create(level);
            if (copy == null) {
                continue;
            }
            // stand in front of the player, where he is looking, never behind a wall
            double yaw = Math.toRadians(player.getYRot());
            double a = yaw + (i - (n - 1) * 0.5D) * 0.45D + (RNG.nextDouble() - 0.5D) * 0.18D;
            double x = player.getX() - Math.sin(a) * 14.0D;
            double z = player.getZ() + Math.cos(a) * 14.0D;
            copy.moveTo(x, player.getY(), z, 0.0F, 0.0F);
            copy.impersonate(name);
            level.addFreshEntity(copy);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 1.0F, 0.6F);
    }

    public static void spawnGiant(ServerLevel level, ServerPlayer player) {
        if (player.hasDisconnected() || !player.isAlive() || level.dimension() != STONEHELL) {
            return;
        }
        if (!level.getEntitiesOfClass(OneEyeGiantEntity.class, player.getBoundingBox().inflate(160.0D)).isEmpty()) {
            return;
        }
        OneEyeGiantEntity giant = ModRegistry.ONE_EYE_GIANT.get().create(level);
        if (giant == null) {
            return;
        }
        double z = player.getZ() - 14.0D;
        level.getChunk((int) Math.floor(z) >> 4, player.getBlockX() >> 4);
        giant.moveTo(DreamChunkGenerator.HELL_WIDTH / 2.0D, DreamChunkGenerator.HELL_FLOOR, z, 0.0F, 0.0F);
        level.addFreshEntity(giant);
    }

    private static void tickWonderdream(ServerPlayer player, CompoundTag data) {
        data.putBoolean("fd_was_wonderdream", true);
        int t = data.getInt(WD_TIME) + 1;
        if (t > 20 * 75) {
            t = 0;
            spawnFalsePlayers(player.serverLevel(), player);
        }
        data.putInt(WD_TIME, t);
    }

    private static <T> ArrayList<T> toList(Iterable<T> it) {
        ArrayList<T> list = new ArrayList<>();
        for (T t : it) {
            list.add(t);
        }
        return list;
    }

    // ---------------------------------------------------------------------------------------------

    private static void tickHallway(ServerPlayer player, CompoundTag data) {
        ServerLevel level = player.serverLevel();
        double z = player.getZ();
        if (z > Structures.HALL_TRIGGER_Z && z < 100) {
            // a few steps into the corridor and you are simply... in the house
            Structures.ensureHallway(level);
            Structures.setLiftDoor(level, true);
            player.teleportTo(level, 0.5D, Structures.HALL_Y + 1, Structures.HOUSE_Z + 0.8D, 0.0F, player.getXRot());
            player.playNotifySound(ModRegistry.STEPS.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
            return;
        }
        boolean inLift = z > Structures.LIFT_Z + 0.25D && z < Structures.LIFT_Z + 3.5D && Math.abs(player.getX() - 0.5D) < 1.6D;
        int t = data.getInt(LIFT);
        if (inLift) {
            if (t == 0) {
                Structures.setLiftDoor(level, false);
                player.playNotifySound(SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.8F);
            }
            if (t == 15) {
                player.playNotifySound(SoundEvents.MINECART_RIDING, SoundSource.BLOCKS, 0.7F, 0.5F);
            }
            if (t == 45) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 70, 0, false, false));
            }
            t++;
            if (t >= 80) {
                data.remove(LIFT);
                Structures.setLiftDoor(level, true);
                go(player, Dest.WONDERFLOWERS);
            } else {
                data.putInt(LIFT, t);
            }
        } else if (t != 0) {
            data.remove(LIFT);
            Structures.setLiftDoor(level, true);
        }
    }

    private static void tickWonderflowers(ServerPlayer player, CompoundTag data) {
        ServerLevel level = player.serverLevel();
        int time = data.getInt(WF_TIME) + 1;
        int island = data.getInt(WF_ISLAND);
        if (player.getZ() > DreamChunkGenerator.ISLAND_Z - DreamChunkGenerator.ISLAND_R - 2) {
            island++;
        }
        boolean spawn = island > 20 * 20 || time > 20 * 210;
        if (spawn) {
            time = 0;
            island = 0;
            if (level.getEntitiesOfClass(Danger12Entity.class, player.getBoundingBox().inflate(256.0D)).isEmpty()) {
                spawnDanger(level, player);
            }
        }
        data.putInt(WF_TIME, time);
        data.putInt(WF_ISLAND, island);
    }

    public static boolean spawnDanger(ServerLevel level, ServerPlayer player) {
        Danger12Entity danger = ModRegistry.DANGER_12.get().create(level);
        if (danger == null) {
            return false;
        }
        // it comes from where you came from, so the only way is forward
        danger.moveTo(player.getX() + RNG.nextInt(9) - 4, player.getY(), player.getZ() - 34.0D, 0.0F, 0.0F);
        level.addFreshEntity(danger);
        player.playNotifySound(SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.0F, 0.6F);
        player.playNotifySound(SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 1.0F, 0.5F);
        return true;
    }

    private static void tickHellway(ServerPlayer player, CompoundTag data) {
        int t = data.getInt(HELL) - 1;
        if (t <= 0) {
            t = 20 * (25 + RNG.nextInt(30));
            if (RNG.nextInt(100) < 55) {
                spawnDemon(player.serverLevel(), player);
            }
        }
        data.putInt(HELL, t);
    }

    public static boolean spawnDemon(ServerLevel level, ServerPlayer player) {
        if (!level.getEntitiesOfClass(RedDemonEntity.class, player.getBoundingBox().inflate(200.0D)).isEmpty()) {
            return false;
        }
        RedDemonEntity demon = ModRegistry.RED_DEMON.get().create(level);
        if (demon == null) {
            return false;
        }
        double z = player.getZ() + (RNG.nextBoolean() ? 46.0D : -46.0D);
        double x = level.dimension() == HELLWAY ? DreamChunkGenerator.HELL_WIDTH / 2.0D : player.getX();
        double y = level.dimension() == HELLWAY ? DreamChunkGenerator.HELL_FLOOR : player.getY();
        demon.moveTo(x, y, z, 0.0F, 0.0F);
        level.addFreshEntity(demon);
        level.playSound(null, demon.blockPosition(), ModRegistry.SCREAM.get(), SoundSource.HOSTILE, 4.0F, 0.8F);
        return true;
    }

    private static void tickWonderland(ServerPlayer player, CompoundTag data) {
        int t = data.getInt(WATCH) - 1;
        if (t <= 0) {
            t = 20 * (35 + RNG.nextInt(70));
            spawnWatcher(player.serverLevel(), player);
        }
        data.putInt(WATCH, t);
    }

    public static boolean spawnWatcher(ServerLevel level, ServerPlayer player) {
        if (!level.getEntitiesOfClass(WatcherEntity.class, player.getBoundingBox().inflate(96.0D)).isEmpty()) {
            return false;
        }
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = RNG.nextDouble() * Math.PI * 2.0D;
            double dist = 24.0D + RNG.nextDouble() * 16.0D;
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * dist);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);
            int y = DreamChunkGenerator.height(DreamChunkGenerator.Style.WONDERLAND, x, z) + 1;
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                    || !level.getBlockState(pos.above(2)).isAir()) {
                continue;
            }
            WatcherEntity watcher = ModRegistry.WATCHER.get().create(level);
            if (watcher == null) {
                return false;
            }
            float yaw = (float) (Math.atan2(player.getZ() - z, player.getX() - x) * (180.0D / Math.PI)) - 90.0F;
            watcher.moveTo(x + 0.5D, y, z + 0.5D, yaw, 0.0F);
            watcher.yHeadRot = yaw;
            watcher.yBodyRot = yaw;
            level.addFreshEntity(watcher);
            return true;
        }
        return false;
    }

    private Journey() {
    }
}

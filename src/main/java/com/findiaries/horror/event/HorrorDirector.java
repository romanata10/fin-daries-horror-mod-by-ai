package com.findiaries.horror.event;

import com.findiaries.horror.Config;
import com.findiaries.horror.FinDiaries;
import com.findiaries.horror.ModRegistry;
import com.findiaries.horror.entity.BlueStrangerEntity;
import com.findiaries.horror.entity.LongLegsEntity;
import com.findiaries.horror.network.Net;
import com.findiaries.horror.network.PhenomenonPacket;
import com.findiaries.horror.world.Structures;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

/** Decides when something happens to the player in the overworld, and makes it happen. */
public final class HorrorDirector {
    private static final Random RNG = new Random();
    private static final String NEXT = "fd_next_event";

    private record Task(long tick, Runnable run) {
    }

    private record Event(int weight, Function<ServerPlayer, Boolean> run) {
    }

    private static final List<Task> TASKS = new ArrayList<>();
    private static long ticks;

    private static final Map<String, Event> EVENTS = new LinkedHashMap<>();

    static {
        EVENTS.put("triangle", new Event(14, HorrorDirector::triangle));
        EVENTS.put("longlegs", new Event(16, HorrorDirector::longLegs));
        EVENTS.put("blue_stranger", new Event(14, HorrorDirector::blueStranger));
        EVENTS.put("slab", new Event(12, HorrorDirector::slab));
        EVENTS.put("mushrooms", new Event(10, HorrorDirector::mushrooms));
        EVENTS.put("tower", new Event(10, HorrorDirector::tower));
        EVENTS.put("ufo", new Event(8, HorrorDirector::ufo));
        EVENTS.put("portal", new Event(6, HorrorDirector::portal));
    }

    public static void later(int delay, Runnable run) {
        TASKS.add(new Task(ticks + delay, run));
    }

    /** Runs everything that was scheduled, right now (used by the self-test). */
    public static void flushTasks() {
        List<Task> all = new ArrayList<>(TASKS);
        TASKS.clear();
        for (Task t : all) {
            t.run.run();
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Forge events
    // ---------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        ticks++;
        if (!TASKS.isEmpty()) {
            List<Task> due = new ArrayList<>();
            for (Iterator<Task> it = TASKS.iterator(); it.hasNext(); ) {
                Task t = it.next();
                if (t.tick <= ticks) {
                    due.add(t);
                    it.remove();
                }
            }
            for (Task t : due) {
                try {
                    t.run.run();
                } catch (RuntimeException e) {
                    FinDiaries.LOGGER.error("Scheduled event failed", e);
                }
            }
        }
        for (ServerPlayer player : new ArrayList<>(server.getPlayerList().getPlayers())) {
            try {
                tickPlayer(player);
            } catch (RuntimeException e) {
                FinDiaries.LOGGER.error("Player tick failed", e);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (Boolean.getBoolean("findiaries.smoke") && event.getServer().isDedicatedServer()) {
            SmokeTest.run(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TASKS.clear();
        Journey.clear();
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("findiaries").requires(s -> s.hasPermission(2));
        for (String name : EVENTS.keySet()) {
            root.then(Commands.literal(name).executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                boolean ok = EVENTS.get(name).run.apply(player);
                ctx.getSource().sendSuccess(() -> Component.literal(name + (ok ? " ..." : ": no suitable place nearby")), false);
                return ok ? 1 : 0;
            }));
        }
        root.then(Commands.literal("watcher").executes(ctx -> {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            return Journey.spawnWatcher(player.serverLevel(), player) ? 1 : 0;
        }));
        root.then(Commands.literal("danger_12").executes(ctx -> {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            return Journey.spawnDanger(player.serverLevel(), player) ? 1 : 0;
        }));
        LiteralArgumentBuilder<CommandSourceStack> tp = Commands.literal("tp");
        for (Journey.Dest dest : Journey.Dest.values()) {
            String name = dest == Journey.Dest.ALIVE ? "overworld" : dest.name().toLowerCase(java.util.Locale.ROOT);
            tp.then(Commands.literal(name).executes(ctx -> {
                Journey.go(ctx.getSource().getPlayerOrException(), dest);
                return 1;
            }));
        }
        root.then(tp);
        event.getDispatcher().register(root);
    }

    // ---------------------------------------------------------------------------------------------

    private static int interval() {
        int min = Config.MIN_SECONDS.get();
        int max = Math.max(min, Config.MAX_SECONDS.get());
        return 20 * (min + RNG.nextInt(max - min + 1));
    }

    private static void tickPlayer(ServerPlayer player) {
        if (!player.isAlive()) {
            return;
        }
        Journey.tick(player);
        if (player.isSpectator() || player.level().dimension() != Level.OVERWORLD) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        if (!data.contains(NEXT)) {
            data.putInt(NEXT, interval());
            return;
        }
        int next = data.getInt(NEXT) - 1;
        if (next <= 0) {
            fireRandom(player);
            next = interval();
        }
        data.putInt(NEXT, next);
    }

    public static List<String> eventNames() {
        return new ArrayList<>(EVENTS.keySet());
    }

    public static boolean runEvent(String name, ServerPlayer player) {
        Event e = EVENTS.get(name);
        return e != null && e.run.apply(player);
    }

    public static void fireRandom(ServerPlayer player) {
        List<Event> pool = new ArrayList<>(EVENTS.values());
        while (!pool.isEmpty()) {
            int total = 0;
            for (Event e : pool) {
                total += e.weight;
            }
            int roll = RNG.nextInt(total);
            Event chosen = pool.get(0);
            for (Event e : pool) {
                roll -= e.weight;
                if (roll < 0) {
                    chosen = e;
                    break;
                }
            }
            if (chosen.run.apply(player)) {
                return;
            }
            pool.remove(chosen);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------------------------------

    /** First free Y above the ground (leaves ignored). Loads the chunk if needed. */
    private static int surfaceY(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    private static void soundAll(ServerLevel level, ServerPlayer source, SoundEvent sound, float volume, float pitch) {
        source.playNotifySound(sound, SoundSource.AMBIENT, volume, pitch);
        for (ServerPlayer other : level.players()) {
            if (other != source && other.distanceToSqr(source) < 300.0D * 300.0D) {
                other.playNotifySound(sound, SoundSource.AMBIENT, volume, pitch);
            }
        }
    }

    /** Horizontal direction the player looks in, turned by up to +-spread radians. */
    private static double viewAngle(ServerPlayer player, double spread) {
        Vec3 look = player.getLookAngle();
        return Math.atan2(look.z, look.x) + (RNG.nextDouble() * 2.0D - 1.0D) * spread;
    }

    private static boolean standable(ServerLevel level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.isFaceSturdy(level, pos.below(), Direction.UP)
                && level.getFluidState(pos).isEmpty()
                && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
    }

    // ---------------------------------------------------------------------------------------------
    // 1. the giant black triangle
    // ---------------------------------------------------------------------------------------------

    private static boolean triangle(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        float angle = RNG.nextFloat() * (float) (Math.PI * 2.0D);
        double dx = -Math.sin(angle), dz = Math.cos(angle);
        double y = Math.max(250.0D, player.getY() + 150.0D);
        Net.toLevel(level, new PhenomenonPacket(PhenomenonPacket.TRIANGLE,
                player.getX() - dx * 1800.0D, y, player.getZ() - dz * 1800.0D, angle, RNG.nextLong()));
        soundAll(level, player, ModRegistry.CHIRP.get(), 1.0F, 1.0F);
        return true;
    }

    // ---------------------------------------------------------------------------------------------
    // 2. kill000145236 behind a tree
    // ---------------------------------------------------------------------------------------------

    private static boolean longLegs(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!level.getEntitiesOfClass(LongLegsEntity.class, player.getBoundingBox().inflate(96.0D)).isEmpty()) {
            return false;
        }
        int px = player.getBlockX(), pz = player.getBlockZ();
        List<BlockPos> trunks = new ArrayList<>();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -34; dx <= 34; dx++) {
            for (int dz = -34; dz <= 34; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 < 13 * 13 || d2 > 34 * 34) {
                    continue;
                }
                int x = px + dx, z = pz + dz;
                if (!level.hasChunk(x >> 4, z >> 4)) {
                    continue;
                }
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (!level.getBlockState(m.set(x, top - 1, z)).is(BlockTags.LOGS)) {
                    continue;
                }
                int y = top - 1;
                while (y > level.getMinBuildHeight() && level.getBlockState(m.set(x, y - 1, z)).is(BlockTags.LOGS)) {
                    y--;
                }
                if (top - y >= 3 && Math.abs(y - player.getBlockY()) < 14) {
                    trunks.add(new BlockPos(x, y, z));
                }
            }
        }
        while (!trunks.isEmpty()) {
            BlockPos trunk = trunks.remove(RNG.nextInt(trunks.size()));
            // one step behind the trunk, as seen from the player
            double ax = trunk.getX() + 0.5D - player.getX(), az = trunk.getZ() + 0.5D - player.getZ();
            double len = Math.sqrt(ax * ax + az * az);
            double sx = trunk.getX() + 0.5D + ax / len * 1.15D, sz = trunk.getZ() + 0.5D + az / len * 1.15D;
            BlockPos stand = BlockPos.containing(sx, trunk.getY(), sz);
            for (int dy = 2; dy >= -2; dy--) {
                BlockPos p = stand.above(dy);
                if (standable(level, p)) {
                    LongLegsEntity legs = ModRegistry.LONGLEGS.get().create(level);
                    if (legs == null) {
                        return false;
                    }
                    float yaw = (float) (Math.atan2(-az, -ax) * (180.0D / Math.PI)) - 90.0F;
                    legs.moveTo(sx, p.getY(), sz, yaw, 0.0F);
                    legs.yHeadRot = yaw;
                    legs.yBodyRot = yaw;
                    level.addFreshEntity(legs);
                    player.playNotifySound(SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0F, 0.8F);
                    return true;
                }
            }
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------------
    // 3. the portal to wonderland.avi
    // ---------------------------------------------------------------------------------------------

    private static boolean portal(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = viewAngle(player, Math.PI);
            double dist = 14.0D + RNG.nextDouble() * 14.0D;
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * dist);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);
            int y = surfaceY(level, x, z);
            if (Math.abs(y - player.getBlockY()) > 12 || !standable(level, new BlockPos(x + 1, y, z))
                    || !level.getFluidState(new BlockPos(x + 2, y - 1, z)).isEmpty()) {
                continue;
            }
            Structures.buildWonderlandPortal(level, x, y, z);
            player.playNotifySound(SoundEvents.PORTAL_AMBIENT, SoundSource.AMBIENT, 0.8F, 0.5F);
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------------
    // 4. the dark slab with the lights / 9. the 1000 block thing
    // ---------------------------------------------------------------------------------------------

    private static boolean slab(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        float angle = RNG.nextFloat() * (float) (Math.PI * 2.0D);
        double dx = -Math.sin(angle), dz = Math.cos(angle);
        double side = (RNG.nextBoolean() ? 1 : -1) * (30.0D + RNG.nextDouble() * 50.0D);
        double y = Math.max(170.0D, player.getY() + 80.0D);
        Net.toLevel(level, new PhenomenonPacket(PhenomenonPacket.SLAB,
                player.getX() - dx * 600.0D + dz * side, y, player.getZ() - dz * 600.0D - dx * side, angle, RNG.nextLong()));
        soundAll(level, player, ModRegistry.HUM.get(), 0.6F, 1.3F);
        return true;
    }

    private static boolean ufo(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        float angle = RNG.nextFloat() * (float) (Math.PI * 2.0D);
        double dx = -Math.sin(angle), dz = Math.cos(angle);
        double side = (RNG.nextBoolean() ? 1 : -1) * (380.0D + RNG.nextDouble() * 200.0D);
        double y = Math.max(330.0D, player.getY() + 230.0D);
        Net.toLevel(level, new PhenomenonPacket(PhenomenonPacket.UFO,
                player.getX() - dx * 2400.0D + dz * side, y, player.getZ() - dz * 2400.0D - dx * side, angle, RNG.nextLong()));
        soundAll(level, player, ModRegistry.HUM.get(), 1.0F, 0.7F);
        return true;
    }

    // ---------------------------------------------------------------------------------------------
    // 5. giant mushrooms on legs, then the ring
    // ---------------------------------------------------------------------------------------------

    private static boolean mushrooms(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        double angle = viewAngle(player, 0.6D);
        double x = player.getX() + Math.cos(angle) * 135.0D;
        double z = player.getZ() + Math.sin(angle) * 135.0D;
        double y = player.getY();
        if (level.hasChunk(((int) x) >> 4, ((int) z) >> 4)) {
            y = Math.min(y, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) x, (int) z));
        }
        Net.toLevel(level, new PhenomenonPacket(PhenomenonPacket.WALKERS, x, y, z, 0.0F, RNG.nextLong()));
        later(140, () -> mushroomRing(player));
        return true;
    }

    private static void mushroomRing(ServerPlayer player) {
        if (player.hasDisconnected() || !player.isAlive()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        int n = 18;
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2.0D * i / n;
            int x = (int) Math.floor(player.getX() + Math.cos(a) * 4.2D);
            int z = (int) Math.floor(player.getZ() + Math.sin(a) * 4.2D);
            for (int dy = 3; dy >= -4; dy--) {
                BlockPos pos = new BlockPos(x, player.getBlockY() + dy, z);
                if (!level.getBlockState(pos).canBeReplaced() || !level.getFluidState(pos).isEmpty()) {
                    continue;
                }
                BlockPos below = pos.below();
                BlockState ground = level.getBlockState(below);
                if (!ground.isFaceSturdy(level, below, Direction.UP)) {
                    continue;
                }
                if (ground.is(BlockTags.DIRT)) {
                    level.setBlock(below, Blocks.MYCELIUM.defaultBlockState(), 2);
                }
                level.setBlock(pos, (i % 2 == 0 ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM).defaultBlockState(), 2);
                break;
            }
        }
        player.playNotifySound(SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0F, 0.6F);
    }

    // ---------------------------------------------------------------------------------------------
    // 6. the rising tower that leaves a cobblestone pillar
    // ---------------------------------------------------------------------------------------------

    private static boolean tower(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = viewAngle(player, 0.9D);
            double dist = 30.0D + RNG.nextDouble() * 16.0D;
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * dist);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);
            int y = surfaceY(level, x, z);
            if (Math.abs(y - player.getBlockY()) > 24) {
                continue;
            }
            Net.toLevel(level, new PhenomenonPacket(PhenomenonPacket.TOWER, x + 0.5D, y, z + 0.5D, 0.0F, RNG.nextLong()));
            soundAll(level, player, ModRegistry.HUM.get(), 1.0F, 1.0F);
            final int fx = x, fy = y, fz = z;
            later(260, () -> {
                int top = Math.min(fy + 50, level.getMaxBuildHeight());
                for (int yy = fy; yy < top; yy++) {
                    level.setBlock(new BlockPos(fx, yy, fz), Blocks.COBBLESTONE.defaultBlockState(), 3);
                }
                level.playSound(null, new BlockPos(fx, fy, fz), SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 3.0F, 0.5F);
            });
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------------
    // 7. the blue stranger
    // ---------------------------------------------------------------------------------------------

    private static boolean blueStranger(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!level.getEntitiesOfClass(BlueStrangerEntity.class, player.getBoundingBox().inflate(96.0D)).isEmpty()) {
            return false;
        }
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = viewAngle(player, 1.2D);
            double dist = 10.0D + RNG.nextDouble() * 9.0D;
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * dist);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);
            int y = surfaceY(level, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (Math.abs(y - player.getBlockY()) > 8 || !standable(level, pos)) {
                continue;
            }
            BlueStrangerEntity stranger = ModRegistry.BLUE_STRANGER.get().create(level);
            if (stranger == null) {
                return false;
            }
            float yaw = (float) (Math.atan2(player.getZ() - z, player.getX() - x) * (180.0D / Math.PI)) - 90.0F;
            stranger.moveTo(x + 0.5D, y, z + 0.5D, yaw, 0.0F);
            stranger.yHeadRot = yaw;
            stranger.yBodyRot = yaw;
            level.addFreshEntity(stranger);
            player.playNotifySound(SoundEvents.AMBIENT_UNDERWATER_ENTER, SoundSource.AMBIENT, 1.0F, 0.7F);
            player.playNotifySound(SoundEvents.PLAYER_SPLASH, SoundSource.AMBIENT, 1.0F, 0.5F);
            return true;
        }
        return false;
    }

    private HorrorDirector() {
    }
}

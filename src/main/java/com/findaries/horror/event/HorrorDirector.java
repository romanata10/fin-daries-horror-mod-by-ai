package com.findaries.horror.event;

import com.findaries.horror.Config;
import com.findaries.horror.FinDaries;
import com.findaries.horror.ModRegistry;
import com.findaries.horror.entity.BlueStrangerEntity;
import com.findaries.horror.entity.CaveDemonEntity;
import com.findaries.horror.entity.EyeObjectEntity;
import com.findaries.horror.entity.FakePlayerEntity;
import com.findaries.horror.entity.NevelimEntity;
import com.findaries.horror.entity.OneEyeGiantEntity;
import com.findaries.horror.entity.PillarEntity;
import com.findaries.horror.entity.FlowerManEntity;
import com.findaries.horror.entity.LongLegsEntity;
import com.findaries.horror.entity.SheEntity;
import com.findaries.horror.entity.SkyPersonEntity;
import com.findaries.horror.entity.WandererEntity;
import com.findaries.horror.world.FlowerFields;
import com.findaries.horror.world.ModWorldData;
import net.minecraft.world.entity.Entity;
import com.findaries.horror.network.Net;
import com.findaries.horror.network.PhenomenonPacket;
import com.findaries.horror.world.Structures;
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
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
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
import java.util.function.Supplier;

/** Decides when something happens to the player in the overworld, and makes it happen. */
public final class HorrorDirector {
    private static final Random RNG = new Random();
    private static final String NEXT = "fd_next_event";
    private static final String CASTLE = "fd_castle";
    private static final String FLOWER = "fd_flower";
    private static final String CAVE = "fd_cave";

    private record Task(long tick, Runnable run) {
    }

    private record Event(int weight, Function<ServerPlayer, Boolean> run) {
    }

    private static final List<Task> TASKS = new ArrayList<>();
    private static long ticks;

    private static final Map<String, Event> EVENTS = new LinkedHashMap<>();

    static {
        EVENTS.put("triangle", new Event(16, HorrorDirector::triangle));
        EVENTS.put("longlegs", new Event(16, HorrorDirector::longLegs));
        EVENTS.put("blue_stranger", new Event(14, HorrorDirector::blueStranger));
        EVENTS.put("slab", new Event(12, HorrorDirector::slab));
        EVENTS.put("mushrooms", new Event(10, HorrorDirector::mushrooms));
        EVENTS.put("tower", new Event(10, HorrorDirector::tower));
        EVENTS.put("ufo", new Event(8, HorrorDirector::ufo));
        EVENTS.put("portal", new Event(6, HorrorDirector::portal));
        EVENTS.put("spaceship", new Event(6, HorrorDirector::spaceship));
        EVENTS.put("hell_portal", new Event(5, HorrorDirector::hellPortal));
        EVENTS.put("castle", new Event(4, HorrorDirector::castle));
        EVENTS.put("wrong_house", new Event(4, HorrorDirector::wrongHouse));
        // weight 0: never picked at random on their own, only through "triangle", the flower fields or by hand
        EVENTS.put("triangle_wanderer", new Event(0, HorrorDirector::triangleWanderer));
        EVENTS.put("triangle_lying", new Event(0, p -> flyTriangle(p, PhenomenonPacket.TRIANGLE_LYING)));
        EVENTS.put("triangle_star", new Event(0, p -> flyTriangle(p, PhenomenonPacket.TRIANGLE_STAR)));
        EVENTS.put("triangle_eyes", new Event(0, HorrorDirector::triangleEyes));
        EVENTS.put("flower_man", new Event(0, HorrorDirector::flowerMan));
        EVENTS.put("pillar", new Event(5, HorrorDirector::pillar));
        EVENTS.put("hole", new Event(5, HorrorDirector::hole));
        EVENTS.put("cave_demon", new Event(3, HorrorDirector::caveDemon));
        EVENTS.put("eye", new Event(4, HorrorDirector::eye));
        // weight 0: only by hand, from a biome / a cave, or extremely rarely
        EVENTS.put("nevelim", new Event(0, HorrorDirector::nevelim));
        EVENTS.put("giant_nevelim", new Event(0, HorrorDirector::giantNevelim));
        EVENTS.put("fake_player", new Event(0, HorrorDirector::fakePlayer));
        EVENTS.put("one_eye_giant", new Event(0, HorrorDirector::oneEyeGiant));
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
                    FinDaries.LOGGER.error("Scheduled event failed", e);
                }
            }
        }
        for (ServerPlayer player : new ArrayList<>(server.getPlayerList().getPlayers())) {
            try {
                tickPlayer(player);
            } catch (RuntimeException e) {
                FinDaries.LOGGER.error("Player tick failed", e);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (Boolean.getBoolean("findaries.smoke") && event.getServer().isDedicatedServer()) {
            SmokeTest.run(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TASKS.clear();
        Journey.clear();
        FlowerFields.clear();
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        // /findaries and the short form /fd. Always available in singleplayer, operators only on servers.
        for (String rootName : new String[]{"findaries", "fd"}) {
            LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(rootName)
                    .requires(s -> s.hasPermission(2) || !s.getServer().isDedicatedServer());

            // /fd summon <entity> - puts the entity a few blocks in front of you
            LiteralArgumentBuilder<CommandSourceStack> summon = Commands.literal("summon");
            Map<String, Supplier<? extends EntityType<? extends Mob>>> entities = new LinkedHashMap<>();
            entities.put("kill000145236", ModRegistry.LONGLEGS);
            entities.put("longlegs", ModRegistry.LONGLEGS);
            entities.put("watcher", ModRegistry.WATCHER);
            entities.put("danger_12", ModRegistry.DANGER_12);
            entities.put("blue_stranger", ModRegistry.BLUE_STRANGER);
            entities.put("wanderer", ModRegistry.WANDERER);
            entities.put("she", ModRegistry.SHE);
            entities.put("flower_man", ModRegistry.FLOWER_MAN);
            entities.put("red_demon", ModRegistry.RED_DEMON);
            entities.put("sky_person", ModRegistry.SKY_PERSON);
            entities.put("pillar", ModRegistry.PILLAR);
            entities.put("fake_player", ModRegistry.FAKE_PLAYER);
            entities.put("cave_demon", ModRegistry.CAVE_DEMON);
            entities.put("one_eye_giant", ModRegistry.ONE_EYE_GIANT);
            entities.put("nevelim", ModRegistry.NEVELIM);
            entities.put("eye", ModRegistry.EYE);
            entities.forEach((name, type) -> summon.then(Commands.literal(name).executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                boolean ok = summon(player, type.get());
                ctx.getSource().sendSuccess(() -> Component.literal(ok ? name + " is here." : "Could not summon " + name), false);
                return ok ? 1 : 0;
            })));
            root.then(summon);

            // /fd event <name> - starts one of the random events right now
            LiteralArgumentBuilder<CommandSourceStack> events = Commands.literal("event");
            for (String name : EVENTS.keySet()) {
                events.then(Commands.literal(name).executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    boolean ok = EVENTS.get(name).run.apply(player);
                    ctx.getSource().sendSuccess(() -> Component.literal(name + (ok ? " ..." : ": no suitable place nearby")), false);
                    return ok ? 1 : 0;
                }));
            }
            events.then(Commands.literal("random").executes(ctx -> {
                fireRandom(ctx.getSource().getPlayerOrException());
                return 1;
            }));
            root.then(events);

            // /fd tp <world>
            LiteralArgumentBuilder<CommandSourceStack> tp = Commands.literal("tp");
            for (Journey.Dest dest : Journey.Dest.values()) {
                String name = switch (dest) {
                    case ALIVE -> "overworld";
                    case WONDERDREAM -> "the_wonderdream";
                    default -> dest.name().toLowerCase(java.util.Locale.ROOT);
                };
                tp.then(Commands.literal(name).executes(ctx -> {
                    Journey.go(ctx.getSource().getPlayerOrException(), dest);
                    return 1;
                }));
            }
            tp.then(Commands.literal("house").executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                ServerLevel level = player.server.getLevel(Journey.HALLWAY);
                if (level == null) {
                    return 0;
                }
                Structures.ensureHallway(level);
                player.teleportTo(level, 0.5D, Structures.HALL_Y + 1, Structures.HOUSE_Z + 0.8D, 0.0F, 0.0F);
                return 1;
            }));
            tp.then(Commands.literal("flowers").executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                ServerLevel overworld = player.server.overworld();
                BlockPos field = FlowerFields.nearest(overworld, player.getBlockX(), player.getBlockZ());
                if (field == null) {
                    return 0;
                }
                int y = surfaceY(overworld, field.getX(), field.getZ());
                player.teleportTo(overworld, field.getX() + 0.5D, y, field.getZ() + 0.5D, player.getYRot(), 0.0F);
                return 1;
            }));
            root.then(tp);
            event.getDispatcher().register(root);
        }
    }

    /** Puts an entity of the mod about ten blocks in front of the player. */
    public static boolean summon(ServerPlayer player, EntityType<? extends Mob> type) {
        ServerLevel level = player.serverLevel();
        Mob mob = type.create(level);
        if (mob == null) {
            return false;
        }
        double angle = viewAngle(player, 0.0D);
        double x = player.getX() + Math.cos(angle) * 10.0D;
        double z = player.getZ() + Math.sin(angle) * 10.0D;
        double y = player.getY();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dy = 6; dy >= -6; dy--) {
            p.set(Mth.floor(x), player.getBlockY() + dy, Mth.floor(z));
            if (standable(level, p)) {
                y = p.getY();
                break;
            }
        }
        float yaw = (float) (Math.atan2(player.getZ() - z, player.getX() - x) * (180.0D / Math.PI)) - 90.0F;
        mob.moveTo(x, y, z, yaw, 0.0F);
        mob.yHeadRot = yaw;
        mob.yBodyRot = yaw;
        return level.addFreshEntity(mob);
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
        tickPlaces(player, data);
        tickRare(player, data);
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

    /** The cobblestone castle (She) and the flower fields (Flower Man). */
    private static void tickPlaces(ServerPlayer player, CompoundTag data) {
        ServerLevel level = player.serverLevel();
        // two minutes inside a castle and She comes
        int inCastle = data.getInt(CASTLE);
        if (ModWorldData.get(level.getServer()).castleAt(player.getX(), player.getY(), player.getZ()) != null) {
            inCastle++;
            if (inCastle >= 20 * 120) {
                spawnShe(player);
                inCastle = -20 * 60 * 5;
            }
        } else if (inCastle > 0) {
            inCastle = 0;
        } else if (inCastle < 0) {
            inCastle++;
        }
        data.putInt(CASTLE, inCastle);

        if (player.tickCount % 40 == 0) {
            FlowerFields.tickAround(level, player.getBlockX(), player.getBlockZ());
        }
        if (FlowerFields.inside(level, player.getX(), player.getZ())) {
            int next = data.contains(FLOWER) ? data.getInt(FLOWER) - 1 : 20 * (20 + RNG.nextInt(40));
            if (next <= 0) {
                flowerMan(player);
                next = 20 * (45 + RNG.nextInt(75));
            }
            data.putInt(FLOWER, next);
        }
    }


    // ---------------------------------------------------------------------------------------------
    // the_wonderdream, stonehell and the new watchers
    // ---------------------------------------------------------------------------------------------

    /** Wrong house: the doors stay shut, and going upstairs throws you into the_wonderdream. */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || !(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof DoorBlock)) {
            return;
        }
        BlockPos pos = event.getPos();
        if (ModWorldData.get(event.getLevel().getServer()).wrongHouseAt(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D) != null) {
            event.setCanceled(true);
        }
    }

    private static void tickRare(ServerPlayer player, CompoundTag data) {
        ServerLevel level = player.serverLevel();
        // the doors that will not open, and the upstairs trap
        BlockPos house = ModWorldData.get(level.getServer()).wrongHouseAt(player.getX(), player.getY(), player.getZ());
        if (house != null && player.getY() >= house.getY() + 5) {
            Journey.go(player, Journey.Dest.WONDERDREAM);
            return;
        }
        if (RNG.nextInt(20 * 60 * 60) == 0) { // the 860 block Nevelim: about once an hour at most
            giantNevelim(player);
        }
        if (player.tickCount % 200 != 0) {
            return;
        }
        boolean darkForest = level.getBiome(player.blockPosition()).unwrapKey()
                .filter(key -> key == Biomes.DARK_FOREST).isPresent();
        if (darkForest && RNG.nextInt(6) == 0
                && level.getEntitiesOfClass(NevelimEntity.class, player.getBoundingBox().inflate(90.0D)).isEmpty()) {
            nevelim(player);
        }
        boolean underground = player.getY() < surfaceY(level, player.getBlockX(), player.getBlockZ()) - 3;
        if (underground) {
            int t = data.getInt(CAVE) + 1;
            if (t >= 3 && level.getEntitiesOfClass(CaveDemonEntity.class, player.getBoundingBox().inflate(90.0D)).isEmpty()) {
                t = 0;
                caveDemon(player);
            }
            data.putInt(CAVE, t);
        }
    }

    private static boolean pillar(ServerPlayer player) {
        return spawnSimple(player, ModRegistry.PILLAR.get(), 12.0D, 20.0D, 6);
    }

    private static boolean spawnSimple(ServerPlayer player, EntityType<? extends Mob> type, double minDist, double maxDist, int maxSlope) {
        ServerLevel level = player.serverLevel();
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = viewAngle(player, 1.2D);
            double dist = minDist + RNG.nextDouble() * (maxDist - minDist);
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * dist);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);
            int y = surfaceY(level, x, z);
            if (Math.abs(y - player.getBlockY()) > maxSlope || !standable(level, new BlockPos(x, y, z))) {
                continue;
            }
            Mob mob = type.create(level);
            if (mob == null) {
                return false;
            }
            float yaw = (float) (Math.atan2(player.getZ() - z, player.getX() - x) * (180.0D / Math.PI)) - 90.0F;
            mob.moveTo(x + 0.5D, y, z + 0.5D, yaw, 0.0F);
            mob.yHeadRot = yaw;
            mob.yBodyRot = yaw;
            level.addFreshEntity(mob);
            player.playNotifySound(SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0F, 0.5F);
            return true;
        }
        return false;
    }

    private static boolean hole(ServerPlayer player) {
        BlockPos pos = site(player, 14.0D, 28.0D, 3, 3);
        if (pos == null) {
            return false;
        }
        Structures.buildHole(player.serverLevel(), pos.getX(), pos.getY(), pos.getZ());
        player.playNotifySound(SoundEvents.PORTAL_AMBIENT, SoundSource.AMBIENT, 0.8F, 0.35F);
        return true;
    }

    private static boolean caveDemon(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!level.getEntitiesOfClass(CaveDemonEntity.class, player.getBoundingBox().inflate(96.0D)).isEmpty()) {
            return false;
        }
        CaveDemonEntity demon = ModRegistry.CAVE_DEMON.get().create(level);
        if (demon == null) {
            return false;
        }
        double a = viewAngle(player, Math.PI);
        demon.moveTo(player.getX() + Math.cos(a) * 17.0D, player.getY(), player.getZ() + Math.sin(a) * 17.0D, 0.0F, 0.0F);
        level.addFreshEntity(demon);
        player.playNotifySound(SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 2.0F, 0.4F);
        player.playNotifySound(ModRegistry.SCREAM.get(), SoundSource.HOSTILE, 2.0F, 1.3F);
        return true;
    }

    private static boolean nevelim(ServerPlayer player) {
        return spawnSimple(player, ModRegistry.NEVELIM.get(), 18.0D, 30.0D, 8);
    }

    private static boolean oneEyeGiant(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        OneEyeGiantEntity giant = ModRegistry.ONE_EYE_GIANT.get().create(level);
        if (giant == null) {
            return false;
        }
        giant.moveTo(player.getX() + 20.0D, player.getY(), player.getZ(), 0.0F, 0.0F);
        level.addFreshEntity(giant);
        player.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 2.0F, 0.5F);
        return true;
    }

    private static boolean fakePlayer(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < 2; i++) {
            FakePlayerEntity copy = ModRegistry.FAKE_PLAYER.get().create(level);
            if (copy == null) {
                return false;
            }
            double a = viewAngle(player, 0.8D);
            copy.moveTo(player.getX() + Math.cos(a) * 13.0D, player.getY(), player.getZ() + Math.sin(a) * 13.0D, 0.0F, 0.0F);
            copy.impersonate(player.getGameProfile().getName());
            level.addFreshEntity(copy);
        }
        return true;
    }

    /** The Eye object comes out from behind a tree, then keeps fifteen blocks away. */
    private static boolean eye(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!level.getEntitiesOfClass(EyeObjectEntity.class, player.getBoundingBox().inflate(96.0D)).isEmpty()) {
            return false;
        }
        int px = player.getBlockX(), pz = player.getBlockZ();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int attempt = 0; attempt < 160; attempt++) {
            int x = px + RNG.nextInt(49) - 24, z = pz + RNG.nextInt(49) - 24;
            int d2 = (x - px) * (x - px) + (z - pz) * (z - pz);
            if (d2 < 14 * 14 || d2 > 24 * 24 || !level.hasChunk(x >> 4, z >> 4)) {
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
            if (top - y < 3) {
                continue;
            }
            double ax = x + 0.5D - player.getX(), az = z + 0.5D - player.getZ();
            double len = Math.max(0.001D, Math.sqrt(ax * ax + az * az));
            double sx = x + 0.5D + ax / len * 1.3D, sz = z + 0.5D + az / len * 1.3D;
            EyeObjectEntity eye = ModRegistry.EYE.get().create(level);
            if (eye == null) {
                return false;
            }
            eye.moveTo(sx, y, sz, 0.0F, 0.0F);
            level.addFreshEntity(eye);
            return true;
        }
        return false;
    }

    /** The 860 block tall Nevelim: the client draws it, the server erases a sphere of blocks. */
    private static boolean giantNevelim(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        double a = viewAngle(player, 0.5D);
        double x = player.getX() + Math.cos(a) * 170.0D, z = player.getZ() + Math.sin(a) * 170.0D;
        int y = surfaceY(level, (int) x, (int) z);
        Net.toLevel(level, new PhenomenonPacket(PhenomenonPacket.GIANT_NEVELIM, x, y, z, 0.0F, RNG.nextLong()));
        soundAll(level, player, ModRegistry.HUM.get(), 1.6F, 0.35F);
        later(215, () -> blastSphere(player, (int) x, y, (int) z, 250, 0));
        return true;
    }

    /** Removes a spherical bowl of blocks in small bands so the server never freezes. */
    private static void blastSphere(ServerPlayer player, int cx, int cy, int cz, int radius, int band) {
        if (player.hasDisconnected() || player.level().dimension() != Level.OVERWORLD || band >= 16) {
            return;
        }
        ServerLevel level = player.serverLevel();
        final int step = 3;
        int bandSize = (2 * radius / 16) + 1;
        int z0 = cz - radius + band * bandSize;
        int z1 = Math.min(cz + radius, z0 + bandSize);
        for (int z = z0; z < z1; z += step) {
            for (int x = cx - radius; x <= cx + radius; x += step) {
                if (!level.hasChunk(x >> 4, z >> 4)) {
                    continue; // only loaded chunks: no terrain generation, no server freeze
                }
                int dx = x - cx, dz = z - cz;
                long d2 = (long) dx * dx + (long) dz * dz;
                if (d2 > (long) radius * radius) {
                    continue;
                }
                int down = (int) Math.min(22.0D, Math.sqrt((double) (radius * radius - d2)) * 0.09D);
                for (int y = cy - down; y <= cy + 9; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || state.is(Blocks.BEDROCK) || state.hasBlockEntity()
                            || state.getDestroySpeed(level, pos) < 0.0F) {
                        continue;
                    }
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
                }
            }
        }
        level.playSound(null, new BlockPos(cx, cy, cz), SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 4.0F, 0.3F);
        later(3, () -> blastSphere(player, cx, cy, cz, radius, band + 1));
    }

    public static boolean spawnShe(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        SheEntity she = ModRegistry.SHE.get().create(level);
        if (she == null) {
            return false;
        }
        double angle = viewAngle(player, 0.5D) + Math.PI; // behind your back
        double x = player.getX() + Math.cos(angle) * 22.0D, z = player.getZ() + Math.sin(angle) * 22.0D;
        she.moveTo(x, player.getY(), z, 0.0F, 0.0F);
        she.setTarget(player);
        level.addFreshEntity(she);
        player.playNotifySound(SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 1.0F, 0.5F);
        player.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 2.0F, 0.8F);
        return true;
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
            if (total <= 0) {
                return;
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

    /** One of the four kinds of triangle. The ordinary one sometimes brings the Wanderer. */
    private static boolean triangle(ServerPlayer player) {
        int roll = RNG.nextInt(100);
        if (roll < 40) {
            return flyTriangle(player, PhenomenonPacket.TRIANGLE);
        } else if (roll < 55) {
            return triangleWanderer(player);
        } else if (roll < 70) {
            return flyTriangle(player, PhenomenonPacket.TRIANGLE_LYING);
        } else if (roll < 85) {
            return flyTriangle(player, PhenomenonPacket.TRIANGLE_STAR);
        }
        return triangleEyes(player);
    }

    private static boolean flyTriangle(ServerPlayer player, int kind) {
        ServerLevel level = player.serverLevel();
        float angle = RNG.nextFloat() * (float) (Math.PI * 2.0D);
        double dx = -Math.sin(angle), dz = Math.cos(angle);
        double y = Math.max(250.0D, player.getY() + 150.0D);
        Net.toLevel(level, new PhenomenonPacket(kind,
                player.getX() - dx * 1800.0D, y, player.getZ() - dz * 1800.0D, angle, RNG.nextLong()));
        soundAll(level, player, ModRegistry.CHIRP.get(), 1.0F, kind == PhenomenonPacket.TRIANGLE_STAR ? 0.8F : 1.0F);
        return true;
    }

    /** The Wanderer stands with his arms out, waves at the triangle and is gone when it is gone. */
    private static boolean triangleWanderer(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        flyTriangle(player, PhenomenonPacket.TRIANGLE);
        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = viewAngle(player, 0.9D);
            double dist = 22.0D + RNG.nextDouble() * 16.0D;
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * dist);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);
            int y = surfaceY(level, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (Math.abs(y - player.getBlockY()) > 10 || !standable(level, pos)) {
                continue;
            }
            WandererEntity wanderer = ModRegistry.WANDERER.get().create(level);
            if (wanderer == null) {
                break;
            }
            float yaw = (float) (Math.atan2(player.getZ() - z, player.getX() - x) * (180.0D / Math.PI)) - 90.0F;
            wanderer.moveTo(x + 0.5D, y, z + 0.5D, yaw, 0.0F);
            wanderer.yHeadRot = yaw;
            wanderer.yBodyRot = yaw;
            level.addFreshEntity(wanderer);
            break;
        }
        return true;
    }

    /** The triangle with eyes looks at you and the ground around you goes five blocks up. */
    private static boolean triangleEyes(ServerPlayer player) {
        flyTriangle(player, PhenomenonPacket.TRIANGLE_EYES);
        later(60, () -> liftGround(player));
        return true;
    }

    private static void liftGround(ServerPlayer player) {
        if (player.hasDisconnected() || !player.isAlive() || player.level().dimension() != Level.OVERWORLD) {
            return;
        }
        ServerLevel level = player.serverLevel();
        final int radius = 10, up = 5;
        BlockPos c = player.blockPosition();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos from = new BlockPos.MutableBlockPos(), to = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = radius; dy >= -radius; dy--) { // top first, so every block has room above it
                    if (dx * dx + dy * dy + dz * dz > radius * radius) {
                        continue;
                    }
                    from.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    to.set(from.getX(), from.getY() + up, from.getZ());
                    if (from.getY() <= level.getMinBuildHeight() || to.getY() >= level.getMaxBuildHeight()) {
                        continue;
                    }
                    BlockState state = level.getBlockState(from);
                    if (state.isAir() || state.hasBlockEntity() || state.getDestroySpeed(level, from) < 0.0F) {
                        continue;
                    }
                    if (!level.getBlockState(to).canBeReplaced()) {
                        continue;
                    }
                    level.setBlock(to, state, 2 | 16);
                    level.setBlock(from, air, 2 | 16);
                }
            }
        }
        for (Entity e : level.getEntities((Entity) null, player.getBoundingBox().inflate(radius), other -> other.distanceToSqr(player) <= radius * radius)) {
            e.teleportTo(e.getX(), e.getY() + up, e.getZ());
        }
        level.playSound(null, c.above(up), SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 4.0F, 0.4F);
        player.playNotifySound(SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0F, 0.5F);
    }

    // ---------------------------------------------------------------------------------------------
    // structures of the second update
    // ---------------------------------------------------------------------------------------------

    /** Picks a dry place in front of the player; returns null if the ground is too uneven. */
    private static BlockPos site(ServerPlayer player, double minDist, double maxDist, int half, int maxSlope) {
        ServerLevel level = player.serverLevel();
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = viewAngle(player, 1.1D);
            double dist = minDist + RNG.nextDouble() * (maxDist - minDist);
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * dist);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);
            int y = surfaceY(level, x, z);
            if (Math.abs(y - player.getBlockY()) > 30 || !level.getFluidState(new BlockPos(x, y - 1, z)).isEmpty()) {
                continue;
            }
            int lo = y, hi = y;
            boolean wet = false;
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    int cy = surfaceY(level, x + sx * half, z + sz * half);
                    lo = Math.min(lo, cy);
                    hi = Math.max(hi, cy);
                    wet |= !level.getFluidState(new BlockPos(x + sx * half, cy - 1, z + sz * half)).isEmpty();
                }
            }
            if (wet || hi - lo > maxSlope) {
                continue;
            }
            return new BlockPos(x, y, z);
        }
        return null;
    }

    private static boolean castle(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (ModWorldData.get(level.getServer()).castleNear(player.getBlockX(), player.getBlockZ(), 400)) {
            return false;
        }
        BlockPos pos = site(player, 70.0D, 110.0D, Structures.CASTLE_HALF, 7);
        if (pos == null) {
            return false;
        }
        Structures.buildCastle(level, pos.getX(), pos.getY(), pos.getZ());
        return true;
    }

    private static boolean hellPortal(ServerPlayer player) {
        BlockPos pos = site(player, 40.0D, 75.0D, 3, 4);
        if (pos == null) {
            return false;
        }
        Structures.buildHellPortal(player.serverLevel(), pos.getX(), pos.getY(), pos.getZ());
        player.playNotifySound(SoundEvents.PORTAL_AMBIENT, SoundSource.AMBIENT, 0.6F, 0.4F);
        return true;
    }

    private static boolean wrongHouse(ServerPlayer player) {
        BlockPos pos = site(player, 55.0D, 95.0D, 4, 3);
        if (pos == null) {
            return false;
        }
        Structures.buildWrongHouse(player.serverLevel(), pos.getX(), pos.getY(), pos.getZ());
        return true;
    }

    /** A ship falls out of the sky and the Sky People climb out of it. */
    private static boolean spaceship(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = site(player, 35.0D, 55.0D, 6, 6);
        if (pos == null) {
            return false;
        }
        Net.toLevel(level, new PhenomenonPacket(PhenomenonPacket.SHIP, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, RNG.nextLong()));
        soundAll(level, player, ModRegistry.HUM.get(), 1.0F, 1.7F);
        later(100, () -> {
            level.explode(null, pos.getX() + 0.5D, pos.getY() + 2.0D, pos.getZ() + 0.5D, 3.0F, Level.ExplosionInteraction.NONE);
            Structures.buildShip(level, pos.getX(), pos.getY(), pos.getZ());
            int n = 2 + RNG.nextInt(2);
            for (int i = 0; i < n; i++) {
                SkyPersonEntity person = ModRegistry.SKY_PERSON.get().create(level);
                if (person == null) {
                    continue;
                }
                double a = Math.PI * 2.0D * i / n + RNG.nextDouble();
                int x = (int) Math.floor(pos.getX() + Math.cos(a) * 8.5D), z = (int) Math.floor(pos.getZ() + Math.sin(a) * 8.5D);
                person.moveTo(x + 0.5D, surfaceY(level, x, z), z + 0.5D, RNG.nextFloat() * 360.0F, 0.0F);
                level.addFreshEntity(person);
            }
        });
        return true;
    }

    /** Flower Man: only comes on its own in the flower fields. */
    private static boolean flowerMan(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!level.getEntitiesOfClass(FlowerManEntity.class, player.getBoundingBox().inflate(64.0D)).isEmpty()) {
            return false;
        }
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = viewAngle(player, 1.0D);
            double dist = 14.0D + RNG.nextDouble() * 12.0D;
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * dist);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);
            int y = surfaceY(level, x, z);
            if (Math.abs(y - player.getBlockY()) > 8 || !level.getFluidState(new BlockPos(x, y - 1, z)).isEmpty()) {
                continue;
            }
            FlowerManEntity man = ModRegistry.FLOWER_MAN.get().create(level);
            if (man == null) {
                return false;
            }
            float yaw = (float) (Math.atan2(player.getZ() - z, player.getX() - x) * (180.0D / Math.PI)) - 90.0F;
            man.moveTo(x + 0.5D, y, z + 0.5D, yaw, 0.0F);
            man.yHeadRot = yaw;
            man.yBodyRot = yaw;
            level.addFreshEntity(man);
            return true;
        }
        return false;
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

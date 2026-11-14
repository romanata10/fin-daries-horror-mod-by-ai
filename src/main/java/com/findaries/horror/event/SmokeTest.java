package com.findaries.horror.event;

import com.findaries.horror.FinDaries;
import com.findaries.horror.ModRegistry;
import com.findaries.horror.world.DreamChunkGenerator;
import com.findaries.horror.world.Structures;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.util.FakePlayerFactory;

import java.util.List;

/**
 * Headless self-test, enabled with -Dfindaries.smoke=true (CI: FD_SMOKE=1 ./gradlew runServer).
 * Generates the dimensions, builds the structures, spawns the entities and fires every event once.
 * Any failure throws and crashes the test server; CI looks for the final OK line.
 */
public final class SmokeTest {
    private static void expect(ServerLevel level, BlockPos pos, Block block, String what) {
        if (!level.getBlockState(pos).is(block)) {
            throw new IllegalStateException("SMOKE: expected " + what + " at " + pos + " in " + level.dimension().location()
                    + " but found " + level.getBlockState(pos));
        }
    }

    private static ServerLevel level(MinecraftServer server, ResourceKey<Level> key) {
        ServerLevel level = server.getLevel(key);
        if (level == null) {
            throw new IllegalStateException("SMOKE: dimension missing: " + key.location());
        }
        return level;
    }

    private static void load(ServerLevel level, int cx0, int cz0, int cx1, int cz1) {
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                level.getChunk(cx, cz);
            }
        }
    }

    public static void run(MinecraftServer server) {
        FinDaries.LOGGER.info("FINDARIES SMOKE: start");
        int g = DreamChunkGenerator.GROUND;

        ServerLevel wonderland = level(server, Journey.WONDERLAND);
        load(wonderland, -4, -4, 8, 5);
        Structures.ensureWonderland(wonderland);
        expect(wonderland, Structures.NOTE_CHEST, Blocks.CHEST, "note chest");
        expect(wonderland, new BlockPos(-1, g + 1, -6), ModRegistry.WONDERLAND_PORTAL.get(), "return portal");
        expect(wonderland, new BlockPos(DreamChunkGenerator.CABIN_X + 2, g + 1, 0), ModRegistry.HALL_PORTAL.get(), "cabin portal");
        expect(wonderland, new BlockPos(0, g, -2), Blocks.GRASS_BLOCK, "flat arrival ground");

        ServerLevel hallway = level(server, Journey.HALLWAY);
        Structures.ensureHallway(hallway);
        expect(hallway, new BlockPos(0, Structures.HALL_Y + 1, Structures.HOUSE_Z + 13), Blocks.IRON_DOOR, "lift door");
        expect(hallway, new BlockPos(0, Structures.HALL_Y + 1, Structures.LIFT_Z + 1), Blocks.AIR, "lift cabin");

        ServerLevel flowers = level(server, Journey.WONDERFLOWERS);
        load(flowers, -3, -2, 3, 16);
        expect(flowers, new BlockPos(0, g, 20), Blocks.DIRT_PATH, "path");
        expect(flowers, new BlockPos(0, g, 170), Blocks.OAK_PLANKS, "bridge");
        expect(flowers, new BlockPos(5, DreamChunkGenerator.WF_WATER, 170), Blocks.WATER, "river");
        expect(flowers, new BlockPos(0, g + 2, DreamChunkGenerator.ISLAND_Z + 3), ModRegistry.DOTS_PORTAL.get(), "dots portal");

        ServerLevel cage = level(server, Journey.CAGE);
        load(cage, -6, -6, 6, 6);
        expect(cage, new BlockPos(0, g + 2, DreamChunkGenerator.CAGE_R + 1), ModRegistry.WHITE_PORTAL.get(), "white portal");
        expect(cage, new BlockPos(0, g + 2, DreamChunkGenerator.CAGE_R), Blocks.AIR, "hole in the wall");
        expect(cage, new BlockPos(41, 200, DreamChunkGenerator.CAGE_R), Blocks.OAK_PLANKS, "wall");
        expect(cage, new BlockPos(0, g, 0), Blocks.GRASS_BLOCK, "arrival ground");
        int logs = 0, leaves = 0;
        for (int x = -70; x <= 70; x += 2) {
            for (int z = -70; z <= 70; z += 2) {
                if (cage.getBlockState(new BlockPos(x, 120, z)).is(Blocks.OAK_LOG)) {
                    logs++;
                }
                if (cage.getBlockState(new BlockPos(x, 175, z)).is(Blocks.OAK_LEAVES)) {
                    leaves++;
                }
            }
        }
        FinDaries.LOGGER.info("FINDARIES SMOKE: cage trunk samples={} leaf samples={}", logs, leaves);
        if (logs == 0) {
            throw new IllegalStateException("SMOKE: no giant trees in the cage");
        }

        ServerLevel hellway = level(server, Journey.HELLWAY);
        load(hellway, -1, 0, 1, 5);
        expect(hellway, new BlockPos(2, DreamChunkGenerator.HELL_FLOOR, 10), Blocks.AIR, "hellway tunnel");
        expect(hellway, new BlockPos(2, DreamChunkGenerator.HELL_FLOOR + DreamChunkGenerator.HELL_HEIGHT - 1, 10), Blocks.AIR, "hellway tunnel top");
        expect(hellway, new BlockPos(7, DreamChunkGenerator.HELL_FLOOR, DreamChunkGenerator.HELL_ROOM_Z + 2), Blocks.AIR, "hellway room");
        expect(hellway, new BlockPos(10, DreamChunkGenerator.HELL_FLOOR, DreamChunkGenerator.HELL_ROOM_Z + 2), ModRegistry.WONDERLAND_PORTAL.get(), "wonderland portal in the hellway");
        for (BlockPos p : List.of(new BlockPos(2, DreamChunkGenerator.HELL_FLOOR - 1, 10), new BlockPos(-1, DreamChunkGenerator.HELL_FLOOR + 5, 11),
                new BlockPos(DreamChunkGenerator.HELL_WIDTH, DreamChunkGenerator.HELL_FLOOR + 5, 11), new BlockPos(2, DreamChunkGenerator.HELL_FLOOR + DreamChunkGenerator.HELL_HEIGHT, 10))) {
            if (!hellway.getBlockState(p).is(ModRegistry.RED_COBBLESTONE.get()) && !hellway.getBlockState(p).is(ModRegistry.BLOOD_COBBLESTONE.get())) {
                throw new IllegalStateException("SMOKE: hellway wall missing at " + p + ": " + hellway.getBlockState(p));
            }
        }

        ServerLevel wonderdream = level(server, Journey.WONDERDREAM);
        load(wonderdream, -1, -1, 2, 2);
        expect(wonderdream, new BlockPos(8, g, 8), Blocks.OAK_PLANKS, "wonderdream floor");
        expect(wonderdream, new BlockPos(8, g + 1, 8), Blocks.AIR, "wonderdream corridor");
        expect(wonderdream, new BlockPos(8, g + 6, 8), Blocks.DARK_OAK_PLANKS, "wonderdream ceiling");
        expect(wonderdream, new BlockPos(8, g + 8, 8), Blocks.AIR, "wonderdream upper floor");
        expect(wonderdream, new BlockPos(0, g + 2, 3), Blocks.SPRUCE_PLANKS, "wonderdream wall");

        ServerLevel stonehell = level(server, Journey.STONEHELL);
        load(stonehell, -1, 0, 1, 3);
        expect(stonehell, new BlockPos(2, DreamChunkGenerator.HELL_FLOOR, 10), Blocks.AIR, "stonehell tunnel");
        expect(stonehell, new BlockPos(2, DreamChunkGenerator.HELL_FLOOR + 11, 10), Blocks.AIR, "stonehell tunnel top");
        BlockPos shWall = new BlockPos(2, DreamChunkGenerator.HELL_FLOOR - 1, 10);
        if (!stonehell.getBlockState(shWall).is(Blocks.COBBLESTONE)
                && !stonehell.getBlockState(shWall).is(Blocks.MOSSY_COBBLESTONE)
                && !stonehell.getBlockState(shWall).is(Blocks.STONE)) {
            throw new IllegalStateException("SMOKE: stonehell wall missing: " + stonehell.getBlockState(shWall));
        }
        int roomSeg = 0;
        for (; roomSeg < 60; roomSeg++) {
            if (Math.floorMod(DreamChunkGenerator.hash(roomSeg, 0, 555), 7) == 0) {
                break;
            }
        }
        int roomZ = roomSeg * 64 + 39;
        load(stonehell, 0, (roomZ >> 4) - 1, 1, (roomZ >> 4) + 1);
        expect(stonehell, new BlockPos(13, DreamChunkGenerator.HELL_FLOOR + 1, roomZ),
                ModRegistry.WONDERDREAM_PORTAL.get(), "wooden room exit");
        expect(stonehell, new BlockPos(7, DreamChunkGenerator.HELL_FLOOR - 1, roomSeg * 64 + 34),
                Blocks.OAK_PLANKS, "wooden room floor");
        FinDaries.LOGGER.info("FINDARIES SMOKE: stonehell wooden room at segment {} z={}", roomSeg, roomZ);

        // entities
        ServerLevel overworld = server.overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();
        overworld.getChunk(spawn.getX() >> 4, spawn.getZ() >> 4);
        int sy = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING, spawn.getX(), spawn.getZ());
        ServerPlayer fake = FakePlayerFactory.getMinecraft(overworld);
        fake.moveTo(spawn.getX() + 0.5D, sy, spawn.getZ() + 0.5D, 0.0F, 0.0F);
        List<EntityType<? extends Entity>> types = List.of(ModRegistry.LONGLEGS.get(), ModRegistry.WATCHER.get(),
                ModRegistry.DANGER_12.get(), ModRegistry.BLUE_STRANGER.get(), ModRegistry.WANDERER.get(), ModRegistry.SHE.get(),
                ModRegistry.FLOWER_MAN.get(), ModRegistry.RED_DEMON.get(), ModRegistry.SKY_PERSON.get(),
                ModRegistry.PILLAR.get(), ModRegistry.FAKE_PLAYER.get(), ModRegistry.CAVE_DEMON.get(),
                ModRegistry.ONE_EYE_GIANT.get(), ModRegistry.NEVELIM.get(), ModRegistry.EYE.get());
        for (EntityType<? extends Entity> type : types) {
            Entity e = type.create(overworld);
            if (e == null) {
                throw new IllegalStateException("SMOKE: cannot create " + type);
            }
            e.moveTo(spawn.getX() + 20.5D, sy, spawn.getZ() + 0.5D, 0.0F, 0.0F);
            overworld.addFreshEntity(e);
            for (int i = 0; i < 10; i++) {
                e.tick();
            }
            e.discard();
        }

        // structures
        int bx = spawn.getX() + 200, bz = spawn.getZ();
        load(overworld, (bx - 16) >> 4, (bz - 16) >> 4, (bx + 80) >> 4, (bz + 16) >> 4);
        int by = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
        Structures.buildCastle(overworld, bx, by, bz);
        BlockPos wall = new BlockPos(bx + Structures.CASTLE_HALF - 1, by + 3, bz + 3);
        if (!overworld.getBlockState(wall).is(Blocks.COBBLESTONE) && !overworld.getBlockState(wall).is(Blocks.MOSSY_COBBLESTONE)) {
            throw new IllegalStateException("SMOKE: castle wall missing: " + overworld.getBlockState(wall));
        }
        if (com.findaries.horror.world.ModWorldData.get(server).castleAt(bx + 0.5D, by, bz + 0.5D) == null) {
            throw new IllegalStateException("SMOKE: castle not remembered");
        }
        Structures.buildHellPortal(overworld, bx + 30, by, bz);
        expect(overworld, new BlockPos(bx + 31, by, bz), ModRegistry.HELLWAY_PORTAL.get(), "hell portal");
        expect(overworld, new BlockPos(bx + 30, by, bz), ModRegistry.BLOOD_COBBLESTONE.get(), "hell portal frame");
        Structures.buildWrongHouse(overworld, bx + 50, by, bz);
        expect(overworld, new BlockPos(bx + 50, by, bz - 3), Blocks.OAK_DOOR, "wrong house door");
        expect(overworld, new BlockPos(bx + 50, by + 4, bz), Blocks.SPRUCE_PLANKS, "wrong house upper floor");
        expect(overworld, new BlockPos(bx + 50, by + 5, bz - 3), Blocks.OAK_DOOR, "wrong house upstairs door");
        if (com.findaries.horror.world.ModWorldData.get(server).wrongHouseAt(bx + 50.5D, by + 1, bz + 0.5D) == null) {
            throw new IllegalStateException("SMOKE: wrong house not remembered");
        }
        int holeX = bx + 90, holeZ = bz + 10;
        load(overworld, (holeX - 4) >> 4, (holeZ - 4) >> 4, (holeX + 4) >> 4, (holeZ + 4) >> 4);
        int holeY = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, holeX, holeZ);
        Structures.buildHole(overworld, holeX, holeY, holeZ);
        expect(overworld, new BlockPos(holeX, holeY - 3, holeZ), ModRegistry.DOOR_PORTAL.get(), "door portal in the hole");
        expect(overworld, new BlockPos(holeX, holeY - 4, holeZ), Blocks.BEDROCK, "bedrock under the hole");
        Structures.buildShip(overworld, bx + 70, by, bz);
        expect(overworld, new BlockPos(bx + 70, by + 1, bz), Blocks.SEA_LANTERN, "ship core");
        com.findaries.horror.world.FlowerFields.tickAround(overworld, spawn.getX(), spawn.getZ());
        BlockPos field = com.findaries.horror.world.FlowerFields.nearest(overworld, spawn.getX(), spawn.getZ());
        FinDaries.LOGGER.info("FINDARIES SMOKE: nearest flower field {}", field);
        if (field == null) {
            throw new IllegalStateException("SMOKE: no flower field anywhere near spawn");
        }

        // every overworld event once
        for (String name : HorrorDirector.eventNames()) {
            boolean ok = HorrorDirector.runEvent(name, fake);
            FinDaries.LOGGER.info("FINDARIES SMOKE: event {} -> {}", name, ok);
        }
        HorrorDirector.flushTasks();
        Journey.spawnWatcher(wonderland, fake);

        FinDaries.LOGGER.info("FINDARIES SMOKE: OK");
        server.halt(false);
    }

    private SmokeTest() {
    }
}

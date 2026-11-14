package com.findaries.horror.client;

import com.findaries.horror.FinDaries;
import com.findaries.horror.ModRegistry;
import com.findaries.horror.entity.ApparitionEntity;
import com.findaries.horror.entity.SkyPersonEntity;
import com.findaries.horror.event.HorrorDirector;
import com.findaries.horror.world.FlowerFields;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import com.findaries.horror.event.Journey;
import com.findaries.horror.network.Net;
import com.findaries.horror.network.PhenomenonPacket;
import com.findaries.horror.world.DreamChunkGenerator;
import com.findaries.horror.world.Structures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Automated walk through the whole mod on a real client (only with -Dfindaries.smoke=true):
 * creates a world, shows every sky object, spawns every entity, visits every dimension,
 * takes screenshots and finally lets itself be thrown to the title screen. Used by CI under xvfb.
 */
@Mod.EventBusSubscriber(modid = FinDaries.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientSmoke {
    private static final boolean ENABLED = Boolean.getBoolean("findaries.smoke");

    private record Step(int delay, Runnable run) {
    }

    private static final List<Step> STEPS = new ArrayList<>();
    private static int state;
    private static int index;
    private static int timer;

    private static void step(int delay, Runnable run) {
        STEPS.add(new Step(delay, run));
    }

    private static void server(Consumer<ServerPlayer> action) {
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            throw new IllegalStateException("CLIENT SMOKE: no integrated server");
        }
        server.execute(() -> {
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            if (players.isEmpty()) {
                throw new IllegalStateException("CLIENT SMOKE: no player");
            }
            action.accept(players.get(0));
        });
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), msg -> {
        });
        FinDaries.LOGGER.info("FINDARIES CLIENT SMOKE: shot {} in {}", name, mc.level == null ? "-" : mc.level.dimension().location());
    }

    private static void fly(ServerPlayer p, double x, double y, double z, float yaw, float pitch) {
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.teleportTo(p.serverLevel(), x, y, z, yaw, pitch);
    }

    private static void clearMobs(ServerPlayer p) {
        for (Mob e : p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(128.0D),
                m -> m instanceof ApparitionEntity || m instanceof SkyPersonEntity)) {
            e.discard();
        }
    }

    private static void look(float pitch) {
        server(p -> fly(p, p.getX(), p.getY(), p.getZ(), 0.0F, pitch));
    }

    private static void expectDimension(ResourceKey<Level> key) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.dimension() != key) {
            throw new IllegalStateException("CLIENT SMOKE: expected to be in " + key.location() + " but is in "
                    + (mc.level == null ? "nothing" : mc.level.dimension().location()));
        }
    }

    private static PhenomenonPacket ahead(int kind, double back, double up) {
        Minecraft mc = Minecraft.getInstance();
        // angle 0 flies towards +Z, which is where yaw 0 looks
        return new PhenomenonPacket(kind, mc.player.getX(), mc.player.getY() + up, mc.player.getZ() - back, 0.0F, 4242L);
    }

    private static void script() {
        int g = DreamChunkGenerator.GROUND;
        step(120, () -> server(p -> {
            ServerLevel level = p.serverLevel();
            level.setDayTime(6000L);
            level.setWeatherParameters(100000, 0, false, false);
            fly(p, p.getX(), p.getY() + 12.0D, p.getZ(), 0.0F, -35.0F);
        }));
        // ---- sky objects ----
        step(40, () -> ClientPhenomena.debugShow(ahead(PhenomenonPacket.TRIANGLE, 1800.0D, 170.0D), 340));
        step(12, () -> shot("01_triangle"));
        step(2, () -> {
            look(-45.0F);
            ClientPhenomena.debugShow(ahead(PhenomenonPacket.SLAB, 600.0D, 80.0D), 300);
        });
        step(12, () -> shot("02_slab"));
        step(2, () -> {
            look(-30.0F);
            ClientPhenomena.debugShow(ahead(PhenomenonPacket.UFO, 2400.0D, 260.0D), 410);
        });
        step(12, () -> shot("03_ufo"));
        step(2, () -> {
            look(-20.0F);
            ClientPhenomena.debugShow(ahead(PhenomenonPacket.WALKERS, -135.0D, -12.0D), 5);
        });
        step(12, () -> shot("04_walkers"));
        step(2, () -> ClientPhenomena.debugShow(ahead(PhenomenonPacket.TOWER, -40.0D, -12.0D), 110));
        step(12, () -> shot("05_tower"));
        // ---- the other triangles ----
        step(2, () -> {
            look(-35.0F);
            ClientPhenomena.debugShow(ahead(PhenomenonPacket.TRIANGLE_EYES, 1800.0D, 170.0D), 340);
        });
        step(12, () -> shot("21_triangle_eyes"));
        step(2, () -> ClientPhenomena.debugShow(ahead(PhenomenonPacket.TRIANGLE_STAR, 1800.0D, 170.0D), 340));
        step(12, () -> shot("22_triangle_star_vhs"));
        step(2, () -> ClientPhenomena.debugShow(ahead(PhenomenonPacket.TRIANGLE_LYING, 1800.0D, 170.0D), 340));
        step(12, () -> shot("23_triangle_lying"));
        step(2, () -> ClientPhenomena.debugShow(ahead(PhenomenonPacket.SHIP, -45.0D, -12.0D), 75));
        step(12, () -> shot("24_ship_falling"));
        // ---- entities ----
        step(2, () -> {
            ClientPhenomena.debugClear();
            server(p -> {
                ServerLevel level = p.serverLevel();
                List<EntityType<? extends Entity>> types = List.of(ModRegistry.LONGLEGS.get(), ModRegistry.WATCHER.get(),
                        ModRegistry.BLUE_STRANGER.get(), ModRegistry.DANGER_12.get());
                for (int i = 0; i < types.size(); i++) {
                    Entity e = types.get(i).create(level);
                    e.setNoGravity(true);
                    double dist = i == 3 ? 34.0D : 15.0D;
                    e.moveTo(p.getX() - 6.0D + i * 4.0D, p.getY() - 2.0D, p.getZ() + dist, 180.0F, 0.0F);
                    level.addFreshEntity(e);
                }
                fly(p, p.getX(), p.getY(), p.getZ(), 0.0F, 0.0F);
            });
        });
        step(50, () -> shot("06_entities"));
        step(2, () -> server(p -> {
            clearMobs(p);
            ServerLevel level = p.serverLevel();
            List<EntityType<? extends Entity>> types = List.of(ModRegistry.WANDERER.get(), ModRegistry.SHE.get(),
                    ModRegistry.FLOWER_MAN.get(), ModRegistry.RED_DEMON.get(), ModRegistry.SKY_PERSON.get());
            for (int i = 0; i < types.size(); i++) {
                Entity e = types.get(i).create(level);
                e.setNoGravity(true);
                e.moveTo(p.getX() - 9.0D + i * 4.5D, p.getY() - 2.0D, p.getZ() + 14.0D, 180.0F, 0.0F);
                level.addFreshEntity(e);
            }
        }));
        step(30, () -> shot("25_new_entities"));
        // ---- flower field ----
        step(2, () -> server(p -> {
            clearMobs(p);
            ServerLevel level = p.serverLevel();
            BlockPos field = FlowerFields.nearest(level, p.getBlockX(), p.getBlockZ());
            level.getChunk(field.getX() >> 4, field.getZ() >> 4);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, field.getX(), field.getZ());
            fly(p, field.getX() + 0.5D, y + 9.0D, field.getZ() + 0.5D, 0.0F, 24.0F);
        }));
        step(260, () -> server(p -> HorrorDirector.runEvent("flower_man", p)));
        step(30, () -> shot("26_flower_field"));
        // ---- structures ----
        step(2, () -> server(p -> {
            clearMobs(p);
            ServerLevel level = p.serverLevel();
            int x = p.getBlockX(), z = p.getBlockZ();
            Structures.buildCastle(level, x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z + 52), z + 52);
            Structures.buildWrongHouse(level, x - 20, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x - 20, z + 26), z + 26);
            Structures.buildHellPortal(level, x + 9, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + 9, z + 20), z + 20);
            Structures.buildShip(level, x + 26, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + 26, z + 34), z + 34);
            BlockPos flower = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z + 9), z + 9);
            DoublePlantBlock.placeAt(level, ModRegistry.EYE_FLOWER.get().defaultBlockState(), flower, 2);
            fly(p, p.getX(), p.getY() + 4.0D, p.getZ(), 0.0F, 20.0F);
        }));
        step(60, () -> shot("27_structures"));
        step(2, () -> server(p -> fly(p, p.getX(), p.getY() - 9.0D, p.getZ() + 4.0D, 0.0F, 35.0F)));
        step(30, () -> shot("28_eye_flower"));
        // ---- hellway ----
        step(2, () -> server(p -> Journey.go(p, Journey.Dest.HELLWAY)));
        step(160, () -> {
            expectDimension(Journey.HELLWAY);
            server(p -> {
                ServerLevel level = p.serverLevel();
                p.teleportTo(level, 2.0D, DreamChunkGenerator.HELL_FLOOR, 0.5D, 0.0F, -5.0F);
                Entity demon = ModRegistry.RED_DEMON.get().create(level);
                demon.moveTo(2.0D, DreamChunkGenerator.HELL_FLOOR, 13.5D, 180.0F, 0.0F);
                level.addFreshEntity(demon);
            });
        });
        step(30, () -> shot("29_hellway_demon"));
        step(2, () -> server(p -> {
            clearMobs(p);
            p.teleportTo(p.serverLevel(), 1.5D, DreamChunkGenerator.HELL_FLOOR, DreamChunkGenerator.HELL_ROOM_Z + 3.0D, -90.0F, 0.0F);
        }));
        step(40, () -> shot("30_hellway_room"));
        step(2, () -> server(p -> p.teleportTo(p.serverLevel(), 10.5D, DreamChunkGenerator.HELL_FLOOR, DreamChunkGenerator.HELL_ROOM_Z + 3.0D, -90.0F, 0.0F)));
        // ---- wonderland.avi ----
        step(200, () -> {
            expectDimension(Journey.WONDERLAND);
            shot("07_wonderland");
        });
        step(2, () -> server(p -> fly(p, 0.5D, g + 46.0D, -70.0D, 0.0F, 22.0F)));
        step(140, () -> shot("08_wonderland_air"));
        step(2, () -> server(p -> fly(p, 0.5D, g + 4.0D, 14.0D, 0.0F, 5.0F)));
        step(80, () -> shot("09_giant_house"));
        step(2, () -> server(p -> fly(p, DreamChunkGenerator.CABIN_X - 9.0D, g + 2.5D, 0.5D, -90.0F, 8.0F)));
        step(120, () -> shot("10_cabin"));
        step(2, () -> server(p -> {
            Journey.spawnWatcher(p.serverLevel(), p);
            p.teleportTo(p.serverLevel(), DreamChunkGenerator.CABIN_X + 2.5D, g + 1.0D, 0.5D, -90.0F, 0.0F);
        }));
        // ---- corridor, house, lift ----
        step(80, () -> {
            expectDimension(Journey.HALLWAY);
            shot("11_corridor");
        });
        step(2, () -> server(p -> p.teleportTo(p.serverLevel(), 0.5D, Structures.HALL_Y + 1, Structures.HALL_TRIGGER_Z + 1.0D, 0.0F, 0.0F)));
        step(60, () -> shot("12_house"));
        step(2, () -> server(p -> p.teleportTo(p.serverLevel(), 0.5D, Structures.HALL_Y + 1, Structures.LIFT_Z + 1.5D, 180.0F, 0.0F)));
        step(30, () -> shot("13_lift"));
        step(220, () -> {
            expectDimension(Journey.WONDERFLOWERS);
            shot("14_wonderflowers");
        });
        step(2, () -> server(p -> fly(p, 0.5D, g + 22.0D, DreamChunkGenerator.RIVER_Z0 - 40.0D, 0.0F, 28.0F)));
        step(140, () -> shot("15_river_island"));
        step(2, () -> server(p -> {
            fly(p, 0.5D, g + 1.0D, DreamChunkGenerator.RIVER_Z0 + 6.0D, 0.0F, 0.0F);
            Journey.spawnDanger(p.serverLevel(), p);
        }));
        step(45, () -> server(p -> fly(p, p.getX(), p.getY(), p.getZ(), 180.0F, -8.0F)));
        step(25, () -> shot("16_danger_12"));
        step(2, () -> server(p -> {
            for (ApparitionEntity e : p.serverLevel().getEntitiesOfClass(ApparitionEntity.class, p.getBoundingBox().inflate(128.0D))) {
                e.discard();
            }
            p.teleportTo(p.serverLevel(), 0.5D, g + 1.0D, DreamChunkGenerator.ISLAND_Z + 3.5D, 0.0F, 0.0F);
        }));
        // ---- cage_wonderland ----
        step(200, () -> {
            expectDimension(Journey.CAGE);
            shot("17_cage");
        });
        step(2, () -> server(p -> fly(p, 0.5D, g + 3.0D, 30.0D, 0.0F, -40.0F)));
        step(100, () -> shot("18_cage_trees"));
        step(2, () -> server(p -> fly(p, 0.5D, g + 1.5D, DreamChunkGenerator.CAGE_R - 9.0D, 0.0F, 0.0F)));
        step(80, () -> shot("19_exit"));
        step(2, () -> server(p -> p.teleportTo(p.serverLevel(), 0.5D, g + 1.0D, DreamChunkGenerator.CAGE_R + 1.5D, 0.0F, 0.0F)));
        step(160, () -> {
            expectDimension(Level.OVERWORLD);
            shot("20_alive");
        });
        // ---- 0.4.0: the new watchers ----
        step(2, () -> server(p -> {
            clearMobs(p);
            ServerLevel level = p.serverLevel();
            List<EntityType<? extends Entity>> types = List.of(ModRegistry.PILLAR.get(), ModRegistry.CAVE_DEMON.get(),
                    ModRegistry.ONE_EYE_GIANT.get(), ModRegistry.NEVELIM.get(), ModRegistry.EYE.get());
            for (int i = 0; i < types.size(); i++) {
                Entity e = types.get(i).create(level);
                e.setNoGravity(true);
                e.moveTo(p.getX() - 8.0D + i * 4.0D, p.getY(), p.getZ() + 10.0D, 180.0F, 0.0F);
                level.addFreshEntity(e);
            }
            fly(p, p.getX(), p.getY() + 2.0D, p.getZ(), 0.0F, 9.0F);
        }));
        step(45, () -> shot("31_new_entities"));
        // ---- the_wonderdream ----
        step(2, () -> server(p -> {
            clearMobs(p);
            Journey.go(p, Journey.Dest.WONDERDREAM);
        }));
        step(170, () -> {
            expectDimension(Journey.WONDERDREAM);
            shot("32_wonderdream");
        });
        step(2, () -> server(p -> fly(p, 8.5D, g + 2.0D, 8.5D, 0.0F, 0.0F)));
        step(70, () -> shot("33_wonderdream_corridor"));
        step(2, () -> server(p -> {
            Journey.spawnFalsePlayers(p.serverLevel(), p);
            fly(p, 8.5D, g + 2.0D, 8.5D, 0.0F, 4.0F);
        }));
        step(45, () -> shot("34_false_players"));
        // ---- stonehell ----
        step(2, () -> server(p -> {
            clearMobs(p);
            Journey.go(p, Journey.Dest.STONEHELL);
        }));
        step(170, () -> {
            expectDimension(Journey.STONEHELL);
            shot("35_stonehell");
        });
        step(2, () -> server(p -> {
            clearMobs(p);
            ServerLevel level = p.serverLevel();
            level.getChunk(0, -1);
            level.getChunk(0, -2);
            fly(p, 2.0D, DreamChunkGenerator.HELL_FLOOR, 0.5D, 180.0F, -8.0F);
            Journey.spawnGiant(level, p);
        }));
        step(50, () -> shot("36_one_eye_giant"));
        // ---- the 860 block Nevelim ----
        step(2, () -> server(p -> Journey.go(p, Journey.Dest.ALIVE)));
        step(170, () -> {
            expectDimension(Level.OVERWORLD);
            ClientPhenomena.debugClear();
        });
        step(2, () -> {
            server(p -> fly(p, p.getX(), p.getY() + 8.0D, p.getZ(), 0.0F, -20.0F));
            ClientPhenomena.debugShow(ahead(PhenomenonPacket.GIANT_NEVELIM, -700.0D, 0.0D), 152);
        });
        step(45, () -> shot("37_giant_nevelim"));
        step(70, () -> shot("37b_giant_explosion"));
        // ---- the wrong house and the hole ----
        step(2, () -> {
            ClientPhenomena.debugClear();
            server(p -> {
                clearMobs(p);
                ServerLevel level = p.serverLevel();
                int x = p.getBlockX(), z = p.getBlockZ();
                int hy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z + 32);
                Structures.buildWrongHouse(level, x, hy, z + 32);
                int dy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + 15, z + 20);
                Structures.buildHole(level, x + 15, dy, z + 20);
                fly(p, x + 7.5D, hy + 15.0D, z - 4.0D, 0.0F, 22.0F);
            });
        });
        step(70, () -> shot("38_wrong_house_hole"));
        // ---- thrown to the main menu ----
        step(40, () -> server(Net::kick));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        switch (state) {
            case 0 -> { // wait for the title screen, then make a world
                if (mc.screen instanceof TitleScreen && mc.getOverlay() == null && mc.level == null && ++timer > 40) {
                    state = 1;
                    timer = 0;
                    FinDaries.LOGGER.info("FINDARIES CLIENT SMOKE: creating world");
                    LevelSettings settings = new LevelSettings("fd_smoke", GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("fd_smoke", settings, new WorldOptions(20240613L, false, false),
                            WorldPresets::createNormalWorldDimensions);
                }
            }
            case 1 -> { // wait until we are in the world
                if (mc.level != null && mc.player != null && mc.screen == null) {
                    state = 2;
                    index = 0;
                    timer = 0;
                    script();
                    FinDaries.LOGGER.info("FINDARIES CLIENT SMOKE: in world, {} steps", STEPS.size());
                }
            }
            case 2 -> {
                if (index >= STEPS.size()) {
                    if (mc.level == null && mc.screen instanceof TitleScreen) {
                        FinDaries.LOGGER.info("FINDARIES CLIENT SMOKE: OK");
                        state = 3;
                        mc.stop();
                    } else if (++timer > 20 * 60) {
                        throw new IllegalStateException("CLIENT SMOKE: was not thrown to the title screen");
                    }
                    return;
                }
                if (mc.level == null) {
                    throw new IllegalStateException("CLIENT SMOKE: left the world at step " + index);
                }
                Step s = STEPS.get(index);
                if (++timer >= s.delay) {
                    timer = 0;
                    index++;
                    s.run.run();
                }
            }
            default -> {
            }
        }
    }

    private ClientSmoke() {
    }
}

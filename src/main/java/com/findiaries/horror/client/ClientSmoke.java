package com.findiaries.horror.client;

import com.findiaries.horror.FinDiaries;
import com.findiaries.horror.ModRegistry;
import com.findiaries.horror.entity.ApparitionEntity;
import com.findiaries.horror.event.Journey;
import com.findiaries.horror.network.Net;
import com.findiaries.horror.network.PhenomenonPacket;
import com.findiaries.horror.world.DreamChunkGenerator;
import com.findiaries.horror.world.Structures;
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
 * Automated walk through the whole mod on a real client (only with -Dfindiaries.smoke=true):
 * creates a world, shows every sky object, spawns every entity, visits every dimension,
 * takes screenshots and finally lets itself be thrown to the title screen. Used by CI under xvfb.
 */
@Mod.EventBusSubscriber(modid = FinDiaries.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientSmoke {
    private static final boolean ENABLED = Boolean.getBoolean("findiaries.smoke");

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
        FinDiaries.LOGGER.info("FINDIARIES CLIENT SMOKE: shot {} in {}", name, mc.level == null ? "-" : mc.level.dimension().location());
    }

    private static void fly(ServerPlayer p, double x, double y, double z, float yaw, float pitch) {
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.teleportTo(p.serverLevel(), x, y, z, yaw, pitch);
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
            for (ApparitionEntity e : p.serverLevel().getEntitiesOfClass(ApparitionEntity.class, p.getBoundingBox().inflate(128.0D))) {
                e.discard();
            }
            Journey.go(p, Journey.Dest.WONDERLAND);
        }));
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
                    FinDiaries.LOGGER.info("FINDIARIES CLIENT SMOKE: creating world");
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
                    FinDiaries.LOGGER.info("FINDIARIES CLIENT SMOKE: in world, {} steps", STEPS.size());
                }
            }
            case 2 -> {
                if (index >= STEPS.size()) {
                    if (mc.level == null && mc.screen instanceof TitleScreen) {
                        FinDiaries.LOGGER.info("FINDIARIES CLIENT SMOKE: OK");
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

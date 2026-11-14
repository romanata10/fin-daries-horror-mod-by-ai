package com.findiaries.horror.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The flower biome of the overworld. Forge cannot add a real biome to vanilla terrain without extra
 * libraries, so it is an overlay: round fields whose place is a pure function of the world seed.
 * Chunks inside a field are covered (once) in poppies and dandelions, and Flower Men
 * only appear while the player stands in one.
 */
public final class FlowerFields {
    private static final int CELL = 320;

    private static final Map<Long, Boolean> DRY = new ConcurrentHashMap<>();

    public static void clear() {
        DRY.clear();
    }

    /** x, z, radius of the field that owns this grid cell, or null. Fields never lie in oceans, rivers or on beaches. */
    private static int[] field(ServerLevel level, int cellX, int cellZ) {
        Random r = new Random(DreamChunkGenerator.hash(cellX, cellZ, level.getSeed() ^ 0x77L));
        if (r.nextInt(4) != 0) {
            return null;
        }
        int x = cellX * CELL + 90 + r.nextInt(CELL - 180);
        int z = cellZ * CELL + 90 + r.nextInt(CELL - 180);
        int radius = 55 + r.nextInt(35);
        boolean dry = DRY.computeIfAbsent(ChunkPos.asLong(cellX, cellZ), k -> {
            Holder<Biome> biome = level.getBiome(new BlockPos(x, level.getSeaLevel() + 1, z));
            return !biome.is(BiomeTags.IS_OCEAN) && !biome.is(BiomeTags.IS_RIVER) && !biome.is(BiomeTags.IS_BEACH);
        });
        return dry ? new int[]{x, z, radius} : null;
    }

    /** 0 outside, up to 1 in the middle of a field. */
    public static double strength(ServerLevel level, int x, int z) {
        int[] f = field(level, Math.floorDiv(x, CELL), Math.floorDiv(z, CELL));
        if (f == null) {
            return 0.0D;
        }
        double d = Math.sqrt((double) (x - f[0]) * (x - f[0]) + (double) (z - f[1]) * (z - f[1]));
        return d >= f[2] ? 0.0D : Math.min(1.0D, (f[2] - d) / 12.0D);
    }

    public static boolean inside(ServerLevel level, double x, double z) {
        return strength(level, (int) Math.floor(x), (int) Math.floor(z)) > 0.0D;
    }

    /** Centre of the closest field (searches a few cells around). */
    public static BlockPos nearest(ServerLevel level, int x, int z) {
        long seed = level.getSeed();
        int cx = Math.floorDiv(x, CELL), cz = Math.floorDiv(z, CELL);
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -12; dz <= 12; dz++) {
                int[] f = field(level, cx + dx, cz + dz);
                if (f == null) {
                    continue;
                }
                double d = (double) (f[0] - x) * (f[0] - x) + (double) (f[1] - z) * (f[1] - z);
                if (d < bestD) {
                    bestD = d;
                    best = new BlockPos(f[0], 0, f[1]);
                }
            }
        }
        return best;
    }

    /** Covers the loaded, not yet planted chunks of nearby fields with flowers. Called now and then for every player. */
    public static void tickAround(ServerLevel overworld, int blockX, int blockZ) {
        long seed = overworld.getSeed();
        ModWorldData data = ModWorldData.get(overworld.getServer());
        int pcx = blockX >> 4, pcz = blockZ >> 4;
        int done = 0;
        for (int cx = pcx - 7; cx <= pcx + 7 && done < 6; cx++) {
            for (int cz = pcz - 7; cz <= pcz + 7 && done < 6; cz++) {
                int x0 = cx << 4, z0 = cz << 4;
                if (!(strength(overworld, x0, z0) > 0 || strength(overworld, x0 + 15, z0) > 0 || strength(overworld, x0, z0 + 15) > 0
                        || strength(overworld, x0 + 15, z0 + 15) > 0 || strength(overworld, x0 + 8, z0 + 8) > 0)) {
                    continue;
                }
                long key = ChunkPos.asLong(cx, cz);
                if (data.isPlanted(key)) {
                    continue;
                }
                LevelChunk chunk = overworld.getChunkSource().getChunkNow(cx, cz);
                if (chunk != null) {
                    plant(overworld, chunk);
                    data.setPlanted(key);
                    done++;
                }
            }
        }
    }

    public static void plant(ServerLevel level, LevelChunk chunk) {
        long seed = level.getSeed();
        ChunkPos cp = chunk.getPos();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = cp.getMinBlockX() + dx, z = cp.getMinBlockZ() + dz;
                double s = strength(level, x, z);
                if (s <= 0.0D) {
                    continue;
                }
                long h = DreamChunkGenerator.hash(x, z, seed ^ 0x5151L);
                if ((h & 1023) > s * 880) {
                    continue;
                }
                int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dx, dz) + 1;
                p.set(x, y, z);
                BlockState here = chunk.getBlockState(p);
                if (here.is(Blocks.GRASS) || here.is(Blocks.FERN)) {
                    // short grass gives way to flowers
                } else if (!here.isAir()) {
                    continue;
                }
                if (!chunk.getBlockState(p.below()).is(Blocks.GRASS_BLOCK)) {
                    continue;
                }
                int pick = (int) ((h >>> 12) & 63);
                BlockState flower;
                if (pick < 29) {
                    flower = Blocks.POPPY.defaultBlockState();
                } else if (pick < 58) {
                    flower = Blocks.DANDELION.defaultBlockState();
                } else if (pick < 60) {
                    flower = Blocks.OXEYE_DAISY.defaultBlockState();
                } else if (pick < 62) {
                    flower = Blocks.AZURE_BLUET.defaultBlockState();
                } else {
                    flower = Blocks.CORNFLOWER.defaultBlockState();
                }
                level.setBlock(p, flower, 2);
            }
        }
    }

    private FlowerFields() {
    }
}

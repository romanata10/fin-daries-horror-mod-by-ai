package com.findiaries.horror.world;

import com.findiaries.horror.ModRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * One generator for all dream dimensions. The "style" picks the look:
 * <ul>
 *   <li>wonderland - bright rolling hills, giant flowers, giant blocks, broken old trees</li>
 *   <li>wonderflowers - a field of giant poppies, a straight path, a river, an island</li>
 *   <li>cage - wooden walls, impossibly tall trees, hills and water</li>
 *   <li>void - nothing (the corridor and the house are built by hand)</li>
 * </ul>
 * Everything is a pure function of the block coordinates, so features can freely cross chunk borders.
 */
public class DreamChunkGenerator extends ChunkGenerator {
    public static final Codec<DreamChunkGenerator> CODEC = RecordCodecBuilder.create(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource),
            Codec.STRING.fieldOf("style").forGetter(g -> g.styleName)
    ).apply(i, DreamChunkGenerator::new));

    public enum Style { WONDERLAND, WONDERFLOWERS, CAGE, VOID }

    // ---- shared layout constants (also used by the hand-built structures) ----------------------
    public static final int GROUND = 64;
    /** Wonderland: the wooden cabin with the corridor portal. */
    public static final int CABIN_X = 96, CABIN_Z = 0;
    /** Wonderflowers: river band and the island with the dotted portal. */
    public static final int RIVER_Z0 = 160, RIVER_Z1 = 232, ISLAND_Z = 196, ISLAND_R = 7, WF_WATER = 58;
    /** Cage: half size of the wooden box and the water level. */
    public static final int CAGE_R = 80, CAGE_WATER = 57, CAGE_WALL_TOP = 230;

    private final String styleName;
    private final Style style;

    public DreamChunkGenerator(BiomeSource biomeSource, String styleName) {
        super(biomeSource);
        this.styleName = styleName;
        Style s;
        try {
            s = Style.valueOf(styleName.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            s = Style.VOID;
        }
        this.style = s;
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    // ---- terrain shape ------------------------------------------------------------------------

    private static double flat(double dx, double dz, double radius, double blend) {
        double d = Math.sqrt(dx * dx + dz * dz);
        double w = (radius + blend - d) / blend;
        return w < 0 ? 0 : (w > 1 ? 1 : w);
    }

    /** Y of the top solid block, or -1 if the column is empty. */
    public static int height(Style style, int x, int z) {
        switch (style) {
            case WONDERLAND: {
                double h = GROUND + 11.0 * Math.sin(x * 0.027) * Math.cos(z * 0.031)
                        + 6.0 * Math.sin((x + z) * 0.052 + 1.3)
                        + 3.0 * Math.sin(x * 0.13) * Math.sin(z * 0.11);
                double w = Math.max(flat(x, z - 22, 44, 20), flat(x - CABIN_X, z - CABIN_Z, 12, 16));
                // keep the trail between the arrival point and the cabin walkable
                if (x > 0 && x < CABIN_X && Math.abs(z) < 24) {
                    w = Math.max(w, 0.55 * (1.0 - Math.abs(z) / 24.0));
                }
                return (int) Math.round(h + (GROUND - h) * w);
            }
            case WONDERFLOWERS: {
                if (z >= RIVER_Z0 && z < RIVER_Z1) {
                    int dz = z - ISLAND_Z;
                    return (x * x + dz * dz <= ISLAND_R * ISLAND_R) ? GROUND : 50;
                }
                if (Math.abs(x) < 8) {
                    return GROUND;
                }
                return GROUND + (int) Math.round(1.6 * Math.sin(x * 0.07) * Math.cos(z * 0.06));
            }
            case CAGE: {
                if (Math.abs(x) > CAGE_R + 2 || Math.abs(z) > CAGE_R + 2) {
                    return -1;
                }
                double h = 62.0 + 10.0 * Math.sin(x * 0.06) * Math.cos(z * 0.05) + 5.0 * Math.sin((x - z) * 0.09);
                double w = Math.max(flat(x, z, 5, 8), flat(x, z - (CAGE_R - 1), 6, 8));
                return (int) Math.round(h + (GROUND - h) * w);
            }
            default:
                return -1;
        }
    }

    static long hash(long a, long b, long salt) {
        long h = a * 0x9E3779B97F4A7C15L ^ b * 0xC2B2AE3D27D4EB4FL ^ salt * 0x165667B19E3779F9L;
        h ^= h >>> 31;
        h *= 0x7FB5D329728EA185L;
        h ^= h >>> 27;
        h *= 0x81DADEF4BC2DD44DL;
        h ^= h >>> 33;
        return h;
    }

    // ---- palette ------------------------------------------------------------------------------
    private static final BlockState BEDROCK = Blocks.BEDROCK.defaultBlockState();
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
    private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();
    private static final BlockState SAND = Blocks.SAND.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState LOG = Blocks.OAK_LOG.defaultBlockState();
    private static final BlockState PLANKS = Blocks.OAK_PLANKS.defaultBlockState();
    private static final BlockState LEAVES = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    private static final BlockState POPPY = Blocks.POPPY.defaultBlockState();
    private static final BlockState DANDELION = Blocks.DANDELION.defaultBlockState();
    private static final BlockState PATH = Blocks.DIRT_PATH.defaultBlockState();

    /** 16x16 pixel art of the flower sprites, blown up into blocks. */
    private static final String[] POPPY_ART = {
            "................",
            "................",
            "................",
            ".....RR.RRR.....",
            "....RRRRRRRR....",
            "....RRrkkRRR....",
            "....RRkkkrRR....",
            ".....RRrRRR.....",
            "......RRRR......",
            ".......GG.......",
            ".......GG.gG....",
            "....Gg.GGGG.....",
            ".....GGGG.......",
            ".......GG.......",
            ".......GG.......",
            ".......GG.......",
    };
    private static final String[] DANDELION_ART = {
            "................",
            "................",
            "................",
            "................",
            "................",
            "......YYYY......",
            ".....YYyYYY.....",
            ".....YyYYyY.....",
            "......YYYY......",
            ".......GG.......",
            ".......GG.......",
            ".......GG.gG....",
            "....Gg.GGGG.....",
            ".....GGGG.......",
            ".......GG.......",
            ".......GG.......",
    };

    private static BlockState pixel(char c) {
        switch (c) {
            case 'R': return Blocks.RED_CONCRETE.defaultBlockState();
            case 'r': return Blocks.RED_TERRACOTTA.defaultBlockState();
            case 'k': return Blocks.BLACK_CONCRETE.defaultBlockState();
            case 'G': return Blocks.GREEN_CONCRETE.defaultBlockState();
            case 'g': return Blocks.LIME_CONCRETE.defaultBlockState();
            case 'Y': return Blocks.YELLOW_CONCRETE.defaultBlockState();
            case 'y': return Blocks.ORANGE_CONCRETE.defaultBlockState();
            default: return null;
        }
    }

    /** Two crossed planes of scaled-up pixel art, just like the flower block model. */
    private static void giantFlower(Canvas c, String[] art, int cx, int by, int cz, int s) {
        int thick = s >= 3 ? 1 : 0;
        for (int row = 0; row < art.length; row++) {
            String line = art[row];
            for (int col = 0; col < line.length(); col++) {
                BlockState state = pixel(line.charAt(col));
                if (state == null) {
                    continue;
                }
                int u = (col - 8) * s;
                int v = by + (art.length - 1 - row) * s;
                c.box(cx + u, v, cz - thick, cx + u + s - 1, v + s - 1, cz, state);
                c.box(cx - thick, v, cz + u, cx, v + s - 1, cz + u + s - 1, state);
            }
        }
    }

    private static void canopy(Canvas c, int x, int top, int z) {
        c.box(x - 2, top - 2, z - 2, x + 2, top - 1, z + 2, LEAVES);
        c.box(x - 1, top, z - 1, x + 1, top + 1, z + 1, LEAVES);
    }

    /** An "old" oak that generated wrong. */
    private static void brokenTree(Canvas c, Random r, int x, int y, int z) {
        int mode = r.nextInt(6);
        switch (mode) {
            case 0: { // far too tall
                int h = 14 + r.nextInt(20);
                canopy(c, x, y + h, z);
                c.box(x, y, z, x, y + h - 1, z, LOG);
                break;
            }
            case 1: { // trunk that wanders sideways
                int h = 9 + r.nextInt(12);
                int tx = x, tz = z;
                for (int i = 0; i < h; i++) {
                    if (i > 2 && r.nextInt(3) == 0) {
                        if (r.nextBoolean()) {
                            tx += r.nextBoolean() ? 1 : -1;
                        } else {
                            tz += r.nextBoolean() ? 1 : -1;
                        }
                        c.set(tx, y + i - 1, tz, LOG);
                    }
                    c.set(tx, y + i, tz, LOG);
                }
                canopy(c, tx, y + h, tz);
                break;
            }
            case 2: { // pancake crown
                int h = 5 + r.nextInt(4);
                int w = 4 + r.nextInt(4);
                c.box(x - w, y + h, z - w, x + w, y + h + 1, z + w, LEAVES);
                c.box(x, y, z, x, y + h, z, LOG);
                break;
            }
            case 3: { // crown that floats away from the trunk
                int h = 5 + r.nextInt(5);
                int gap = 3 + r.nextInt(5);
                c.box(x, y, z, x, y + h, z, LOG);
                canopy(c, x + r.nextInt(5) - 2, y + h + gap + 2, z + r.nextInt(5) - 2);
                break;
            }
            case 4: { // column of leaves
                int h = 10 + r.nextInt(14);
                c.box(x - 1, y + 3, z - 1, x + 1, y + h, z + 1, LEAVES);
                c.box(x, y, z, x, y + h - 2, z, LOG);
                break;
            }
            default: { // normal old tree, repeated on top of itself
                int h = 5 + r.nextInt(2);
                int n = 1 + r.nextInt(3);
                for (int i = 0; i < n; i++) {
                    int b = y + i * (h + 2);
                    canopy(c, x, b + h, z);
                    c.box(x, b, z, x, b + h - 1, z, LOG);
                }
                break;
            }
        }
    }

    private static BlockState giantBlockState(Random r) {
        switch (r.nextInt(3)) {
            case 0: return Blocks.DIAMOND_BLOCK.defaultBlockState();
            case 1: return Blocks.COBBLESTONE.defaultBlockState();
            default: return Blocks.CRAFTING_TABLE.defaultBlockState();
        }
    }

    // ---- chunk filling ------------------------------------------------------------------------

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState random, StructureManager structures, ChunkAccess chunk) {
        if (this.style != Style.VOID) {
            Canvas c = new Canvas(chunk);
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            int water = this.style == Style.WONDERFLOWERS ? WF_WATER : (this.style == Style.CAGE ? CAGE_WATER : -1);
            for (int x = c.minX; x <= c.maxX; x++) {
                for (int z = c.minZ; z <= c.maxZ; z++) {
                    int h = height(this.style, x, z);
                    if (h < 1) {
                        continue;
                    }
                    boolean wet = h < water;
                    chunk.setBlockState(p.set(x, 0, z), BEDROCK, false);
                    for (int y = 1; y <= h; y++) {
                        BlockState s = y < h - 3 ? STONE : (y < h ? DIRT : (wet ? SAND : GRASS));
                        chunk.setBlockState(p.set(x, y, z), s, false);
                    }
                    for (int y = h + 1; y <= water; y++) {
                        chunk.setBlockState(p.set(x, y, z), WATER, false);
                    }
                }
            }
            switch (this.style) {
                case WONDERLAND -> wonderland(c);
                case WONDERFLOWERS -> wonderflowers(c);
                case CAGE -> cage(c);
                default -> {
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    private static boolean wonderlandReserved(int x, int z, int margin) {
        if (Math.sqrt((double) x * x + (double) (z - 22) * (z - 22)) < 44 + margin) {
            return true;
        }
        if (Math.sqrt((double) (x - CABIN_X) * (x - CABIN_X) + (double) (z - CABIN_Z) * (z - CABIN_Z)) < 12 + margin) {
            return true;
        }
        return x > -margin && x < CABIN_X + margin && Math.abs(z) < 5 + margin;
    }

    private static void wonderland(Canvas c) {
        // small flowers and grass tufts
        for (int x = c.minX; x <= c.maxX; x++) {
            for (int z = c.minZ; z <= c.maxZ; z++) {
                int h = height(Style.WONDERLAND, x, z);
                long r = hash(x, z, 11) & 0xFFFF;
                if (r < 1300) {
                    c.set(x, h + 1, z, (r & 1) == 0 ? POPPY : DANDELION);
                }
            }
        }
        final int cell = 44, reach = 28;
        for (int gx = Math.floorDiv(c.minX - reach, cell); gx <= Math.floorDiv(c.maxX + reach, cell); gx++) {
            for (int gz = Math.floorDiv(c.minZ - reach, cell); gz <= Math.floorDiv(c.maxZ + reach, cell); gz++) {
                Random r = new Random(hash(gx, gz, 101));
                int cx = gx * cell + 8 + r.nextInt(cell - 16);
                int cz = gz * cell + 8 + r.nextInt(cell - 16);
                int roll = r.nextInt(100);
                if (wonderlandReserved(cx, cz, 26)) {
                    continue;
                }
                int base = height(Style.WONDERLAND, cx, cz) + 1;
                if (roll < 28) {
                    giantFlower(c, POPPY_ART, cx, base, cz, 1 + r.nextInt(3));
                } else if (roll < 52) {
                    giantFlower(c, DANDELION_ART, cx, base, cz, 1 + r.nextInt(3));
                } else if (roll < 84) {
                    int n = 3 + r.nextInt(5);
                    for (int i = 0; i < n; i++) {
                        int tx = cx + r.nextInt(29) - 14;
                        int tz = cz + r.nextInt(29) - 14;
                        brokenTree(c, r, tx, height(Style.WONDERLAND, tx, tz) + 1, tz);
                    }
                } else {
                    int size = 6 + r.nextInt(8);
                    int half = size / 2;
                    int y0 = base - 1 - r.nextInt(3);
                    c.box(cx - half, y0, cz - half, cx - half + size - 1, y0 + size - 1, cz - half + size - 1, giantBlockState(r));
                }
            }
        }
    }

    private static void wonderflowers(Canvas c) {
        BlockState portal = ModRegistry.DOTS_PORTAL.get().defaultBlockState();
        for (int x = c.minX; x <= c.maxX; x++) {
            for (int z = c.minZ; z <= c.maxZ; z++) {
                int h = height(Style.WONDERFLOWERS, x, z);
                boolean river = z >= RIVER_Z0 && z < RIVER_Z1;
                if (Math.abs(x) <= 1 && z >= -6 && z < RIVER_Z0) {
                    c.set(x, h, z, PATH);
                } else if (h >= GROUND - 2 && (hash(x, z, 23) & 7) < 2 && !(river && Math.abs(x) <= 2 && z > ISLAND_Z)) {
                    c.set(x, h + 1, z, POPPY);
                }
                // the oak bridge from the bank to the island
                if (Math.abs(x) <= 1 && z >= RIVER_Z0 && z <= ISLAND_Z - ISLAND_R + 1) {
                    c.set(x, GROUND, z, PLANKS);
                    c.set(x, GROUND + 1, z, AIR);
                }
                // the portal made of coloured dots
                if (Math.abs(x) <= 1 && z == ISLAND_Z + 3) {
                    for (int y = GROUND + 1; y <= GROUND + 4; y++) {
                        c.set(x, y, z, portal);
                    }
                }
            }
        }
        final int cell = 30, reach = 28;
        for (int gx = Math.floorDiv(c.minX - reach, cell); gx <= Math.floorDiv(c.maxX + reach, cell); gx++) {
            for (int gz = Math.floorDiv(c.minZ - reach, cell); gz <= Math.floorDiv(c.maxZ + reach, cell); gz++) {
                Random r = new Random(hash(gx, gz, 202));
                int cx = gx * cell + 5 + r.nextInt(cell - 10);
                int cz = gz * cell + 5 + r.nextInt(cell - 10);
                int s = 1 + r.nextInt(3);
                int half = 8 * s + 1;
                if (Math.abs(cx) < half + 4) {
                    continue; // keep the path clear
                }
                if (cz > RIVER_Z0 - half - 2 && cz < RIVER_Z1 + half + 2) {
                    continue; // keep the river clear
                }
                giantFlower(c, POPPY_ART, cx, height(Style.WONDERFLOWERS, cx, cz) + 1, cz, s);
            }
        }
    }

    private static void cage(Canvas c) {
        BlockState portal = ModRegistry.WHITE_PORTAL.get().defaultBlockState();
        for (int x = c.minX; x <= c.maxX; x++) {
            for (int z = c.minZ; z <= c.maxZ; z++) {
                int edge = Math.max(Math.abs(x), Math.abs(z));
                if (edge > CAGE_R + 2) {
                    continue;
                }
                int h = height(Style.CAGE, x, z);
                if (edge >= CAGE_R) {
                    BlockState wall = Math.floorMod(x + z, 8) == 0 ? LOG : PLANKS;
                    c.box(x, 1, z, x, CAGE_WALL_TOP, z, wall);
                    // the hole with the white portal, in the +Z wall
                    if (Math.abs(x) <= 1 && z >= CAGE_R && z <= CAGE_R + 1) {
                        for (int y = GROUND + 1; y <= GROUND + 4; y++) {
                            c.set(x, y, z, z == CAGE_R ? AIR : portal);
                        }
                    }
                } else if (h >= CAGE_WATER) {
                    long r = hash(x, z, 37) & 0xFF;
                    if (r < 26) {
                        c.set(x, h + 1, z, Blocks.GRASS.defaultBlockState());
                    }
                }
            }
        }
        c.clip(-CAGE_R + 1, -CAGE_R + 1, CAGE_R - 1, CAGE_R - 1);
        final int cell = 34, reach = 24;
        for (int gx = Math.floorDiv(c.minX - reach, cell); gx <= Math.floorDiv(c.maxX + reach, cell); gx++) {
            for (int gz = Math.floorDiv(c.minZ - reach, cell); gz <= Math.floorDiv(c.maxZ + reach, cell); gz++) {
                Random r = new Random(hash(gx, gz, 303));
                int cx = gx * cell + 6 + r.nextInt(cell - 12);
                int cz = gz * cell + 6 + r.nextInt(cell - 12);
                if (Math.abs(cx) > CAGE_R - 6 || Math.abs(cz) > CAGE_R - 6) {
                    continue;
                }
                if (cx * cx + cz * cz < 14 * 14 || cx * cx + (cz - CAGE_R) * (cz - CAGE_R) < 16 * 16) {
                    continue;
                }
                int base = height(Style.CAGE, cx, cz) - 2;
                int h = 105 + r.nextInt(60);
                int top = base + h;
                int rad = 12 + r.nextInt(7);
                c.blob(cx, top, cz, rad, 8 + r.nextInt(4), rad, LEAVES);
                c.blob(cx + r.nextInt(13) - 6, top - 14, cz + r.nextInt(13) - 6, rad - 4, 5, rad - 4, LEAVES);
                c.box(cx - 2, base, cz - 2, cx + 1, top + 2, cz + 1, LOG);
                // roots
                c.box(cx - 3, base, cz - 1, cx + 2, base + 5, cz, LOG);
                c.box(cx - 1, base, cz - 3, cx, base + 5, cz + 2, LOG);
            }
        }
        c.clip(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    // ---- boring required overrides ------------------------------------------------------------

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState random, BiomeManager biomes, StructureManager structures, ChunkAccess chunk, GenerationStep.Carving step) {
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState random, ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public int getGenDepth() {
        return 256;
    }

    @Override
    public int getSeaLevel() {
        return 58;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return Math.max(0, height(this.style, x, z) + 1);
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        return new NoiseColumn(0, new BlockState[0]);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
    }
}

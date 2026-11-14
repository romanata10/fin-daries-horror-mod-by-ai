package com.findaries.horror.world;

import com.findaries.horror.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import static com.findaries.horror.world.DreamChunkGenerator.CABIN_X;
import static com.findaries.horror.world.DreamChunkGenerator.CABIN_Z;
import static com.findaries.horror.world.DreamChunkGenerator.GROUND;

/** Hand-built set pieces. Each one is (re)built lazily the first time somebody needs it. */
public final class Structures {
    public static final String NOTE = "fun fun fun fun fun fun wonderland is fun place fun fun";

    /** Chest of the giant-block house in wonderland. */
    public static final BlockPos NOTE_CHEST = new BlockPos(0, GROUND + 1, 37);

    // corridor + house live in the void "hallway" dimension
    public static final int HALL_Y = 100;
    public static final int HALL_TRIGGER_Z = 14;
    public static final int HOUSE_Z = 200;
    public static final int LIFT_Z = HOUSE_Z + 14;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private static void set(ServerLevel level, int x, int y, int z, BlockState state) {
        level.setBlock(new BlockPos(x, y, z), state, 2 | 16);
    }

    private static void fill(ServerLevel level, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    set(level, x, y, z, state);
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Portal frame (used in the overworld and at the wonderland arrival point)
    // ---------------------------------------------------------------------------------------------

    /** 4x5 mossy frame along the X axis with a 2x3 static portal. {@code y} is the first free block above ground. */
    public static void buildWonderlandPortal(ServerLevel level, int x, int y, int z) {
        BlockState frame = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        BlockState portal = ModRegistry.WONDERLAND_PORTAL.get().defaultBlockState();
        for (int dx = 0; dx < 4; dx++) {
            for (int dy = -1; dy <= 3; dy++) {
                boolean border = dx == 0 || dx == 3 || dy == -1 || dy == 3;
                set(level, x + dx, y + dy, z, border ? frame : portal);
            }
        }
        // a few flowers, as if the place leaked through
        for (int dx = -1; dx <= 4; dx++) {
            for (int dz = -1; dz <= 1; dz += 2) {
                BlockPos p = new BlockPos(x + dx, y, z + dz);
                BlockState flower = ((dx + dz) & 1) == 0 ? Blocks.POPPY.defaultBlockState() : Blocks.DANDELION.defaultBlockState();
                if (level.getBlockState(p).isAir() && flower.canSurvive(level, p)) {
                    level.setBlock(p, flower, 2);
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // wonderland.avi
    // ---------------------------------------------------------------------------------------------

    public static void ensureWonderland(ServerLevel level) {
        if (level.getBlockState(NOTE_CHEST).is(Blocks.CHEST)) {
            return;
        }
        int g = GROUND + 1;

        // way back, right behind the arrival point
        buildWonderlandPortal(level, -2, g, -6);

        // ---- the house made of giant blocks: 5x5 giant blocks, each 6x6x6 ----
        final int s = 6;
        final int x0 = -15, z0 = 22;
        BlockState[] mats = {
                Blocks.DIAMOND_BLOCK.defaultBlockState(),
                Blocks.COBBLESTONE.defaultBlockState(),
                Blocks.CRAFTING_TABLE.defaultBlockState()
        };
        for (int i = 0; i < 5; i++) {
            for (int k = 0; k < 5; k++) {
                for (int lvl = 0; lvl < 3; lvl++) {
                    boolean wall = i == 0 || i == 4 || k == 0 || k == 4;
                    boolean door = i == 2 && k == 0 && lvl == 0;
                    BlockState state;
                    if (lvl == 2 || (wall && !door)) {
                        state = mats[Math.floorMod(i * 2 + k + lvl, 3)];
                    } else {
                        state = AIR;
                    }
                    fill(level, x0 + i * s, g + lvl * s, z0 + k * s, x0 + i * s + s - 1, g + lvl * s + s - 1, z0 + k * s + s - 1, state);
                }
            }
        }
        // the chest with the note
        for (int dx = -2; dx <= 2; dx += 4) {
            for (int dz = -2; dz <= 2; dz += 4) {
                set(level, NOTE_CHEST.getX() + dx, g, NOTE_CHEST.getZ() + dz, Blocks.TORCH.defaultBlockState());
            }
        }
        level.setBlock(NOTE_CHEST, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH), 3);
        if (level.getBlockEntity(NOTE_CHEST) instanceof ChestBlockEntity chest) {
            chest.setItem(13, noteBook());
        }

        // ---- trail to the cabin ----
        for (int x = 4; x < CABIN_X - 4; x++) {
            for (int z = -1; z <= 0; z++) {
                int h = DreamChunkGenerator.height(DreamChunkGenerator.Style.WONDERLAND, x, z);
                set(level, x, h, z, Blocks.DIRT_PATH.defaultBlockState());
                set(level, x, h + 1, z, AIR);
            }
        }

        // ---- the wooden cabin with the portal ----
        buildCabin(level, CABIN_X, g, CABIN_Z);
    }

    public static ItemStack noteBook() {
        return book("note", "wonderland.avi", NOTE);
    }

    /** A written book with a single page. */
    public static ItemStack book(String title, String author, String text) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        tag.putString("title", title);
        tag.putString("author", author);
        ListTag pages = new ListTag();
        pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(text))));
        tag.put("pages", pages);
        return book;
    }

    private static void buildCabin(ServerLevel level, int cx, int g, int cz) {
        BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        BlockState portal = ModRegistry.HALL_PORTAL.get().defaultBlockState();
        for (int x = cx - 3; x <= cx + 3; x++) {
            for (int z = cz - 3; z <= cz + 3; z++) {
                boolean edgeX = x == cx - 3 || x == cx + 3;
                boolean edgeZ = z == cz - 3 || z == cz + 3;
                set(level, x, g - 1, z, planks);
                for (int y = g; y <= g + 3; y++) {
                    BlockState s = AIR;
                    if (edgeX && edgeZ) {
                        s = log;
                    } else if (edgeX || edgeZ) {
                        s = planks;
                    }
                    set(level, x, y, z, s);
                }
                set(level, x, g + 4, z, planks);
            }
        }
        // roof ridge
        fill(level, cx - 2, g + 5, cz - 2, cx + 2, g + 5, cz + 2, Blocks.SPRUCE_PLANKS.defaultBlockState());
        // doorway towards the trail
        set(level, cx - 3, g, cz, AIR);
        set(level, cx - 3, g + 1, cz, AIR);
        // windows
        set(level, cx, g + 1, cz - 3, Blocks.GLASS.defaultBlockState());
        set(level, cx, g + 1, cz + 3, Blocks.GLASS.defaultBlockState());
        // the portal on the far wall
        for (int z = cz - 1; z <= cz + 1; z++) {
            for (int y = g; y <= g + 2; y++) {
                set(level, cx + 2, y, z, portal);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // corridor + wooden house with the lift (void dimension)
    // ---------------------------------------------------------------------------------------------

    public static void ensureHallway(ServerLevel level) {
        if (level.getBlockState(new BlockPos(0, HALL_Y, 0)).is(Blocks.SPRUCE_PLANKS)
                && level.getBlockState(new BlockPos(0, HALL_Y, HOUSE_Z + 1)).is(Blocks.OAK_PLANKS)) {
            return;
        }
        int y = HALL_Y;
        BlockState oak = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState spruce = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState rib = Blocks.DARK_OAK_LOG.defaultBlockState();
        BlockState lantern = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);

        // ---- the long corridor ----
        for (int z = -1; z <= 64; z++) {
            for (int x = -2; x <= 2; x++) {
                for (int dy = 0; dy <= 4; dy++) {
                    boolean shell = x == -2 || x == 2 || dy == 0 || dy == 4 || z == -1 || z == 64;
                    BlockState s = AIR;
                    if (shell) {
                        s = dy == 0 ? spruce : ((Math.floorMod(z, 4) == 0 && dy != 0) ? rib : oak);
                    }
                    set(level, x, y + dy, z, s);
                }
            }
            if (Math.floorMod(z, 12) == 2) {
                set(level, 0, y + 3, z, lantern);
            }
        }

        // ---- the wooden house ----
        int z0 = HOUSE_Z - 1, z1 = HOUSE_Z + 13; // walls
        for (int x = -6; x <= 6; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int dy = 0; dy <= 5; dy++) {
                    boolean wall = x == -6 || x == 6 || z == z0 || z == z1;
                    BlockState s = AIR;
                    if (dy == 0 || dy == 5) {
                        s = oak;
                    } else if (wall) {
                        boolean corner = (x == -6 || x == 6) && (z == z0 || z == z1);
                        s = corner ? Blocks.OAK_LOG.defaultBlockState() : spruce;
                    }
                    set(level, x, y + dy, z, s);
                }
            }
        }
        int f = y + 1; // floor level (first air block)
        // the door you "came through"
        BlockState door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH);
        set(level, 0, f, z0, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(level, 0, f + 1, z0, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        // windows into nothing
        for (int z : new int[]{HOUSE_Z + 3, HOUSE_Z + 4, HOUSE_Z + 9, HOUSE_Z + 10}) {
            for (int dy = 1; dy <= 2; dy++) {
                set(level, -6, f + dy, z, Blocks.GLASS.defaultBlockState());
                set(level, 6, f + dy, z, Blocks.GLASS.defaultBlockState());
            }
        }
        // carpet
        fill(level, -1, f, HOUSE_Z + 4, 1, f, HOUSE_Z + 8, Blocks.RED_CARPET.defaultBlockState());
        // table and chairs
        set(level, -3, f, HOUSE_Z + 5, Blocks.OAK_FENCE.defaultBlockState());
        set(level, -3, f + 1, HOUSE_Z + 5, Blocks.OAK_PRESSURE_PLATE.defaultBlockState());
        set(level, -4, f, HOUSE_Z + 5, Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
        set(level, -2, f, HOUSE_Z + 5, Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
        // bed
        BlockState bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH);
        set(level, 4, f, HOUSE_Z + 2, bed.setValue(BedBlock.PART, BedPart.FOOT));
        set(level, 4, f, HOUSE_Z + 1, bed.setValue(BedBlock.PART, BedPart.HEAD));
        // shelves and flowers
        fill(level, -5, f, HOUSE_Z + 8, -5, f + 1, HOUSE_Z + 11, Blocks.BOOKSHELF.defaultBlockState());
        set(level, -5, f + 2, HOUSE_Z + 8, Blocks.POTTED_POPPY.defaultBlockState());
        set(level, -5, f + 2, HOUSE_Z + 11, Blocks.POTTED_DANDELION.defaultBlockState());
        // work corner
        set(level, 5, f, HOUSE_Z + 6, Blocks.CRAFTING_TABLE.defaultBlockState());
        set(level, 5, f, HOUSE_Z + 7, Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.WEST));
        set(level, 5, f, HOUSE_Z + 9, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST));
        set(level, -5, f, HOUSE_Z + 1, Blocks.JUKEBOX.defaultBlockState());
        // light
        set(level, 0, f + 3, HOUSE_Z + 6, lantern);
        set(level, -3, f + 3, HOUSE_Z + 10, lantern);

        // ---- the lift ----
        BlockState iron = Blocks.IRON_BLOCK.defaultBlockState();
        for (int x = -2; x <= 2; x++) {
            for (int z = z1; z <= z1 + 4; z++) {
                for (int dy = 0; dy <= 4; dy++) {
                    boolean inside = x >= -1 && x <= 1 && z >= z1 + 1 && z <= z1 + 3 && dy >= 1 && dy <= 3;
                    if (z == z1 && (Math.abs(x) == 2 || dy == 4 || dy == 0)) {
                        continue; // leave the house wall alone outside the lift front
                    }
                    set(level, x, y + dy, z, inside ? AIR : iron);
                }
            }
        }
        set(level, 0, y + 4, z1 + 2, Blocks.SEA_LANTERN.defaultBlockState());
        set(level, 0, f + 2, z1, iron);
        setLiftDoor(level, true);
    }

    /** Opens or closes the iron door of the lift. */
    public static void setLiftDoor(ServerLevel level, boolean open) {
        int z = HOUSE_Z + 13;
        BlockState door = Blocks.IRON_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.OPEN, open);
        set(level, 0, HALL_Y + 1, z, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(level, 0, HALL_Y + 2, z, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    // ---------------------------------------------------------------------------------------------
    // overworld structures of the second update
    // ---------------------------------------------------------------------------------------------

    public static final int CASTLE_HALF = 11;

    private static BlockState castleStone(int x, int y, int z) {
        long h = DreamChunkGenerator.hash(x, z, y * 31L + 9);
        return (h & 7) < 2 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
    }

    /** Cobblestone castle: outer wall with corner towers, a gate and a keep. {@code y} = first free block above ground. */
    public static void buildCastle(ServerLevel level, int cx, int y, int cz) {
        final int half = CASTLE_HALF;
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                int x = cx + dx, z = cz + dz;
                int ax = Math.abs(dx), az = Math.abs(dz);
                // foundation and floor
                for (int yy = y - 5; yy < y; yy++) {
                    set(level, x, yy, z, castleStone(x, yy, z));
                }
                boolean tower = ax >= half - 3 && az >= half - 3;
                boolean towerInside = tower && ax >= half - 2 && ax <= half - 1 && az >= half - 2 && az <= half - 1;
                boolean wall = !tower && (ax == half - 1 || az == half - 1);
                boolean outside = !tower && (ax == half || az == half);
                boolean gate = dz == -(half - 1) && ax <= 1;
                boolean keepWall = (ax == 4 && az <= 4) || (az == 4 && ax <= 4);
                boolean keepInside = ax < 4 && az < 4;
                boolean keepDoor = dz == -4 && dx == 0;
                for (int dy = 0; dy <= 15; dy++) {
                    BlockState s = AIR;
                    if (outside) {
                        continue; // leave the terrain around the walls alone
                    }
                    if (tower) {
                        if (dy <= 10) {
                            s = towerInside && dy > 0 && dy < 10 ? AIR : castleStone(x, y + dy, z);
                        } else if (dy == 11 && !towerInside && ((ax + az) & 1) == 0) {
                            s = castleStone(x, y + dy, z);
                        }
                    } else if (wall) {
                        if (gate && dy <= 3) {
                            s = AIR;
                        } else if (dy <= 6 || (dy == 7 && ((dx + dz) & 1) == 0)) {
                            s = castleStone(x, y + dy, z);
                        }
                    } else if (keepWall) {
                        if (keepDoor && dy <= 1) {
                            s = AIR;
                        } else if (dy <= 8 && !(dy >= 5 && dy <= 6 && (dx == 0 || dz == 0) && !(dx == 0 && dz == -4 && dy < 5))) {
                            s = castleStone(x, y + dy, z);
                        } else if (dy == 9 && ((dx + dz) & 1) == 0) {
                            s = castleStone(x, y + dy, z);
                        }
                    } else if (keepInside && dy == 8) {
                        s = castleStone(x, y + dy, z);
                    }
                    set(level, x, y + dy, z, s);
                }
            }
        }
        // a little light, not much
        set(level, cx - 3, y, cz + 3, Blocks.TORCH.defaultBlockState());
        set(level, cx + 3, y, cz + 3, Blocks.TORCH.defaultBlockState());
        set(level, cx - 6, y, cz - 6, Blocks.TORCH.defaultBlockState());
        set(level, cx + 6, y, cz - 6, Blocks.TORCH.defaultBlockState());
        ModWorldData.get(level.getServer()).addCastle(new BlockPos(cx, y, cz));
    }

    /** A frame of blood cobblestone on scorched ground with a portal to the Hellway. */
    public static void buildHellPortal(ServerLevel level, int x, int y, int z) {
        BlockState frame = ModRegistry.BLOOD_COBBLESTONE.get().defaultBlockState();
        BlockState portal = ModRegistry.HELLWAY_PORTAL.get().defaultBlockState();
        for (int dx = -3; dx <= 6; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                long h = DreamChunkGenerator.hash(x + dx, z + dz, 66);
                double d = Math.sqrt((dx - 1.5D) * (dx - 1.5D) + dz * dz);
                if (d < 2.5D + (h & 3) * 0.6D && !level.getBlockState(new BlockPos(x + dx, y - 1, z + dz)).isAir()) {
                    set(level, x + dx, y - 1, z + dz, (h & 15) < 3 ? Blocks.MAGMA_BLOCK.defaultBlockState()
                            : ((h & 15) < 8 ? frame : Blocks.NETHERRACK.defaultBlockState()));
                }
            }
        }
        for (int dx = 0; dx < 4; dx++) {
            for (int dy = -1; dy <= 3; dy++) {
                boolean border = dx == 0 || dx == 3 || dy == -1 || dy == 3;
                set(level, x + dx, y + dy, z, border ? frame : portal);
            }
        }
    }

    /**
     * The wrong house: TWO storeys of oak, with a staircase upstairs.
     * The doors never open once you are inside (see HorrorDirector), and going up to the
     * first floor throws the player into the_wonderdream. There is no portal instead of a bed.
     */
    public static void buildWrongHouse(ServerLevel level, int cx, int y, int cz) {
        BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState spruce = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        BlockState cobble = Blocks.COBBLESTONE.defaultBlockState();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int x = cx + dx, z = cz + dz;
                boolean edgeX = Math.abs(dx) == 3, edgeZ = Math.abs(dz) == 3;
                for (int yy = y - 3; yy < y - 1; yy++) {
                    if (level.getBlockState(new BlockPos(x, yy, z)).canBeReplaced()) {
                        set(level, x, yy, z, cobble);
                    }
                }
                set(level, x, y - 1, z, edgeX || edgeZ ? cobble : planks);
                for (int dy = 0; dy <= 8; dy++) {
                    BlockState state = AIR;
                    if (edgeX && edgeZ) {
                        state = log;
                    } else if (edgeX || edgeZ) {
                        state = dy == 0 ? cobble : (dy == 4 ? spruce : planks);
                    } else if (dy == 4) {
                        state = spruce; // ceiling / upper floor
                    }
                    set(level, x, y + dy, z, state);
                }
            }
        }
        // roof
        for (int layer = 0; layer <= 3; layer++) {
            int r = 4 - layer;
            fill(level, cx - r, y + 9 + layer, cz - r, cx + r, y + 9 + layer, cz + r,
                    layer == 3 ? Blocks.OAK_SLAB.defaultBlockState() : planks);
            if (layer < 3 && r > 1) {
                fill(level, cx - r + 1, y + 9 + layer, cz - r + 1, cx + r - 1, y + 9 + layer, cz + r - 1,
                        layer == 0 ? planks : AIR);
            }
        }
        // the staircase to the first floor and the hole above it
        for (int i = 0; i < 5; i++) {
            set(level, cx + 2, y + i, cz - 1 + i, Blocks.OAK_STAIRS.defaultBlockState()
                    .setValue(StairBlock.FACING, Direction.SOUTH));
            set(level, cx + 2, y + 4, cz - 1 + i, AIR);
        }
        // doors: they are ordinary oak doors, but they will not open inside this house
        BlockState lower = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH);
        set(level, cx, y, cz - 3, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(level, cx, y + 1, cz - 3, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        set(level, cx, y + 5, cz - 3, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(level, cx, y + 6, cz - 3, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        // windows on both floors
        for (int floor : new int[]{y + 1, y + 6}) {
            set(level, cx - 3, floor, cz, Blocks.GLASS.defaultBlockState());
            set(level, cx + 3, floor, cz, Blocks.GLASS.defaultBlockState());
            set(level, cx - 1, floor, cz + 3, Blocks.GLASS.defaultBlockState());
            set(level, cx + 1, floor, cz + 3, Blocks.GLASS.defaultBlockState());
        }
        // furniture downstairs
        set(level, cx - 2, y, cz + 2, Blocks.CRAFTING_TABLE.defaultBlockState());
        set(level, cx - 2, y, cz + 1, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST));
        set(level, cx + 1, y, cz + 2, Blocks.OAK_FENCE.defaultBlockState());
        set(level, cx + 1, y + 1, cz + 2, Blocks.OAK_PRESSURE_PLATE.defaultBlockState());
        set(level, cx + 2, y, cz + 2, Blocks.BARREL.defaultBlockState());
        // a bed upstairs - ordinary, nothing wrong with the bed any more
        set(level, cx - 2, y + 5, cz + 2, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.WEST)
                .setValue(BedBlock.PART, BedPart.HEAD));
        set(level, cx - 1, y + 5, cz + 2, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.WEST)
                .setValue(BedBlock.PART, BedPart.FOOT));
        set(level, cx + 2, y + 5, cz + 2, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST));
        set(level, cx, y + 3, cz, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        set(level, cx, y + 8, cz, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        ModWorldData.get(level.getServer()).addWrongHouse(new BlockPos(cx, y, cz));
    }

    /** A three block deep hole down to bedrock; a door-textured portal lies at the bottom. */
    public static void buildHole(ServerLevel level, int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 1; dy <= 3; dy++) {
                    set(level, x + dx, y - dy, z + dz, AIR);
                }
                set(level, x + dx, y - 4, z + dz, Blocks.BEDROCK.defaultBlockState());
            }
        }
        set(level, x, y - 3, z, ModRegistry.DOOR_PORTAL.get().defaultBlockState());
        // a cracked rim
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                    long h = DreamChunkGenerator.hash(x + dx, z + dz, 731);
                    if ((h & 3) == 0) {
                        set(level, x + dx, y - 1, z + dz, Blocks.COBBLESTONE.defaultBlockState());
                    }
                }
            }
        }
    }

    /** The crashed ship of the Sky People. */
    public static void buildShip(ServerLevel level, int cx, int y, int cz) {
        BlockState hull = Blocks.IRON_BLOCK.defaultBlockState();
        BlockState rim = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        BlockState glass = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                int x = cx + dx, z = cz + dz;
                if (d <= 3.6D) {
                    set(level, x, y, z, hull);
                }
                if (d <= 6.3D) {
                    BlockState s = d > 5.2D ? rim : hull;
                    if (d > 5.2D && Math.floorMod(dx * 3 + dz * 5, 4) == 0) {
                        s = ((dx + dz) & 1) == 0 ? Blocks.REDSTONE_BLOCK.defaultBlockState() : Blocks.EMERALD_BLOCK.defaultBlockState();
                    }
                    set(level, x, y + 1, z, s);
                }
                if (d <= 4.4D) {
                    set(level, x, y + 2, z, d > 3.3D ? hull : AIR);
                    set(level, x, y + 3, z, d > 2.4D ? hull : glass);
                }
                if (d <= 2.4D) {
                    set(level, x, y + 4, z, glass);
                }
            }
        }
        // the hatch they came out of, and the light inside
        set(level, cx, y + 2, cz - 4, AIR);
        set(level, cx, y + 2, cz - 3, AIR);
        set(level, cx, y + 1, cz, Blocks.SEA_LANTERN.defaultBlockState());
        // scorched ground
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                long h = DreamChunkGenerator.hash(cx + dx, cz + dz, 404);
                BlockPos p = new BlockPos(cx + dx, y - 1, cz + dz);
                if (dx * dx + dz * dz <= 64 && (h & 3) == 0 && level.getBlockState(p).isSolidRender(level, p)) {
                    level.setBlock(p, (h & 4) == 0 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRAVEL.defaultBlockState(), 2 | 16);
                }
            }
        }
    }

    private Structures() {
    }
}

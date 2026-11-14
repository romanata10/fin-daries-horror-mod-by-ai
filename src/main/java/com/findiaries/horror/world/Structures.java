package com.findiaries.horror.world;

import com.findiaries.horror.ModRegistry;
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

import static com.findiaries.horror.world.DreamChunkGenerator.CABIN_X;
import static com.findiaries.horror.world.DreamChunkGenerator.CABIN_Z;
import static com.findiaries.horror.world.DreamChunkGenerator.GROUND;

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
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        tag.putString("title", "note");
        tag.putString("author", "wonderland.avi");
        ListTag pages = new ListTag();
        pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(NOTE))));
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

    private Structures() {
    }
}

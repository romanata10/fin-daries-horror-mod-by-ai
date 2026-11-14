package com.findiaries.horror.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Things the mod has to remember about a world: where the cobblestone castles stand. */
public class ModWorldData extends SavedData {
    private final List<BlockPos> castles = new ArrayList<>();
    private final it.unimi.dsi.fastutil.longs.LongOpenHashSet planted = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();

    public static ModWorldData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(ModWorldData::load, ModWorldData::new, "findiaries");
    }

    private static ModWorldData load(CompoundTag tag) {
        ModWorldData data = new ModWorldData();
        ListTag list = tag.getList("castles", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            data.castles.add(NbtUtils.readBlockPos(list.getCompound(i)));
        }
        for (long key : tag.getLongArray("planted")) {
            data.planted.add(key);
        }
        return data;
    }

    public boolean isPlanted(long chunk) {
        return this.planted.contains(chunk);
    }

    public void setPlanted(long chunk) {
        this.planted.add(chunk);
        this.setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (BlockPos pos : this.castles) {
            list.add(NbtUtils.writeBlockPos(pos));
        }
        tag.put("castles", list);
        tag.putLongArray("planted", this.planted.toLongArray());
        return tag;
    }

    public void addCastle(BlockPos center) {
        this.castles.add(center.immutable());
        this.setDirty();
    }

    public boolean castleNear(int x, int z, int distance) {
        for (BlockPos c : this.castles) {
            if (Math.abs(c.getX() - x) < distance && Math.abs(c.getZ() - z) < distance) {
                return true;
            }
        }
        return false;
    }

    /** The castle the position is inside of, or null. */
    public BlockPos castleAt(double x, double y, double z) {
        for (BlockPos c : this.castles) {
            if (Math.abs(x - c.getX() - 0.5D) <= Structures.CASTLE_HALF + 0.5D && Math.abs(z - c.getZ() - 0.5D) <= Structures.CASTLE_HALF + 0.5D
                    && y >= c.getY() - 3 && y <= c.getY() + 16) {
                return c;
            }
        }
        return null;
    }
}

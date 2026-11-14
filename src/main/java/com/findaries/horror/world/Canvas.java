package com.findaries.horror.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Writes world-space shapes into a single chunk; everything outside the chunk is silently clipped. */
final class Canvas {
    private final ChunkAccess chunk;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    final int minX, maxX, minZ, maxZ;
    private int clipMinX = Integer.MIN_VALUE, clipMaxX = Integer.MAX_VALUE, clipMinZ = Integer.MIN_VALUE, clipMaxZ = Integer.MAX_VALUE;

    Canvas(ChunkAccess chunk) {
        this.chunk = chunk;
        this.minX = chunk.getPos().getMinBlockX();
        this.maxX = this.minX + 15;
        this.minZ = chunk.getPos().getMinBlockZ();
        this.maxZ = this.minZ + 15;
    }

    /** Additional world-space limit (used to keep the cage trees inside the cage). */
    void clip(int x0, int z0, int x1, int z1) {
        this.clipMinX = x0;
        this.clipMinZ = z0;
        this.clipMaxX = x1;
        this.clipMaxZ = z1;
    }

    private int loX(int x) {
        return Math.max(Math.max(x, this.minX), this.clipMinX);
    }

    private int hiX(int x) {
        return Math.min(Math.min(x, this.maxX), this.clipMaxX);
    }

    private int loZ(int z) {
        return Math.max(Math.max(z, this.minZ), this.clipMinZ);
    }

    private int hiZ(int z) {
        return Math.min(Math.min(z, this.maxZ), this.clipMaxZ);
    }

    void set(int x, int y, int z, BlockState state) {
        if (x < loX(x) || x > hiX(x) || z < loZ(z) || z > hiZ(z) || y < 1 || y > 254) {
            return;
        }
        this.chunk.setBlockState(this.pos.set(x, y, z), state, false);
    }

    BlockState get(int x, int y, int z) {
        return this.chunk.getBlockState(this.pos.set(x, y, z));
    }

    void box(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        int ax = loX(x0), bx = hiX(x1), az = loZ(z0), bz = hiZ(z1);
        int ay = Math.max(y0, 1), by = Math.min(y1, 254);
        for (int x = ax; x <= bx; x++) {
            for (int z = az; z <= bz; z++) {
                for (int y = ay; y <= by; y++) {
                    this.chunk.setBlockState(this.pos.set(x, y, z), state, false);
                }
            }
        }
    }

    /** Solid ellipsoid. */
    void blob(int cx, int cy, int cz, int rx, int ry, int rz, BlockState state) {
        int ax = loX(cx - rx), bx = hiX(cx + rx), az = loZ(cz - rz), bz = hiZ(cz + rz);
        int ay = Math.max(cy - ry, 1), by = Math.min(cy + ry, 254);
        for (int x = ax; x <= bx; x++) {
            double dx = (x - cx) / (rx + 0.5D);
            for (int z = az; z <= bz; z++) {
                double dz = (z - cz) / (rz + 0.5D);
                for (int y = ay; y <= by; y++) {
                    double dy = (y - cy) / (ry + 0.5D);
                    if (dx * dx + dy * dy + dz * dz <= 1.0D) {
                        this.chunk.setBlockState(this.pos.set(x, y, z), state, false);
                    }
                }
            }
        }
    }
}

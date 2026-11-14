package com.findaries.horror.block;

import com.findaries.horror.event.Journey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** A walk-through portal block. Touching it asks {@link Journey} to move the player on. */
public class DreamPortalBlock extends Block {
    private final Journey.Dest dest;

    public DreamPortalBlock(Journey.Dest dest, BlockBehaviour.Properties properties) {
        super(properties);
        this.dest = dest;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            Journey.request(player, this.dest);
        }
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacent, Direction side) {
        return adjacent.is(this) || super.skipRendering(state, adjacent, side);
    }
}

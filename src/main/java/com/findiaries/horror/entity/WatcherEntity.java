package com.findiaries.horror.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** The thin dark figure of wonderland.avi. It only watches and leaves a dandelion behind. */
public class WatcherEntity extends ApparitionEntity {
    public WatcherEntity(EntityType<? extends WatcherEntity> type, Level level) {
        super(type, level);
        this.maxLife = 20 * 70;
    }

    @Override
    protected void nearPlayer(ServerPlayer player, double distance) {
        if (distance < 10.0D) {
            BlockPos pos = this.blockPosition();
            BlockState dandelion = Blocks.DANDELION.defaultBlockState();
            Level level = this.level();
            if (level.getBlockState(pos).canBeReplaced() && level.getFluidState(pos).isEmpty() && dandelion.canSurvive(level, pos)) {
                level.setBlock(pos, dandelion, 3);
            } else {
                level.addFreshEntity(new ItemEntity(level, this.getX(), this.getY() + 0.2D, this.getZ(), new ItemStack(Items.DANDELION)));
            }
            this.discard();
        }
    }
}

package com.findiaries.horror.entity;

import com.findiaries.horror.ModRegistry;
import com.findiaries.horror.event.Journey;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Flower Man: a block of dirt on two long green legs. Leaves an eye flower where it stood. */
public class FlowerManEntity extends ApparitionEntity {
    public static final String[] LINES = {"FUN", "IM IN HELL, HELP", "LIFE"};

    public FlowerManEntity(EntityType<? extends FlowerManEntity> type, Level level) {
        super(type, level);
        this.maxLife = 20 * 120;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount == 2) {
            Component line = Component.literal(LINES[this.random.nextInt(LINES.length)]);
            for (Player p : this.level().players()) {
                if (p instanceof ServerPlayer sp && sp.distanceToSqr(this) < 64.0D * 64.0D) {
                    sp.sendSystemMessage(line);
                }
            }
        }
    }

    @Override
    protected void nearPlayer(ServerPlayer player, double distance) {
        if (distance < 6.0D) {
            Level level = this.level();
            BlockPos pos = this.blockPosition();
            BlockState flower = ModRegistry.EYE_FLOWER.get().defaultBlockState();
            if (level.getBlockState(pos).canBeReplaced() && level.getBlockState(pos.above()).canBeReplaced()
                    && level.getFluidState(pos).isEmpty() && flower.canSurvive(level, pos)) {
                DoublePlantBlock.placeAt(level, flower, pos, 3);
            } else {
                level.addFreshEntity(new ItemEntity(level, this.getX(), this.getY() + 0.3D, this.getZ(), new ItemStack(ModRegistry.EYE_FLOWER.get())));
            }
            Journey.award(player, "root_in_hell");
            this.discard();
        }
    }
}

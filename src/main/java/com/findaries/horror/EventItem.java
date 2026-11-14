package com.findaries.horror;

import com.findaries.horror.event.HorrorDirector;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Creative-tab item: right click starts one of the mod's events around the player. */
public class EventItem extends Item {
    private final String event;

    public EventItem(String event, Properties properties) {
        super(properties);
        this.event = event;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            boolean ok = HorrorDirector.runEvent(this.event, serverPlayer);
            if (!ok) {
                serverPlayer.displayClientMessage(Component.translatable("message.findaries.no_place"), true);
            }
            player.getCooldowns().addCooldown(this, 40);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.findaries.event").withStyle(ChatFormatting.GRAY));
    }
}

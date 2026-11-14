package com.findiaries.horror.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Sky People from the fallen ship. They talk (every line starts with K**S.RU), sometimes hand out
 * iron or gold, and all of them turn on anyone who hits one of them.
 */
public class SkyPersonEntity extends PathfinderMob {
    public static final String PREFIX = "K**S.RU ";
    private static final String[] LINES = {
            "we saw you from above.",
            "do not be afraid.",
            "the sky is not empty.",
            "the triangle is closer than you think.",
            "you were not supposed to find us.",
            "wonderland is not a fun place.",
            "do not go near the long one.",
            "we fell. it was not an accident.",
            "you are being watched. not by us.",
            "do not hit us. please.",
    };
    private static final String[] ANGRY_LINES = {
            "why.",
            "you should not have done that.",
            "now you are one of them.",
    };

    private int talkTimer = 60;
    private int giftTimer = 20 * 25;
    private int gifts = 3;
    private boolean wasAngry;

    public SkyPersonEntity(EntityType<? extends SkyPersonEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    private void say(String text) {
        Component line = Component.literal(PREFIX + text);
        for (Player p : this.level().players()) {
            if (p instanceof ServerPlayer sp && sp.distanceToSqr(this) < 28.0D * 28.0D) {
                sp.sendSystemMessage(line);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.tickCount > 20 * 60 * 6) {
            this.discard(); // they do not stay
            return;
        }
        boolean angry = this.getTarget() != null;
        if (angry && !this.wasAngry) {
            say(ANGRY_LINES[this.random.nextInt(ANGRY_LINES.length)]);
        }
        this.wasAngry = angry;
        if (angry) {
            return;
        }
        Player near = this.level().getNearestPlayer(this, 16.0D);
        if (near == null) {
            return;
        }
        if (--this.talkTimer <= 0) {
            this.talkTimer = 20 * (9 + this.random.nextInt(14));
            say(LINES[this.random.nextInt(LINES.length)]);
        }
        if (--this.giftTimer <= 0) {
            this.giftTimer = 20 * (25 + this.random.nextInt(35));
            if (this.gifts > 0 && this.distanceToSqr(near) < 7.0D * 7.0D) {
                this.gifts--;
                ItemStack gift = new ItemStack(this.random.nextBoolean() ? Items.IRON_INGOT : Items.GOLD_INGOT, 1 + this.random.nextInt(2));
                ItemEntity item = new ItemEntity(this.level(), this.getX(), this.getEyeY() - 0.3D, this.getZ(), gift);
                Vec3 dir = near.position().subtract(this.position()).normalize().scale(0.3D);
                item.setDeltaMovement(dir.x, 0.25D, dir.z);
                this.level().addFreshEntity(item);
                this.playSound(SoundEvents.ITEM_PICKUP, 1.0F, 0.6F);
                say("take this.");
            }
        }
    }
}

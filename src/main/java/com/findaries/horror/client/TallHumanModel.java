package com.findaries.horror.client;

import com.findaries.horror.FinDaries;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

/**
 * A player-skin shaped body whose parts are stretched per entity:
 * the same mesh makes the five block tall faceless Steve and the thin stick figures.
 */
public class TallHumanModel<T extends Mob> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(FinDaries.rl("tall_human"), "main");

    /** All factors are relative to a normal player (legs/arms/body 12 px long, limbs 4 px thick). */
    public record Shape(float head, float bodyWidth, float bodyHeight, float limbWidth, float armLength, float legLength, float armSpread, boolean horns) {
        public static final Shape LONGLEGS = new Shape(0.9F, 0.5F, 1.4F, 0.22F, 2.8F, 4.6F, 0.05F, false);
        public static final Shape BLUE_STRANGER = new Shape(0.9F, 0.5F, 1.6F, 0.22F, 2.2F, 3.2F, 0.05F, false);
        public static final Shape WATCHER = new Shape(0.5F, 0.22F, 1.6F, 0.12F, 3.0F, 3.8F, 0.35F, false);
        public static final Shape WANDERER = new Shape(0.6F, 0.35F, 1.8F, 0.14F, 2.9F, 3.6F, Mth.HALF_PI, false);
        public static final Shape SHE = new Shape(0.6F, 0.3F, 1.8F, 0.14F, 2.2F, 3.4F, 0.03F, false);
        /** Image-12: extremely thin, horned, almost all legs. */
        public static final Shape RED_DEMON = new Shape(0.30F, 0.08F, 2.7F, 0.05F, 2.2F, 5.6F, 0.02F, true);
        public static final Shape CAVE_DEMON = RED_DEMON;
        public static final Shape SKY_PERSON = new Shape(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
        public static final Shape DANGER_12 = new Shape(0.5F, 0.2F, 1.8F, 0.12F, 2.8F, 3.6F, 0.25F, false);
        /** Image-19: a wide black rectangle with a small head and two yellow eyes. */
        public static final Shape ONE_EYE_GIANT = new Shape(0.55F, 1.8F, 3.2F, 0.30F, 0.5F, 2.0F, 0.0F, false);
        /** Image-22: very tall, very thin, dark navy, cube head with white eyes. */
        public static final Shape NEVELIM = new Shape(0.50F, 0.16F, 2.9F, 0.07F, 3.6F, 5.0F, 0.02F, false);
        /** Image-20: a black monolith with a thin blue outline and a dotted head. */
        public static final Shape EYE = new Shape(0.45F, 1.35F, 2.8F, 0.10F, 0.25F, 1.7F, 0.0F, false);
    }

    private final Shape shape;
    private final ModelPart head;
    private final ModelPart horns;
    private final ModelPart jaw;
    private final ModelPart shoulders;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public TallHumanModel(ModelPart root, Shape shape) {
        this.shape = shape;
        this.head = root.getChild("head");
        this.horns = root.getChild("horns");
        this.jaw = root.getChild("jaw");
        this.shoulders = root.getChild("shoulders");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("horns", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4.0F, -17.0F, -0.5F, 1.0F, 9.0F, 1.0F).addBox(3.0F, -17.0F, -0.5F, 1.0F, 9.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3.0F, -2.0F, -5.0F, 6.0F, 2.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("shoulders", CubeListBuilder.create().texOffs(16, 16)
                .addBox(-6.0F, 0.0F, -3.0F, 12.0F, 2.0F, 6.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public boolean glows() {
        return this.shape.horns;
    }

    private static void place(ModelPart part, float x, float y, float sx, float sy, float sz) {
        part.x = x;
        part.y = y;
        part.z = 0.0F;
        part.xRot = 0.0F;
        part.yRot = 0.0F;
        part.zRot = 0.0F;
        part.xScale = sx;
        part.yScale = sy;
        part.zScale = sz;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        Shape s = this.shape;
        // model space: y grows downwards, the ground is at y = 24
        float legTop = 24.0F - 12.0F * s.legLength;
        float bodyTop = legTop - 12.0F * s.bodyHeight;
        float halfBody = 4.0F * s.bodyWidth;
        float legX = Math.max(1.9F * s.bodyWidth, 2.0F * s.limbWidth + 0.05F);

        place(this.head, 0.0F, bodyTop, s.head, s.head, s.head);
        place(this.body, 0.0F, bodyTop, s.bodyWidth, s.bodyHeight, s.bodyWidth);
        place(this.rightArm, -(halfBody + s.limbWidth), bodyTop + 2.0F * s.armLength, s.limbWidth, s.armLength, s.limbWidth);
        place(this.leftArm, halfBody + s.limbWidth, bodyTop + 2.0F * s.armLength, s.limbWidth, s.armLength, s.limbWidth);
        place(this.rightLeg, -legX, legTop, s.limbWidth, s.legLength, s.limbWidth);
        place(this.leftLeg, legX, legTop, s.limbWidth, s.legLength, s.limbWidth);

        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD;
        place(this.jaw, 0.0F, bodyTop, s.head, s.head, s.head);
        this.jaw.yRot = this.head.yRot;
        this.jaw.xRot = this.head.xRot;
        place(this.shoulders, 0.0F, bodyTop, s.bodyWidth, s.bodyHeight, s.bodyWidth);
        this.shoulders.visible = s.bodyWidth > 1.0F;
        place(this.horns, 0.0F, bodyTop, s.head, s.head, s.head);
        this.horns.yRot = this.head.yRot;
        this.horns.xRot = this.head.xRot;
        this.horns.visible = s.horns;

        float stride = 0.6662F / Mth.sqrt(s.legLength);
        float swing = Mth.cos(limbSwing * stride) * 0.8F * limbSwingAmount;
        this.rightLeg.xRot = swing;
        this.leftLeg.xRot = -swing;
        this.rightArm.xRot = -swing * 0.6F;
        this.leftArm.xRot = swing * 0.6F;

        float sway = Mth.cos(ageInTicks * 0.06F) * 0.03F;
        this.rightArm.zRot = s.armSpread + sway;
        this.leftArm.zRot = -s.armSpread - sway;
        if (s.armSpread > 1.4F && ageInTicks > 150.0F && ageInTicks < 450.0F) {
            // the Wanderer waves while the triangle is overhead
            float wave = Mth.sin(ageInTicks * 0.45F) * 0.6F;
            this.rightArm.zRot = s.armSpread + wave;
            this.leftArm.zRot = -s.armSpread - wave;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.head.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.horns.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.jaw.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.shoulders.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.body.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.rightArm.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.leftArm.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.rightLeg.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.leftLeg.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}

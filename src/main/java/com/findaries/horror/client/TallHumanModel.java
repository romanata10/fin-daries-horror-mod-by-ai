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
 * A player-skin shaped body whose parts are stretched per entity.
 * LONGLEGS is the vanilla Steve mesh with every part stretched uniformly to the
 * 5 block hitbox from the original prompt (image-10, "Стив без лица"); every other
 * shape follows its own reference photo.
 */
public class TallHumanModel<T extends Mob> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(FinDaries.rl("tall_human"), "main");

    /** All factors are relative to a normal player (legs/arms/body 12 px long, limbs 4 px thick). */
    public record Shape(float head, float bodyWidth, float bodyHeight, float limbWidth, float armLength, float legLength, float armSpread, boolean horns) {
        /** image-10 / "Стив без лица": EXACT vanilla player proportions (all 1.0).
         *  The whole mesh is then stretched vertically x2.5 to a 5 block tall Steve. */
        public static final Shape LONGLEGS = new Shape(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
        /** image-1: dark thin figure against blue sky, still player-shaped. */
        public static final Shape BLUE_STRANGER = new Shape(0.95F, 0.95F, 1.20F, 0.95F, 1.20F, 2.10F, 0.0F, false);
        /** image-6: blurred grey humanoid, almost normal proportions. */
        public static final Shape WATCHER = new Shape(0.95F, 0.90F, 1.25F, 0.90F, 1.15F, 1.90F, 0.0F, false);
        /** image-9: a cross-shaped silhouette in the forest, arms horizontal. */
        public static final Shape WANDERER = new Shape(0.85F, 0.80F, 1.20F, 0.70F, 1.50F, 2.00F, Mth.HALF_PI, false);
        /** image-2: thin figure in a purple city. */
        public static final Shape SHE = new Shape(0.90F, 0.80F, 1.30F, 0.75F, 1.30F, 2.10F, 0.02F, false);
        /** image-12: extremely thin, horned, almost all legs. */
        public static final Shape RED_DEMON = new Shape(0.42F, 0.12F, 2.30F, 0.06F, 2.40F, 4.20F, 0.02F, true);
        public static final Shape CAVE_DEMON = RED_DEMON;
        /** image-13: Steve's head on a wide grey traffic-light column, no visible limbs. */
        public static final Shape SKY_PERSON = new Shape(1.0F, 1.30F, 1.80F, 0.12F, 0.35F, 0.60F, 0.0F, false);
        public static final Shape DANGER_12 = new Shape(0.85F, 0.75F, 1.30F, 0.70F, 1.40F, 2.10F, 0.10F, false);
        /** image-19: a solid black rectangle, small head, two yellow eyes, no legs to speak of. */
        public static final Shape ONE_EYE_GIANT = new Shape(0.80F, 1.25F, 2.60F, 0.25F, 0.50F, 0.90F, 0.0F, false);
        /** image-22: tall thin navy figure with a cube head and long legs. */
        public static final Shape NEVELIM = new Shape(0.80F, 0.60F, 1.35F, 0.18F, 1.60F, 3.20F, 0.02F, false);
        /** image-20: a black monolith with a thin blue outline and a dotted head. */
        public static final Shape EYE = new Shape(0.55F, 1.05F, 2.40F, 0.12F, 0.30F, 1.50F, 0.0F, false);
    }

    private final Shape shape;
    private final ModelPart head;
    private final ModelPart horns;
    private final ModelPart jaw;
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
                .addBox(-4.0F, -12.0F, -0.5F, 1.0F, 4.0F, 1.0F).addBox(3.0F, -12.0F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3.0F, -2.0F, -5.0F, 6.0F, 2.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
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

    /** Model space: y grows downwards and the feet sit at y = 24. Stretch everything
     *  relative to the feet so a 2 block Steve becomes the 5 block tall LONGLEGS. */
    private static float stretchY(float y, float k) {
        return 24.0F + (y - 24.0F) * k;
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

        // the faceless Steve is the vanilla player mesh, uniformly stretched to 5 blocks
        float k = s == Shape.LONGLEGS ? 2.5F : 1.0F;
        place(this.head, 0.0F, stretchY(bodyTop, k), s.head, s.head * k, s.head);
        place(this.body, 0.0F, stretchY(bodyTop, k), s.bodyWidth, s.bodyHeight * k, s.bodyWidth);
        place(this.rightArm, -(halfBody + s.limbWidth), stretchY(bodyTop + 2.0F * s.armLength, k), s.limbWidth, s.armLength * k, s.limbWidth);
        place(this.leftArm, halfBody + s.limbWidth, stretchY(bodyTop + 2.0F * s.armLength, k), s.limbWidth, s.armLength * k, s.limbWidth);
        place(this.rightLeg, -legX, stretchY(legTop, k), s.limbWidth, s.legLength * k, s.limbWidth);
        place(this.leftLeg, legX, stretchY(legTop, k), s.limbWidth, s.legLength * k, s.limbWidth);

        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD;
        place(this.jaw, 0.0F, stretchY(bodyTop, k), s.head, s.head * k, s.head);
        this.jaw.visible = s.horns;
        this.jaw.yRot = this.head.yRot;
        this.jaw.xRot = this.head.xRot;
        place(this.horns, 0.0F, stretchY(bodyTop, k), s.head, s.head * k, s.head);
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
        this.body.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.rightArm.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.leftArm.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.rightLeg.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.leftLeg.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}

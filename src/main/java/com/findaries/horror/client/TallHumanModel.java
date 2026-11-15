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
 * 0.12.0: every apparition now has its OWN geometry taken from its reference photo,
 * not just a differently scaled Steve. Extra 3D parts (eyes, ribs, traffic lamps,
 * head dots) are switched on per creature by the flags below.
 */
public class TallHumanModel<T extends Mob> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(FinDaries.rl("tall_human"), "main");

    /** Geometry flags, combined with |. */
    public static final int EYES       = 1;   // two glowing eye cubes on the face
    public static final int RIBS       = 2;   // four rib bars across the chest
    public static final int DOTS       = 4;   // dotted head (image-20)
    public static final int TRAFFIC    = 8;   // red / green lamp cubes (image-13)
    public static final int NO_ARMS    = 16;
    public static final int NO_LEGS    = 32;
    public static final int WIDE_HEAD  = 64;  // flat wide head (image-19)
    public static final int GLOW       = 128; // render at full block light
    public static final int FLAT       = NO_ARMS | NO_LEGS;

    /** All factors are relative to a normal player (legs/arms/body 12 px, limbs 4 px thick). */
    public record Shape(float head, float bodyWidth, float bodyHeight, float limbWidth,
                        float armLength, float legLength, float armSpread, boolean horns,
                        int flags, float headTilt) {
        /** image-10: faceless Steve, 5 blocks tall = vanilla mesh stretched x2.5. */
        public static final Shape LONGLEGS = new Shape(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, false, 0, 0.0F);
        /** plain player mesh, used by the False Players with the real player skin. */
        public static final Shape PLAYER = new Shape(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, false, 0, 0.0F);
        /** image-1: thin dark figure on blue sky. */
        public static final Shape BLUE_STRANGER = new Shape(0.95F, 0.95F, 1.20F, 0.95F, 1.20F, 2.10F, 0.0F, false, 0, 0.0F);
        /** image-6: blurred grey humanoid with two pale eyes. */
        public static final Shape WATCHER = new Shape(0.95F, 0.90F, 1.25F, 0.90F, 1.15F, 1.90F, 0.0F, false, EYES, 0.0F);
        /** image-9: cross silhouette in the forest, arms horizontal. */
        public static final Shape WANDERER = new Shape(0.85F, 0.80F, 1.20F, 0.70F, 1.50F, 2.00F, Mth.HALF_PI, false, 0, 0.08F);
        /** image-2: thin figure in a purple city, faint glowing eyes. */
        public static final Shape SHE = new Shape(0.90F, 0.80F, 1.30F, 0.75F, 1.30F, 2.10F, 0.02F, false, EYES | GLOW, 0.0F);
        /** image-12: extremely thin horned demon, glowing eyes and real 3D ribs. */
        public static final Shape RED_DEMON = new Shape(0.42F, 0.12F, 2.30F, 0.06F, 2.40F, 4.20F, 0.02F, true,
                EYES | RIBS | GLOW, 0.0F);
        public static final Shape CAVE_DEMON = RED_DEMON;
        /** image-13: Steve head on a grey traffic-light column, no arms or legs. */
        public static final Shape SKY_PERSON = new Shape(1.0F, 1.30F, 1.80F, 0.12F, 0.35F, 0.60F, 0.0F, false,
                TRAFFIC | FLAT, 0.0F);
        public static final Shape DANGER_12 = new Shape(0.85F, 0.75F, 1.30F, 0.70F, 1.40F, 2.10F, 0.10F, false,
                EYES | WIDE_HEAD, 0.22F);
        /** image-19: solid black rectangle, small wide head, two yellow eyes, no limbs. */
        public static final Shape ONE_EYE_GIANT = new Shape(0.80F, 1.35F, 2.70F, 0.25F, 0.50F, 0.90F, 0.0F, false,
                EYES | WIDE_HEAD | FLAT | GLOW, 0.0F);
        /** image-22: tall thin navy figure, cube head, white eyes. */
        public static final Shape NEVELIM = new Shape(0.80F, 0.60F, 1.35F, 0.18F, 1.60F, 3.20F, 0.02F, false,
                EYES | GLOW, 0.0F);
        /** image-20: black monolith with a dotted head, no limbs. */
        public static final Shape EYE = new Shape(0.55F, 1.05F, 2.40F, 0.12F, 0.30F, 1.50F, 0.0F, false,
                DOTS | EYES | FLAT | GLOW, 0.0F);

        public boolean has(int flag) { return (this.flags & flag) != 0; }
    }

    private final Shape shape;
    private final ModelPart head, horns, jaw, body;
    private final ModelPart rightArm, leftArm, rightLeg, leftLeg;
    private final ModelPart eyes, ribs, dots, traffic;

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
        this.eyes = root.getChild("eyes");
        this.ribs = root.getChild("ribs");
        this.dots = root.getChild("dots");
        this.traffic = root.getChild("traffic");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartPose zero = PartPose.offset(0.0F, 0.0F, 0.0F);
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), zero);
        root.addOrReplaceChild("horns", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4.0F, -12.0F, -0.5F, 1.0F, 4.0F, 1.0F).addBox(3.0F, -12.0F, -0.5F, 1.0F, 4.0F, 1.0F), zero);
        root.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3.0F, -2.0F, -5.0F, 6.0F, 2.0F, 1.0F), zero);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F), zero);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), zero);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), zero);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), zero);
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), zero);
        // ---- new 3D details, each with its own UV patch in the free overlay zones ----
        root.addOrReplaceChild("eyes", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-2.6F, -1.0F, -0.3F, 1.6F, 1.6F, 0.6F)
                .addBox(1.0F, -1.0F, -0.3F, 1.6F, 1.6F, 0.6F), zero);
        root.addOrReplaceChild("ribs", CubeListBuilder.create().texOffs(0, 40)
                .addBox(-3.0F, 0.0F, -0.3F, 6.0F, 0.6F, 0.6F)
                .addBox(-3.0F, 2.5F, -0.3F, 6.0F, 0.6F, 0.6F)
                .addBox(-3.0F, 5.0F, -0.3F, 6.0F, 0.6F, 0.6F)
                .addBox(-3.0F, 7.5F, -0.3F, 6.0F, 0.6F, 0.6F), zero);
        root.addOrReplaceChild("dots", CubeListBuilder.create().texOffs(16, 32)
                .addBox(-2.8F, -3.0F, -0.25F, 0.8F, 0.8F, 0.5F)
                .addBox(-0.9F, -3.6F, -0.25F, 0.8F, 0.8F, 0.5F)
                .addBox(1.2F, -2.7F, -0.25F, 0.8F, 0.8F, 0.5F)
                .addBox(-2.0F, -1.2F, -0.25F, 0.8F, 0.8F, 0.5F)
                .addBox(0.1F, -0.8F, -0.25F, 0.8F, 0.8F, 0.5F)
                .addBox(2.2F, -0.3F, -0.25F, 0.8F, 0.8F, 0.5F), zero);
        root.addOrReplaceChild("traffic", CubeListBuilder.create().texOffs(24, 32)
                .addBox(-2.0F, 0.0F, -0.35F, 4.0F, 3.4F, 0.7F)
                .addBox(-2.0F, 4.6F, -0.35F, 4.0F, 3.4F, 0.7F), zero);
        return LayerDefinition.create(mesh, 64, 64);
    }

    public boolean glows() { return this.shape.has(GLOW) || this.shape.horns(); }

    private static float stretchY(float y, float k) { return 24.0F + (y - 24.0F) * k; }

    private static void place(ModelPart part, float x, float y, float sx, float sy, float sz) {
        placeAt(part, x, y, 0.0F, sx, sy, sz);
    }

    private static void placeAt(ModelPart part, float x, float y, float z, float sx, float sy, float sz) {
        part.x = x; part.y = y; part.z = z;
        part.xRot = 0.0F; part.yRot = 0.0F; part.zRot = 0.0F;
        part.xScale = sx; part.yScale = sy; part.zScale = sz;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        Shape s = this.shape;
        float legTop = 24.0F - 12.0F * s.legLength;
        float bodyTop = legTop - 12.0F * s.bodyHeight;
        float halfBody = 4.0F * s.bodyWidth;
        float legX = Math.max(1.9F * s.bodyWidth, 2.0F * s.limbWidth + 0.05F);
        float k = s == Shape.LONGLEGS ? 2.5F : 1.0F;
        float headY = stretchY(bodyTop, k);
        float headYS = s.head * k;
        float bodyYS = s.bodyHeight * k;
        float headZ = 4.0F * s.head;
        float bodyZ = 2.0F * s.bodyWidth;

        place(this.head, 0.0F, headY, s.head * (s.has(WIDE_HEAD) ? 1.5F : 1.0F), headYS, s.head);
        place(this.body, 0.0F, headY, s.bodyWidth, bodyYS, s.bodyWidth);
        place(this.rightArm, -(halfBody + s.limbWidth), stretchY(bodyTop + 2.0F * s.armLength, k), s.limbWidth, s.armLength * k, s.limbWidth);
        place(this.leftArm, halfBody + s.limbWidth, stretchY(bodyTop + 2.0F * s.armLength, k), s.limbWidth, s.armLength * k, s.limbWidth);
        place(this.rightLeg, -legX, stretchY(legTop, k), s.limbWidth, s.legLength * k, s.limbWidth);
        place(this.leftLeg, legX, stretchY(legTop, k), s.limbWidth, s.legLength * k, s.limbWidth);

        this.rightArm.visible = !s.has(NO_ARMS);
        this.leftArm.visible = !s.has(NO_ARMS);
        this.rightLeg.visible = !s.has(NO_LEGS);
        this.leftLeg.visible = !s.has(NO_LEGS);

        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD;
        this.head.zRot = s.headTilt();
        place(this.jaw, 0.0F, headY, s.head, headYS, s.head);
        this.jaw.visible = s.horns();
        this.jaw.yRot = this.head.yRot; this.jaw.xRot = this.head.xRot; this.jaw.zRot = this.head.zRot;
        place(this.horns, 0.0F, headY, s.head, headYS, s.head);
        this.horns.visible = s.horns();
        this.horns.yRot = this.head.yRot; this.horns.xRot = this.head.xRot; this.horns.zRot = this.head.zRot;

        // ---- photo-specific 3D details, sitting on the FRONT faces ----
        placeAt(this.eyes, 0.0F, headY - 4.6F * headYS, -(headZ + 0.35F), s.head, headYS, s.head);
        this.eyes.visible = s.has(EYES);
        this.eyes.yRot = this.head.yRot; this.eyes.xRot = this.head.xRot; this.eyes.zRot = this.head.zRot;
        placeAt(this.dots, 0.0F, headY - 5.6F * headYS, -(headZ + 0.30F), s.head, headYS, 1.0F);
        this.dots.visible = s.has(DOTS);
        this.dots.yRot = this.head.yRot; this.dots.xRot = this.head.xRot; this.dots.zRot = this.head.zRot;
        placeAt(this.ribs, 0.0F, headY + 1.2F, -(bodyZ + 0.35F), s.bodyWidth, bodyYS / 12.0F, 1.0F);
        this.ribs.visible = s.has(RIBS);
        placeAt(this.traffic, 0.0F, headY + 2.2F, -(bodyZ + 0.40F), Math.max(0.6F, s.bodyWidth), bodyYS / 12.0F, 1.0F);
        this.traffic.visible = s.has(TRAFFIC);

        float stride = 0.6662F / Mth.sqrt(s.legLength);
        float swing = Mth.cos(limbSwing * stride) * 0.8F * limbSwingAmount;
        this.rightLeg.xRot = swing; this.leftLeg.xRot = -swing;
        this.rightArm.xRot = -swing * 0.6F; this.leftArm.xRot = swing * 0.6F;
        float sway = Mth.cos(ageInTicks * 0.06F) * 0.03F;
        this.rightArm.zRot = s.armSpread + sway;
        this.leftArm.zRot = -s.armSpread - sway;
        if (s.armSpread > 1.4F && ageInTicks > 150.0F && ageInTicks < 450.0F) {
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
        this.eyes.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.dots.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.body.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.ribs.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.traffic.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.rightArm.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.leftArm.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.rightLeg.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.leftLeg.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}

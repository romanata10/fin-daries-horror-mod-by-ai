package com.findaries.horror.client;

import com.findaries.horror.FinDaries;
import com.findaries.horror.entity.PillarEntity;
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

/** Three blocks tall, one block wide, two eyes - and a belly that opens into a portal. */
public class PillarModel extends EntityModel<PillarEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(FinDaries.rl("pillar"), "main");

    private final ModelPart body;
    private final ModelPart eyes;
    private final ModelPart bellyLeft;
    private final ModelPart bellyRight;
    private final ModelPart inside;
    private final ModelPart brows;
    private final ModelPart teeth;

    public PillarModel(ModelPart root) {
        this.body = root.getChild("body");
        this.eyes = root.getChild("eyes");
        this.bellyLeft = root.getChild("belly_left");
        this.bellyRight = root.getChild("belly_right");
        this.inside = root.getChild("inside");
        this.brows = root.getChild("brows");
        this.teeth = root.getChild("teeth");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartPose base = PartPose.offset(0.0F, 24.0F, 0.0F);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-8.0F, -48.0F, -8.0F, 16.0F, 48.0F, 16.0F), base);
        root.addOrReplaceChild("eyes", CubeListBuilder.create().texOffs(0, 48)
                .addBox(-5.0F, -42.0F, -9.0F, 3.0F, 3.0F, 1.0F)
                .addBox(2.0F, -42.0F, -9.0F, 3.0F, 3.0F, 1.0F), base);
        root.addOrReplaceChild("belly_left", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-8.0F, -24.0F, -9.0F, 8.0F, 14.0F, 1.0F), base);
        root.addOrReplaceChild("belly_right", CubeListBuilder.create().texOffs(0, 0)
                .addBox(0.0F, -24.0F, -9.0F, 8.0F, 14.0F, 1.0F), base);
        root.addOrReplaceChild("inside", CubeListBuilder.create().texOffs(16, 48)
                .addBox(-7.0F, -24.0F, -8.5F, 14.0F, 14.0F, 0.5F), base);
        root.addOrReplaceChild("brows", CubeListBuilder.create().texOffs(0, 48)
                .addBox(-6.0F, -38.5F, -9.2F, 5.0F, 1.0F, 1.0F)
                .addBox(1.0F, -38.5F, -9.2F, 5.0F, 1.0F, 1.0F), base);
        root.addOrReplaceChild("teeth", CubeListBuilder.create().texOffs(16, 48)
                .addBox(-6.0F, -23.0F, -9.2F, 2.0F, 3.0F, 1.0F)
                .addBox(-2.0F, -23.0F, -9.2F, 2.0F, 3.0F, 1.0F)
                .addBox(2.0F, -23.0F, -9.2F, 2.0F, 3.0F, 1.0F)
                .addBox(6.0F, -23.0F, -9.2F, 2.0F, 3.0F, 1.0F), base);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(PillarEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        boolean open = entity.bellyOpen();
        this.bellyLeft.x = open ? -7.0F : 0.0F;
        this.bellyRight.x = open ? 7.0F : 0.0F;
        this.inside.visible = open;
        this.teeth.visible = open;
        this.body.zRot = Mth.cos(ageInTicks * 0.03F) * 0.012F;
        this.body.yRot = netHeadYaw * Mth.DEG_TO_RAD;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.body.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.eyes.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.bellyLeft.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.bellyRight.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.inside.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.brows.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.teeth.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}

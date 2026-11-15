package com.findaries.horror.client;

import com.findaries.horror.FinDaries;
import com.findaries.horror.entity.FlowerManEntity;
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

/**
 * The Flower Man: a tall column of earth and grass with a yellow flower on top,
 * standing on two long green legs (user reference image).
 */
public class FlowerManModel extends EntityModel<FlowerManEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(FinDaries.rl("flower_man"), "main");
    private static final float LEGS = 30.0F;

    private final ModelPart column;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public FlowerManModel(ModelPart root) {
        this.column = root.getChild("column");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        float top = 24.0F - LEGS;
        PartDefinition column = root.addOrReplaceChild("column",
                CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -32.0F, -8.0F, 16.0F, 32.0F, 16.0F),
                PartPose.offset(0.0F, top, 0.0F));
        column.addOrReplaceChild("stem",
                CubeListBuilder.create().texOffs(64, 32).addBox(-1.0F, -12.0F, -1.0F, 2.0F, 12.0F, 2.0F),
                PartPose.offset(0.0F, -32.0F, 0.0F));
        column.addOrReplaceChild("petal_a",
                CubeListBuilder.create().texOffs(16, 32).addBox(-9.0F, -19.0F, 0.0F, 18.0F, 8.0F, 0.0F),
                PartPose.offset(0.0F, -32.0F, 0.0F));
        column.addOrReplaceChild("petal_b",
                CubeListBuilder.create().texOffs(16, 32).addBox(-9.0F, -19.0F, 0.0F, 18.0F, 8.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, -32.0F, 0.0F, 0.0F, Mth.HALF_PI, 0.0F));
        column.addOrReplaceChild("petal_c",
                CubeListBuilder.create().texOffs(16, 32).addBox(-9.0F, -19.0F, 0.0F, 18.0F, 8.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, -32.0F, 0.0F, 0.0F, 0.7853982F, 0.0F));
        column.addOrReplaceChild("petal_d",
                CubeListBuilder.create().texOffs(16, 32).addBox(-9.0F, -19.0F, 0.0F, 18.0F, 8.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, -32.0F, 0.0F, 0.0F, -0.7853982F, 0.0F));
        column.addOrReplaceChild("core",
                CubeListBuilder.create().texOffs(48, 32).addBox(-4.0F, -19.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, -32.0F, 0.0F));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 64).addBox(-1.5F, 0.0F, -1.5F, 3.0F, LEGS, 3.0F),
                PartPose.offset(-4.0F, top, 0.0F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 64).addBox(-1.5F, 0.0F, -1.5F, 3.0F, LEGS, 3.0F),
                PartPose.offset(4.0F, top, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(FlowerManEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.column.zRot = Mth.cos(ageInTicks * 0.05F) * 0.03F;
        this.column.xRot = Mth.sin(ageInTicks * 0.037F) * 0.018F;
        float swing = Mth.cos(limbSwing * 0.3F) * 0.6F * limbSwingAmount;
        this.rightLeg.xRot = swing;
        this.leftLeg.xRot = -swing;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.column.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.rightLeg.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.leftLeg.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}

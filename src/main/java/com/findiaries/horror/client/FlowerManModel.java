package com.findiaries.horror.client;

import com.findiaries.horror.FinDiaries;
import com.findiaries.horror.entity.FlowerManEntity;
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

/** A grass block with a flower on top, standing on two long green legs. */
public class FlowerManModel extends EntityModel<FlowerManEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(FinDiaries.rl("flower_man"), "main");
    private static final float LEGS = 28.0F;

    private final ModelPart body;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public FlowerManModel(ModelPart root) {
        this.body = root.getChild("body");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        float top = 24.0F - LEGS;
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -16.0F, -8.0F, 16.0F, 16.0F, 16.0F), PartPose.offset(0.0F, top, 0.0F));
        body.addOrReplaceChild("flower_a",
                CubeListBuilder.create().texOffs(16, 32).addBox(-4.0F, -8.0F, 0.0F, 8.0F, 8.0F, 0.0F), PartPose.offset(0.0F, -16.0F, 0.0F));
        body.addOrReplaceChild("flower_b",
                CubeListBuilder.create().texOffs(16, 32).addBox(-4.0F, -8.0F, 0.0F, 8.0F, 8.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, -16.0F, 0.0F, 0.0F, Mth.HALF_PI, 0.0F));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 32).addBox(-1.5F, 0.0F, -1.5F, 3.0F, LEGS, 3.0F), PartPose.offset(-4.0F, top, 0.0F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 32).addBox(-1.5F, 0.0F, -1.5F, 3.0F, LEGS, 3.0F), PartPose.offset(4.0F, top, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(FlowerManEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.body.zRot = Mth.cos(ageInTicks * 0.05F) * 0.035F;
        this.body.xRot = Mth.sin(ageInTicks * 0.037F) * 0.02F;
        float swing = Mth.cos(limbSwing * 0.3F) * 0.6F * limbSwingAmount;
        this.rightLeg.xRot = swing;
        this.leftLeg.xRot = -swing;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.body.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.rightLeg.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.leftLeg.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}

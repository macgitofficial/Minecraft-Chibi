package net.mac.projectmod.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class ChibiRenderer extends GeoEntityRenderer<ChibiEntity> {

    public ChibiRenderer(EntityRendererProvider.Context context) {
        super(context, new ChibiModel());

        this.shadowRadius = 0.5F;

        this.addRenderLayer(
                new BlockAndItemGeoLayer<ChibiEntity>(this) {

                    @Override
                    protected ItemStack getStackForBone(
                            GeoBone bone,
                            ChibiEntity animatable
                    ) {
                        return switch (bone.getName()) {
                            case "RightHandLocator" ->
                                    animatable.getMainHandItem();
                            case "LeftHandLocator" ->
                                    animatable.getOffhandItem();
                            default ->
                                    ItemStack.EMPTY;
                        };
                    }

                    @Override
                    protected ItemDisplayContext getTransformTypeForStack(
                            GeoBone bone,
                            ItemStack stack,
                            ChibiEntity animatable
                    ) {
                        return switch (bone.getName()) {
                            case "RightHandLocator" -> ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
                            case "LeftHandLocator" -> ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
                            default -> ItemDisplayContext.NONE;
                        };
                    }

                    @Override
                    protected void renderStackForBone(
                            PoseStack poseStack,
                            GeoBone bone,
                            ItemStack stack,
                            ChibiEntity animatable,
                            MultiBufferSource bufferSource,
                            float partialTick,
                            int packedLight,
                            int packedOverlay
                    ) {
                        poseStack.pushPose();
                        if (bone.getName().equals("RightHandLocator")) {
                            poseStack.mulPose(
                                    com.mojang.math.Axis.XP.rotationDegrees(-98.0F)
                            );
                            poseStack.translate(-0.12D, 0.12D, 0.0D);
                        }

                        if (bone.getName().equals("LeftHandLocator")) {
                            poseStack.mulPose(
                                    com.mojang.math.Axis.ZP.rotationDegrees(179.0F)
                            );
                            poseStack.translate(-0.15D, -0.13D, -0.3D);
                        }
//                        if (stack.getItem() instanceof ShieldItem) {
//                            if (bone.getName().equals("RightHandLocator")) {
//                                poseStack.mulPose(
//                                        Axis.XP.rotationDegrees(-100.0F)
//                                );
//
//                                poseStack.mulPose(
//                                        Axis.ZP.rotationDegrees(90.0F)
//                                );
//
//                                poseStack.translate(
//                                        -0.08D,
//                                        0.05D,
//                                        0.12D
//                                );
//                            }
//
//                            if (bone.getName().equals("LeftHandLocator")) {
//                                poseStack.mulPose(
//                                        Axis.XP.rotationDegrees(100.0F)
//                                );
//
//                                poseStack.mulPose(
//                                        Axis.ZN.rotationDegrees(90.0F)
//                                );
//
//                                poseStack.translate(
//                                        0.08D,
//                                        0.05D,
//                                        0.12D
//                                );
//                            }
//                        }
                        super.renderStackForBone(
                                poseStack,
                                bone,
                                stack,
                                animatable,
                                bufferSource,
                                partialTick,
                                packedLight,
                                packedOverlay
                        );

                        poseStack.popPose();
                    }
                }
        );
    }
}
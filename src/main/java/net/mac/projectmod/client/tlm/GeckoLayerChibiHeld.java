package net.mac.projectmod.client.tlm;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.processor.ILocationBone;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.GeoLayerRenderer;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.IGeoEntityRenderer;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.ILocationModel;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.mac.projectmod.entity.ChibiEntity;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * TLM-style vanilla held-item layer for Chibi.
 *
 * This intentionally ports only the normal Minecraft item path from
 * TLM's GeckoLayerMaidHeld. TLM compatibility renderers and other
 * special-item integrations are deliberately not included.
 */
public final class GeckoLayerChibiHeld extends GeoLayerRenderer<ChibiEntity, TlmChibiRenderer> {
    private final ItemInHandRenderer itemInHandRenderer;

    public GeckoLayerChibiHeld(TlmChibiRenderer renderer, ItemInHandRenderer itemInHandRenderer) {
        super(renderer);
        this.itemInHandRenderer = itemInHandRenderer;
    }

    @Override
    public GeoLayerRenderer<ChibiEntity, TlmChibiRenderer> copy(TlmChibiRenderer renderer) {
        return new GeckoLayerChibiHeld(renderer, this.itemInHandRenderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       ChibiEntity entity, float limbSwing, float limbSwingAmount, float partialTicks,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack offhandItem = entity.getOffhandItem();
        ItemStack mainHandItem = entity.getMainHandItem();

        ILocationModel geoModel = getLocationModel(entity);
        if (geoModel == null || (offhandItem.isEmpty() && mainHandItem.isEmpty())) {
            return;
        }

        poseStack.pushPose();
        if (!geoModel.rightHandBones().isEmpty()) {
            renderArmWithItem(entity, mainHandItem, geoModel,
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, HumanoidArm.RIGHT,
                    poseStack, bufferSource, packedLight);
        }
        if (!geoModel.leftHandBones().isEmpty()) {
            renderArmWithItem(entity, offhandItem, geoModel,
                    ItemDisplayContext.THIRD_PERSON_LEFT_HAND, HumanoidArm.LEFT,
                    poseStack, bufferSource, packedLight);
        }
        poseStack.popPose();
    }

    private void renderArmWithItem(ChibiEntity entity, ItemStack itemStack, ILocationModel geoModel,
                                   ItemDisplayContext displayContext, HumanoidArm arm,
                                   PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        if (itemStack.isEmpty()) {
            return;
        }

        boolean isLeftHand = arm == HumanoidArm.LEFT;
        poseStack.pushPose();

        boolean scaleResult = translateToHand(arm, poseStack, geoModel);
        if (!scaleResult) {
            poseStack.translate(0, -0.0625D, -0.1D);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            itemInHandRenderer.renderItem(entity, itemStack, displayContext, isLeftHand,
                    poseStack, bufferSource, light);
        }

        poseStack.popPose();
    }

    private boolean translateToHand(HumanoidArm arm, PoseStack poseStack, ILocationModel geoModel) {
        return arm == HumanoidArm.LEFT
                ? RenderUtils.prepMatrixForLocator(poseStack, geoModel.leftHandBones())
                : RenderUtils.prepMatrixForLocator(poseStack, geoModel.rightHandBones());
    }
}

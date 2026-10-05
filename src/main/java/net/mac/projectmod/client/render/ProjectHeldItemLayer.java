/**
 * ProjectMod adaptation of rendering/animation code originally derived from
 * Touhou Little Maid. See LICENSE-TLM-MIT.txt and THIRD-PARTY-NOTICES.md for
 * upstream attribution and license. ProjectMod changes are maintained by the
 * ProjectMod project.
 */
package net.mac.projectmod.client.render;

import net.mac.projectmod.client.render.legacy.geckolib3.core.processor.ILocationBone;
import net.mac.projectmod.client.render.legacy.geckolib3.geo.GeoLayerRenderer;
import net.mac.projectmod.client.render.legacy.geckolib3.geo.IGeoEntityRenderer;
import net.mac.projectmod.client.render.legacy.geckolib3.geo.animated.ILocationModel;
import net.mac.projectmod.client.render.legacy.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.mac.projectmod.entity.ChibiEntity;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * TLM-style vanilla held-item layer for Chibi.
 *
 * This intentionally ports only the normal Minecraft item path from
 * TLM's GeckoLayerMaidHeld. TLM compatibility renderers and other
 * special-item integrations are deliberately not included.
 */
public final class ProjectHeldItemLayer extends GeoLayerRenderer<ChibiEntity, ProjectChibiRenderer> {
    private final ItemInHandRenderer itemInHandRenderer;
    private final ItemRenderer itemRenderer;

    public ProjectHeldItemLayer(ProjectChibiRenderer renderer, ItemInHandRenderer itemInHandRenderer, ItemRenderer itemRenderer) {
        super(renderer);
        this.itemInHandRenderer = itemInHandRenderer;
        this.itemRenderer = itemRenderer;
    }

    @Override
    public GeoLayerRenderer<ChibiEntity, ProjectChibiRenderer> copy(ProjectChibiRenderer renderer) {
        return new ProjectHeldItemLayer(renderer, this.itemInHandRenderer, this.itemRenderer);
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
            renderVanillaHeldItem(entity, itemStack, displayContext, isLeftHand,
                    poseStack, bufferSource, light);
        }

        poseStack.popPose();
    }

    private boolean translateToHand(HumanoidArm arm, PoseStack poseStack, ILocationModel geoModel) {
        return arm == HumanoidArm.LEFT
                ? RenderUtils.prepMatrixForLocator(poseStack, geoModel.leftHandBones())
                : RenderUtils.prepMatrixForLocator(poseStack, geoModel.rightHandBones());
    }
    /**
     * Fishing rods are special in vanilla: their item model has a `cast` override
     * which switches to fishing_rod_cast when a bobber is attached. Chibi already
     * renders the actual fishing line from ProjectFishingHook -> owner, so using
     * the vanilla cast model here would leave an extra baked line segment on the rod.
     * Render the base fishing_rod model directly to bypass ItemOverrides.
     */
    private void renderVanillaHeldItem(ChibiEntity entity, ItemStack itemStack,
                                       ItemDisplayContext displayContext, boolean isLeftHand,
                                       PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        if (itemStack.is(Items.FISHING_ROD)) {
            BakedModel baseModel = itemRenderer.getItemModelShaper().getItemModel(Items.FISHING_ROD);
            itemRenderer.render(
                    itemStack,
                    displayContext,
                    isLeftHand,
                    poseStack,
                    bufferSource,
                    light,
                    OverlayTexture.NO_OVERLAY,
                    baseModel
            );
            return;
        }

        itemInHandRenderer.renderItem(
                entity, itemStack, displayContext, isLeftHand,
                poseStack, bufferSource, light
        );
    }

}

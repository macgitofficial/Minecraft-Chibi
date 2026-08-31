package net.mac.projectmod.entity;

import net.mac.projectmod.ProjectMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class ChibiModel extends GeoModel<ChibiEntity> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ProjectMod.MOD_ID, "geo/guga.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ProjectMod.MOD_ID, "textures/entity/guga.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ProjectMod.MOD_ID, "animations/guga.animation.json");

    @Override
    public ResourceLocation getModelResource(ChibiEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ChibiEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ChibiEntity animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(
            ChibiEntity animatable,
            long instanceId,
            AnimationState<ChibiEntity> animationState
    ) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        GeoBone head = this.getAnimationProcessor().getBone("Head");

        if (head != null) {
            EntityModelData extraData =
                    animationState.getData(DataTickets.ENTITY_MODEL_DATA);

            head.setRotX(extraData.headPitch() * ((float)Math.PI / 180F));
            head.setRotY(extraData.netHeadYaw() * ((float)Math.PI / 180F));
        }
    }
}
package net.mac.projectmod.entity;

import net.mac.projectmod.ProjectMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ChibiModel extends GeoModel<ChibiEntity> {

    @Override
    public ResourceLocation getModelResource(ChibiEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ProjectMod.MOD_ID,
                "geo/guga.geo.json"
        );
    }

    @Override
    public ResourceLocation getTextureResource(ChibiEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ProjectMod.MOD_ID,
                "textures/entity/guga.png"
        );
    }

    @Override
    public ResourceLocation getAnimationResource(ChibiEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ProjectMod.MOD_ID,
                "animations/guga.animation.json"
        );
    }
}
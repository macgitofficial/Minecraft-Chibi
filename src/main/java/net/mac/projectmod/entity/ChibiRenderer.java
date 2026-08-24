package net.mac.projectmod.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ChibiRenderer extends GeoEntityRenderer<ChibiEntity> {

    public ChibiRenderer(EntityRendererProvider.Context context) {
        super(context, new ChibiModel());

        this.shadowRadius = 0.5F;
    }
}
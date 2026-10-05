package net.mac.projectmod.client.render.legacy.geckolib3.geo;

import net.mac.projectmod.client.render.legacy.geckolib3.geo.animated.ILocationModel;

/** ProjectMod-specific minimal contract required by the renderer pipeline. */
public interface IGeoEntity {
    ILocationModel getGeoModel();
}

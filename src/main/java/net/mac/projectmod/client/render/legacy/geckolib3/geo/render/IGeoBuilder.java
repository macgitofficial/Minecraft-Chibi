package net.mac.projectmod.client.render.legacy.geckolib3.geo.render;

import net.mac.projectmod.client.render.legacy.geckolib3.geo.raw.tree.RawGeometryTree;
import net.mac.projectmod.client.render.legacy.geckolib3.geo.render.built.GeoModel;


public interface IGeoBuilder {
    GeoModel constructGeoModel(RawGeometryTree geometryTree);
}

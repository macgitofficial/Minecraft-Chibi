/**
 * ProjectMod adaptation of rendering/animation code originally derived from
 * Touhou Little Maid. See LICENSE-TLM-MIT.txt and THIRD-PARTY-NOTICES.md for
 * upstream attribution and license. ProjectMod changes are maintained by the
 * ProjectMod project.
 */
package net.mac.projectmod.client.render;

import net.mac.projectmod.client.render.legacy.geckolib3.geo.GeoReplacedEntityRenderer;
import net.mac.projectmod.client.render.legacy.geckolib3.geo.GeoLayerRenderer;
import net.mac.projectmod.client.render.legacy.geckolib3.geo.IGeoEntity;
import net.mac.projectmod.client.render.legacy.geckolib3.geo.IGeoEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.mac.projectmod.entity.ChibiEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class ProjectChibiRenderer extends GeoReplacedEntityRenderer<ChibiEntity, ProjectGeckoChibiEntity> implements IGeoEntityRenderer<ChibiEntity> {
    private final Map<Integer, ProjectGeckoChibiEntity> animatables = new HashMap<>();

    public ProjectChibiRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        addLayer(new ProjectHeldItemLayer(this, context.getItemInHandRenderer(), context.getItemRenderer()));
    }

    @Override
    public ProjectGeckoChibiEntity getAnimatableEntity(ChibiEntity entity) {
        ProjectModelLoader.ensureLoaded();
        ProjectGeckoChibiEntity wrapper = animatables.computeIfAbsent(entity.getId(), id -> new ProjectGeckoChibiEntity(entity));
        if ((animatables.size() & 31) == 0) {
            purgeDeadWrappers();
        }
        return wrapper;
    }


    @Override
    public IGeoEntity getGeoEntity(ChibiEntity entity) {
        ProjectGeckoChibiEntity wrapper = getAnimatableEntity(entity);
        return new IGeoEntity() {
            @Override
            public net.mac.projectmod.client.render.legacy.geckolib3.geo.animated.ILocationModel getGeoModel() {
                return wrapper.getCurrentModel();
            }
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addGeoLayerRenderer(GeoLayerRenderer<?, ?> layerRenderer) {
        addLayer((GeoLayerRenderer<ChibiEntity, ?>) layerRenderer);
    }

    @Override
    public void geoRender(ChibiEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private void purgeDeadWrappers() {
        Iterator<Map.Entry<Integer, ProjectGeckoChibiEntity>> it = animatables.entrySet().iterator();
        while (it.hasNext()) {
            if (!it.next().getValue().getEntity().isAlive()) it.remove();
        }
    }
}

package net.mac.projectmod.client.tlm;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.GeoReplacedEntityRenderer;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.GeoLayerRenderer;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.IGeoEntity;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.IGeoEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.mac.projectmod.entity.ChibiEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class TlmChibiRenderer extends GeoReplacedEntityRenderer<ChibiEntity, GeckoChibiEntity> implements IGeoEntityRenderer<ChibiEntity> {
    private final Map<Integer, GeckoChibiEntity> animatables = new HashMap<>();

    public TlmChibiRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        addLayer(new GeckoLayerChibiHeld(this, context.getItemInHandRenderer()));
    }

    @Override
    public GeckoChibiEntity getAnimatableEntity(ChibiEntity entity) {
        TlmModelLoader.ensureLoaded();
        GeckoChibiEntity wrapper = animatables.computeIfAbsent(entity.getId(), id -> new GeckoChibiEntity(entity));
        if ((animatables.size() & 31) == 0) {
            purgeDeadWrappers();
        }
        return wrapper;
    }


    @Override
    public IGeoEntity getGeoEntity(ChibiEntity entity) {
        GeckoChibiEntity wrapper = getAnimatableEntity(entity);
        return new IGeoEntity() {
            @Override
            public com.github.tartaricacid.touhoulittlemaid.api.entity.IMaid getMaid() {
                return null;
            }

            @Override
            public com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.MaidModelInfo getMaidInfo() {
                return null;
            }

            @Override
            public com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.ILocationModel getGeoModel() {
                return wrapper.getCurrentModel();
            }

            @Override
            public void setMaidInfo(com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.MaidModelInfo info) {
            }

            @Override
            public void setYsmModel(String modelId, String texture) {
            }

            @Override
            public void updateRoamingVars(it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap<String> roamingVars) {
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
        Iterator<Map.Entry<Integer, GeckoChibiEntity>> it = animatables.entrySet().iterator();
        while (it.hasNext()) {
            if (!it.next().getValue().getEntity().isAlive()) it.remove();
        }
    }
}

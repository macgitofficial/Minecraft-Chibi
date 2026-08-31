package net.mac.projectmod.client.tlm;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.AnimatableEntity;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.controller.AnimationController;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.event.predicate.AnimationEvent;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.molang.context.AnimationContext;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.AnimatedGeoModel;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.model.provider.data.EntityModelData;
import net.mac.projectmod.ProjectMod;
import net.mac.projectmod.entity.ChibiEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;


public final class GeckoChibiEntity extends AnimatableEntity<ChibiEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ProjectMod.MOD_ID, "guga");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ProjectMod.MOD_ID, "textures/entity/guga.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ProjectMod.MOD_ID, "guga");

    private final Vector2f headRot = new Vector2f();
    private final TlmAnimationManager animationManager;
    private float lastHeadYaw;
    private float lastBodyYaw;

    public GeckoChibiEntity(ChibiEntity entity) {
        super(entity, 60);
        this.animationManager = new TlmAnimationManager();
        addAnimationController(new AnimationController<>(this, "main", 2, animationManager::predicateMain));
        addAnimationController(new AnimationController<>(this, "swing", 2, animationManager::predicateSwing));
        addAnimationController(new AnimationController<>(this, "use", 2, animationManager::predicateUse));
    }

    @Override
    public ResourceLocation getModelLocation() {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureLocation() {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationFileLocation() {
        return ANIMATION;
    }

    @Override
    public boolean setCustomAnimations(AnimationContext context, @NotNull AnimationEvent event) {
        boolean updated = super.setCustomAnimations(context, event);
        if (Minecraft.getInstance().isPaused()) return updated;
        if (!(event.getExtraData().stream().findFirst().orElse(null) instanceof EntityModelData data)) {
            return updated;
        }
        AnimatedGeoModel model = getCurrentModel();
        if (model == null || model.head() == null) return updated;
        if (updated) {
            headRot.set(model.head().getRotationX(), model.head().getRotationY());
        }
        model.head().setRotationX(headRot.x + (float) Math.toRadians(data.headPitch));
        model.head().setRotationY(headRot.y + (float) Math.toRadians(data.netHeadYaw));
        return updated;
    }

    @Override
    protected boolean forceUpdate(AnimationEvent<?> event) {
        var entity = getEntity();
        boolean changed = entity.yHeadRot != lastHeadYaw || entity.yBodyRot != lastBodyYaw;
        lastHeadYaw = entity.yHeadRot;
        lastBodyYaw = entity.yBodyRot;
        return changed;
    }
}

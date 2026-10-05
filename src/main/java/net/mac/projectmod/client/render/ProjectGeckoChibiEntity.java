/**
 * ProjectMod adaptation of rendering/animation code originally derived from
 * Touhou Little Maid. See LICENSE-TLM-MIT.txt and THIRD-PARTY-NOTICES.md for
 * upstream attribution and license. ProjectMod changes are maintained by the
 * ProjectMod project.
 */
package net.mac.projectmod.client.render;

import net.mac.projectmod.client.render.legacy.geckolib3.core.AnimatableEntity;
import net.mac.projectmod.client.render.legacy.geckolib3.core.controller.AnimationController;
import net.mac.projectmod.client.render.legacy.geckolib3.core.event.predicate.AnimationEvent;
import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.context.AnimationContext;
import net.mac.projectmod.client.render.legacy.geckolib3.geo.animated.AnimatedGeoModel;
import net.mac.projectmod.client.render.legacy.geckolib3.model.provider.data.EntityModelData;
import net.mac.projectmod.ProjectMod;
import net.mac.projectmod.entity.ChibiEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;


public final class ProjectGeckoChibiEntity extends AnimatableEntity<ChibiEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ProjectMod.MOD_ID, "guga");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ProjectMod.MOD_ID, "textures/entity/guga.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ProjectMod.MOD_ID, "guga");

    private final Vector2f headRot = new Vector2f();
    private final ProjectAnimationManager animationManager;
    private float lastHeadYaw;
    private float lastBodyYaw;
    private boolean lastFishingState;
    private boolean lastSwingingState;
    private boolean lastUsingState;

    public ProjectGeckoChibiEntity(ChibiEntity entity) {
        super(entity, 60);
        this.animationManager = new ProjectAnimationManager();
        addAnimationController(new AnimationController<>(this, "main", 2, animationManager::predicateMain));
        addAnimationController(new AnimationController<>(this, "hold_mainhand", 0, animationManager::predicateMainhandHold));
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
        boolean fishingChanged = entity.hasFishingHook() != lastFishingState;
        boolean swingingChanged = entity.swinging != lastSwingingState;
        boolean usingChanged = entity.isUsingItem() != lastUsingState;
        boolean changed = entity.yHeadRot != lastHeadYaw
                || entity.yBodyRot != lastBodyYaw
                || fishingChanged
                || swingingChanged
                || usingChanged;
        lastHeadYaw = entity.yHeadRot;
        lastBodyYaw = entity.yBodyRot;
        lastFishingState = entity.hasFishingHook();
        lastSwingingState = entity.swinging;
        lastUsingState = entity.isUsingItem();
        return changed;
    }
}

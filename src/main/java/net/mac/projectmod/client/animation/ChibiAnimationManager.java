package net.mac.projectmod.client.animation;

import net.mac.projectmod.entity.ChibiEntity;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Central animation manager for Gugu Gaga.
 *
 * This intentionally follows the architecture used by Touhou Little Maid:
 * one main locomotion controller plus independent controllers reserved for
 * held-item/use/swing animations.  Fishing is NOT enabled here yet; that is
 * the next step after the model/controller structure is verified.
 */
public final class ChibiAnimationManager {

    private ChibiAnimationManager() {}

    public static final RawAnimation IDLE =
            RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation WALK =
            RawAnimation.begin().thenLoop("walk");
    public static final RawAnimation SIT =
            RawAnimation.begin().thenLoop("sit");
    public static final RawAnimation SLEEP =
            RawAnimation.begin().thenLoop("sleep");
    public static final RawAnimation SWIM_STAND =
            RawAnimation.begin().thenLoop("swim_stand");
    public static final RawAnimation BOAT =
            RawAnimation.begin().thenLoop("boat");
    public static final RawAnimation JUMP =
            RawAnimation.begin().thenLoop("jump");
    public static final RawAnimation DEATH =
            RawAnimation.begin().thenPlay("death");
    public static final RawAnimation ATTACKED =
            RawAnimation.begin().thenPlay("attacked");
    public static final RawAnimation SWING_HAND =
            RawAnimation.begin().thenPlay("swing_hand");
    public static final RawAnimation FISHING =
            RawAnimation.begin().thenLoop("hold_mainhand:fishing");

    /**
     * TLM-like controller layout.  Only the main controller has active
     * gameplay logic for now.  The other controllers are intentionally
     * registered so the model architecture is ready for item/fishing states.
     */
    public static void registerControllers(
            ChibiEntity chibi,
            AnimatableManager.ControllerRegistrar controllers) {

        controllers.add(
                new AnimationController<>(
                        chibi,
                        "main",
                        2,
                        ChibiAnimationManager::predicateMain
                )
                        .triggerableAnim("attacked", ATTACKED)
                        .triggerableAnim("swing_hand", SWING_HAND)
        );

        controllers.add(new AnimationController<>(
                chibi, "hold_mainhand", 0, ChibiAnimationManager::predicateHoldMainhand));

        controllers.add(new AnimationController<>(
                chibi, "hold_offhand", 0, ChibiAnimationManager::predicateHoldOffhand));

        controllers.add(new AnimationController<>(
                chibi, "swing", 2, ChibiAnimationManager::predicateSwing));

        controllers.add(new AnimationController<>(
                chibi, "use", 2, ChibiAnimationManager::predicateUse));
    }

    public static PlayState predicateMain(AnimationState<ChibiEntity> state) {
        ChibiEntity chibi = state.getAnimatable();

        if (chibi.isDeadOrDying()) {
            state.getController().setAnimation(DEATH);
        } else if (chibi.isPassenger()) {
            state.getController().setAnimation(BOAT);
        } else if (chibi.isSleeping()) {
            state.getController().setAnimation(SLEEP);
        } else if (chibi.isSittingByServer()) {
            state.getController().setAnimation(SIT);
        } else if (chibi.isSwimming() || chibi.isInWater()) {
            state.getController().setAnimation(SWIM_STAND);
        } else if (!chibi.onGround()) {
            state.getController().setAnimation(JUMP);
        } else if (chibi.isWalkingByAI()) {
            state.getController().setAnimation(WALK);
        } else {
            state.getController().setAnimation(IDLE);
        }

        return PlayState.CONTINUE;
    }

    // TLM-style item/hand animation layer. Fishing is driven by the active
    // ProjectFishingHook state synced by the GoalSelector fishing goal.
    private static PlayState predicateHoldMainhand(AnimationState<ChibiEntity> state) {
        ChibiEntity chibi = state.getAnimatable();

        // Mirrors TLM 1.21: keep the fishing pose active while the bobber exists,
        // but yield to swing/use animations.
        if (chibi.hasFishingHook() && !chibi.swinging && !chibi.isUsingItem()) {
            state.getController().setAnimation(FISHING);
            return PlayState.CONTINUE;
        }

        return PlayState.STOP;
    }

    private static PlayState predicateHoldOffhand(AnimationState<ChibiEntity> state) {
        return PlayState.STOP;
    }

    private static PlayState predicateSwing(AnimationState<ChibiEntity> state) {
        return PlayState.STOP;
    }

    private static PlayState predicateUse(AnimationState<ChibiEntity> state) {
        return PlayState.STOP;
    }
}

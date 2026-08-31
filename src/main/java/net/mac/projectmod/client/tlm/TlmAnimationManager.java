package net.mac.projectmod.client.tlm;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.PlayState;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.builder.AnimationBuilder;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.builder.ILoopType;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.event.predicate.AnimationEvent;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Pose;

import java.util.function.BiPredicate;

public final class TlmAnimationManager {
    public static final int HIGHEST = 0;
    public static final int HIGH = 1;
    public static final int NORMAL = 2;
    public static final int LOW = 3;
    public static final int LOWEST = 4;
    private static final double MIN_SPEED = 0.05D;

    private final ReferenceArrayList<TlmAnimationState>[] data;

    @SuppressWarnings("unchecked")
    public TlmAnimationManager() {
        this.data = new ReferenceArrayList[LOWEST + 1];
        for (int i = 0; i < data.length; i++) data[i] = new ReferenceArrayList<>();
        registerAnimationState();
    }

    private void registerAnimationState() {
        register("death", ILoopType.EDefaultLoopTypes.PLAY_ONCE, HIGHEST, (chibi, e) -> chibi.getEntity().isDeadOrDying());
        register("sleep", HIGHEST, (chibi, e) -> chibi.getEntity().getPose() == Pose.SLEEPING);
        register("swim", HIGHEST, (chibi, e) -> chibi.getEntity().isSwimming());

        register("boat", HIGH, (chibi, e) -> chibi.getEntity().getVehicle() instanceof Boat);
        register("sit", HIGH, (chibi, e) -> chibi.getEntity().isSittingByServer());

        register("swim_stand", NORMAL, (chibi, e) -> chibi.getEntity().isInWater());
        register("attacked", ILoopType.EDefaultLoopTypes.PLAY_ONCE, NORMAL,
                (chibi, e) -> chibi.getEntity().hurtTime > 0);
        register("jump", NORMAL,
                (chibi, e) -> !chibi.getEntity().onGround() && !chibi.getEntity().isInWater());

        register("run", LOW,
                (chibi, e) -> chibi.getEntity().onGround() && chibi.getEntity().isSprinting());
        register("walk", LOW,
                (chibi, e) -> chibi.getEntity().onGround() && e.getLimbSwingAmount() > MIN_SPEED);

        register("idle", LOWEST, (chibi, e) -> true);
    }

    private void register(String animationName, int priority, BiPredicate<GeckoChibiEntity, AnimationEvent<?>> predicate) {
        register(animationName, ILoopType.EDefaultLoopTypes.LOOP, priority, predicate);
    }

    private void register(String animationName, ILoopType loopType, int priority,
                          BiPredicate<GeckoChibiEntity, AnimationEvent<?>> predicate) {
        data[priority].add(new TlmAnimationState(animationName, loopType, priority, predicate));
    }

    public PlayState predicateMain(AnimationEvent<GeckoChibiEntity> event) {
        GeckoChibiEntity chibi = event.getAnimatableEntity();
        for (int i = HIGHEST; i <= LOWEST; i++) {
            for (TlmAnimationState state : data[i]) {
                if (state.predicate().test(chibi, event)) {
                    event.getController().setAnimation(
                            new AnimationBuilder().addAnimation(state.animationName(), state.loopType())
                    );
                    return PlayState.CONTINUE;
                }
            }
        }
        return PlayState.STOP;
    }

    public PlayState predicateSwing(AnimationEvent<GeckoChibiEntity> event) {
        if (event.getAnimatableEntity().getEntity().swinging
                && !event.getAnimatableEntity().getEntity().isSleeping()) {
            event.getController().setAnimation(
                    new AnimationBuilder().addAnimation("swing_hand", ILoopType.EDefaultLoopTypes.PLAY_ONCE)
            );
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    public PlayState predicateUse(AnimationEvent<GeckoChibiEntity> event) {
        var entity = event.getAnimatableEntity().getEntity();
        if (entity.isUsingItem() && !entity.isSleeping()) {
            String animation = entity.getUsedItemHand() == InteractionHand.MAIN_HAND
                    ? "use_mainhand" : "use_offhand";
            event.getController().setAnimation(
                    new AnimationBuilder().addAnimation(animation, ILoopType.EDefaultLoopTypes.LOOP)
            );
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }
}

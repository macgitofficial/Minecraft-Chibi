package net.mac.projectmod.client.tlm;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.builder.ILoopType;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.event.predicate.AnimationEvent;

import java.util.function.BiPredicate;

public final class TlmAnimationState {
    private final String animationName;
    private final ILoopType loopType;
    private final int priority;
    private final BiPredicate<GeckoChibiEntity, AnimationEvent<?>> predicate;

    public TlmAnimationState(String animationName, ILoopType loopType, int priority,
                             BiPredicate<GeckoChibiEntity, AnimationEvent<?>> predicate) {
        this.animationName = animationName;
        this.loopType = loopType;
        this.priority = priority;
        this.predicate = predicate;
    }

    public String animationName() { return animationName; }
    public ILoopType loopType() { return loopType; }
    public int priority() { return priority; }
    public BiPredicate<GeckoChibiEntity, AnimationEvent<?>> predicate() { return predicate; }
}

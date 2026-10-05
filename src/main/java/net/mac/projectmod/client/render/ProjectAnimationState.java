/**
 * ProjectMod adaptation of rendering/animation code originally derived from
 * Touhou Little Maid. See LICENSE-TLM-MIT.txt and THIRD-PARTY-NOTICES.md for
 * upstream attribution and license. ProjectMod changes are maintained by the
 * ProjectMod project.
 */
package net.mac.projectmod.client.render;

import net.mac.projectmod.client.render.legacy.geckolib3.core.builder.ILoopType;
import net.mac.projectmod.client.render.legacy.geckolib3.core.event.predicate.AnimationEvent;

import java.util.function.BiPredicate;

public final class ProjectAnimationState {
    private final String animationName;
    private final ILoopType loopType;
    private final int priority;
    private final BiPredicate<ProjectGeckoChibiEntity, AnimationEvent<?>> predicate;

    public ProjectAnimationState(String animationName, ILoopType loopType, int priority,
                             BiPredicate<ProjectGeckoChibiEntity, AnimationEvent<?>> predicate) {
        this.animationName = animationName;
        this.loopType = loopType;
        this.priority = priority;
        this.predicate = predicate;
    }

    public String animationName() { return animationName; }
    public ILoopType loopType() { return loopType; }
    public int priority() { return priority; }
    public BiPredicate<ProjectGeckoChibiEntity, AnimationEvent<?>> predicate() { return predicate; }
}

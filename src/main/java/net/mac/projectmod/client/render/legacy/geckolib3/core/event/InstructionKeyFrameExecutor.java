package net.mac.projectmod.client.render.legacy.geckolib3.core.event;

import net.mac.projectmod.client.render.legacy.geckolib3.core.keyframe.event.EventKeyFrame;
import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.value.IValue;
import net.mac.projectmod.client.render.legacy.molang.runtime.ExpressionEvaluator;

import java.util.List;

public class InstructionKeyFrameExecutor {
    private final List<EventKeyFrame<IValue[]>> list;
    private int nextIndex = 0;

    public InstructionKeyFrameExecutor(List<EventKeyFrame<IValue[]>> list) {
        this.list = list;
    }

    private void evalValues(ExpressionEvaluator<?> evaluator, IValue[] values) {
        for (IValue value : values) {
            value.evalAsDouble(evaluator);
        }
    }

    public void executeTo(ExpressionEvaluator<?> evaluator, double currentTick) {
        while (!reachEnd()) {
            EventKeyFrame<IValue[]> keyFrame = list.get(nextIndex);
            if (keyFrame.getStartTick() > currentTick) {
                return;
            }
            evalValues(evaluator, keyFrame.getEventData());
            nextIndex++;
        }
    }

    public void executeRemaining(ExpressionEvaluator<?> evaluator) {
        for (int i = nextIndex; i < list.size(); i++) {
            evalValues(evaluator, list.get(i).getEventData());
        }
        nextIndex = list.size();
    }

    public boolean reachEnd() {
        return nextIndex >= list.size();
    }

    public void reset() {
        nextIndex = 0;
    }
}

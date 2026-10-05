package net.mac.projectmod.client.render.legacy.geckolib3.core.molang.variable.block;

import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.context.IContext;
import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.variable.IValueEvaluator;
import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.variable.LambdaVariable;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class AbstractBlockVariable extends LambdaVariable<BlockBehaviour> {
    public AbstractBlockVariable(IValueEvaluator<?, IContext<BlockBehaviour>> evaluator) {
        super(evaluator);
    }

    @Override
    protected boolean validateContext(IContext<?> context) {
        return context.entity() instanceof BlockBehaviour;
    }
}

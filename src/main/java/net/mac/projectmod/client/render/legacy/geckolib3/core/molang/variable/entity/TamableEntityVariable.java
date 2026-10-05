package net.mac.projectmod.client.render.legacy.geckolib3.core.molang.variable.entity;

import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.context.IContext;
import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.variable.IValueEvaluator;
import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.variable.LambdaVariable;
import net.minecraft.world.entity.TamableAnimal;

public class TamableEntityVariable extends LambdaVariable<TamableAnimal> {
    public TamableEntityVariable(IValueEvaluator<?, IContext<TamableAnimal>> evaluator) {
        super(evaluator);
    }

    @Override
    protected boolean validateContext(IContext<?> context) {
        return context.entity() instanceof TamableAnimal;
    }
}

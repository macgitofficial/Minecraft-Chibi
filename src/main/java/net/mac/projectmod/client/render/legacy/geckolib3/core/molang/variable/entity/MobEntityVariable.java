package net.mac.projectmod.client.render.legacy.geckolib3.core.molang.variable.entity;

import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.context.IContext;
import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.variable.IValueEvaluator;
import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.variable.LambdaVariable;
import net.minecraft.world.entity.Mob;

public class MobEntityVariable extends LambdaVariable<Mob> {
    public MobEntityVariable(IValueEvaluator<?, IContext<Mob>> evaluator) {
        super(evaluator);
    }

    @Override
    protected boolean validateContext(IContext<?> context) {
        return context.entity() instanceof Mob;
    }
}

package com.github.tartaricacid.touhoulittlemaid.geckolib3.core.molang.variable.entity;

import net.minecraft.world.entity.LivingEntity;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.molang.context.IContext;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.molang.variable.IValueEvaluator;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.molang.variable.LambdaVariable;

public class MaidEntityVariable extends LambdaVariable<LivingEntity> {
    public MaidEntityVariable(IValueEvaluator<?, IContext<LivingEntity>> evaluator) {
        super(evaluator);
    }

    @Override
    protected boolean validateContext(IContext<?> context) {
        return context.entity() instanceof LivingEntity;
    }
}

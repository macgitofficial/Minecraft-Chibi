package net.mac.projectmod.client.render.legacy.geckolib3.core.molang.variable.block;

import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.context.IContext;
import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.variable.IValueEvaluator;
import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.variable.LambdaVariable;
import net.minecraft.world.level.block.Block;

public class BlockVariable extends LambdaVariable<Block> {
    public BlockVariable(IValueEvaluator<?, IContext<Block>> evaluator) {
        super(evaluator);
    }

    @Override
    protected boolean validateContext(IContext<?> context) {
        return context.entity() instanceof Block;
    }
}

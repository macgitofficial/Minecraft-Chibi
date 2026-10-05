package net.mac.projectmod.client.render.legacy.geckolib3.core.molang.variable.item;

import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.context.IContext;
import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.variable.IValueEvaluator;
import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.variable.LambdaVariable;
import net.minecraft.world.item.ItemStack;

public class ItemStackVariable extends LambdaVariable<ItemStack> {
    public ItemStackVariable(IValueEvaluator<?, IContext<ItemStack>> evaluator) {
        super(evaluator);
    }

    @Override
    protected boolean validateContext(IContext<?> context) {
        return context.entity() instanceof ItemStack;
    }
}

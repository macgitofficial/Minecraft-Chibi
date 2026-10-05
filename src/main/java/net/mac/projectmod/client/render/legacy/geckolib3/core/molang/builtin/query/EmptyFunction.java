package net.mac.projectmod.client.render.legacy.geckolib3.core.molang.builtin.query;

import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.context.IContext;
import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.function.ContextFunction;
import net.mac.projectmod.client.render.legacy.molang.runtime.ExecutionContext;

public class EmptyFunction extends ContextFunction<Object> {
    @Override
    protected Object eval(ExecutionContext<IContext<Object>> context, ArgumentCollection arguments) {
        return null;
    }
}

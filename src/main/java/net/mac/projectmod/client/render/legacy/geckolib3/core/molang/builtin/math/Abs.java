package net.mac.projectmod.client.render.legacy.geckolib3.core.molang.builtin.math;

import net.mac.projectmod.client.render.legacy.molang.runtime.ExecutionContext;
import net.mac.projectmod.client.render.legacy.molang.runtime.Function;

public class Abs implements Function {
    @Override
    public Object evaluate(ExecutionContext<?> context, ArgumentCollection arguments) {
        return Math.abs(arguments.getAsDouble(context, 0));
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1;
    }
}

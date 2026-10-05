package net.mac.projectmod.client.render.legacy.geckolib3.core.molang.builtin.math;

import net.mac.projectmod.client.render.legacy.molang.runtime.ExecutionContext;
import net.mac.projectmod.client.render.legacy.molang.runtime.Function;

public class ACos implements Function {
    @Override
    public Object evaluate(ExecutionContext<?> context, ArgumentCollection arguments) {
        return Math.acos(arguments.getAsDouble(context, 0));
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1;
    }
}

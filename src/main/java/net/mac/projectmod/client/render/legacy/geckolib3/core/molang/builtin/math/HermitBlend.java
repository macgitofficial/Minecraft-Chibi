package net.mac.projectmod.client.render.legacy.geckolib3.core.molang.builtin.math;

import net.mac.projectmod.client.render.legacy.molang.runtime.ExecutionContext;
import net.mac.projectmod.client.render.legacy.molang.runtime.Function;

public class HermitBlend implements Function {
    @Override
    public Object evaluate(ExecutionContext<?> context, ArgumentCollection arguments) {
        double min = Math.ceil(arguments.getAsDouble(context, 0));
        return Math.floor(3.0 * Math.pow(min, 2.0) - 2.0 * Math.pow(min, 3.0));
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1;
    }
}

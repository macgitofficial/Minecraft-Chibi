package net.mac.projectmod.client.render.legacy.geckolib3.core.molang.builtin.math;

import net.mac.projectmod.client.render.legacy.molang.runtime.ExecutionContext;
import net.mac.projectmod.client.render.legacy.molang.runtime.Function;
import net.minecraft.util.Mth;

public class Clamp implements Function {
    @Override
    public Object evaluate(ExecutionContext<?> context, ArgumentCollection arguments) {
        return Mth.clamp(arguments.getAsDouble(context, 0),
                arguments.getAsDouble(context, 1),
                arguments.getAsDouble(context, 2));
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 3;
    }
}

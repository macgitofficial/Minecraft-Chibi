package net.mac.projectmod.client.render.legacy.geckolib3.core.molang.builtin.query;

import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.context.IContext;
import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.function.entity.EntityFunction;
import net.mac.projectmod.client.render.legacy.molang.runtime.ExecutionContext;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class Position extends EntityFunction {
    @Override
    protected Object eval(ExecutionContext<IContext<Entity>> context, ArgumentCollection arguments) {
        int axis = arguments.getAsInt(context, 0);
        float partialTicks = context.entity().animationEvent().getPartialTick();
        Entity entity = context.entity().entity();
        switch (axis) {
            case 0: return Mth.lerp(partialTicks, entity.xo, entity.getX());
            case 1: return Mth.lerp(partialTicks, entity.yo, entity.getY());
            case 2: return Mth.lerp(partialTicks, entity.zo, entity.getZ());
            default: return null;
        }
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1;
    }
}

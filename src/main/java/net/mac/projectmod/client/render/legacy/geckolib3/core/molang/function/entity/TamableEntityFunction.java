package net.mac.projectmod.client.render.legacy.geckolib3.core.molang.function.entity;

import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.context.IContext;
import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.function.ContextFunction;
import net.minecraft.world.entity.TamableAnimal;

public abstract class TamableEntityFunction extends ContextFunction<TamableAnimal> {
    @Override
    protected boolean validateContext(IContext<?> context) {
        return context.entity() instanceof TamableAnimal;
    }
}

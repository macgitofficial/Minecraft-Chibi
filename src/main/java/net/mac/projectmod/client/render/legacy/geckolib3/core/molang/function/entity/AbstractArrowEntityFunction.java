package net.mac.projectmod.client.render.legacy.geckolib3.core.molang.function.entity;

import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.context.IContext;
import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.function.ContextFunction;
import net.minecraft.world.entity.projectile.AbstractArrow;

public abstract class AbstractArrowEntityFunction extends ContextFunction<AbstractArrow> {
    @Override
    protected boolean validateContext(IContext<?> context) {
        return context.entity() instanceof AbstractArrow;
    }
}

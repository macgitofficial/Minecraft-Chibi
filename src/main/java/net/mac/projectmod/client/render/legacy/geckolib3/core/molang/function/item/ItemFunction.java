package net.mac.projectmod.client.render.legacy.geckolib3.core.molang.function.item;

import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.context.IContext;
import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.function.ContextFunction;
import net.minecraft.world.item.Item;

public abstract class ItemFunction extends ContextFunction<Item> {
    @Override
    protected boolean validateContext(IContext<?> context) {
        return context.entity() instanceof Item;
    }
}

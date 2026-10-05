package net.mac.projectmod.client.render.legacy.geckolib3.core.molang.variable;

import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.context.IContext;
import net.mac.projectmod.client.render.legacy.molang.runtime.ExecutionContext;
import net.mac.projectmod.client.render.legacy.molang.runtime.Variable;

public abstract class ContextVariable<TEntity> implements Variable {
    protected boolean validateContext(IContext<?> context) {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final Object evaluate(ExecutionContext<?> context) {
        Object entity = context.entity();
        if (entity instanceof IContext && validateContext((IContext<?>) entity)) {
            return evaluate((IContext<TEntity>) entity);
        } else {
            return null;
        }
    }

    public abstract Object evaluate(IContext<TEntity> context);
}

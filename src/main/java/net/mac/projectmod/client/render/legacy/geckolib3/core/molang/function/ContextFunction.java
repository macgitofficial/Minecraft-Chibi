package net.mac.projectmod.client.render.legacy.geckolib3.core.molang.function;

import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.context.IContext;
import net.mac.projectmod.client.render.legacy.molang.runtime.ExecutionContext;
import net.mac.projectmod.client.render.legacy.molang.runtime.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ContextFunction<TEntity> implements Function {
    protected boolean validateContext(IContext<?> context) {
        return true;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public final Object evaluate(@NotNull ExecutionContext<?> context, @NotNull ArgumentCollection arguments) {
        Object entity = context.entity();
        if (entity instanceof IContext && validateContext((IContext<?>) entity)) {
            return eval((ExecutionContext<IContext<TEntity>>) context, arguments);
        } else {
            return null;
        }
    }

    protected abstract Object eval(ExecutionContext<IContext<TEntity>> context, ArgumentCollection arguments);
}

package net.mac.projectmod.client.render.legacy.geckolib3.core.molang.binding.variable;

import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.context.IContext;
import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.storage.IForeignVariableStorage;
import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.util.StringPool;
import net.mac.projectmod.client.render.legacy.molang.runtime.ExecutionContext;
import net.mac.projectmod.client.render.legacy.molang.runtime.Variable;
import net.mac.projectmod.client.render.legacy.molang.runtime.binding.ObjectBinding;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("MapOrSetKeyShouldOverrideHashCodeEquals")
public class ForeignVariableBinding implements ObjectBinding {
    private final Int2ReferenceOpenHashMap<ForeignVariable> variableMap = new Int2ReferenceOpenHashMap<>();

    @Override
    public Object getProperty(String name) {
        return variableMap.computeIfAbsent(StringPool.computeIfAbsent(name), ForeignVariable::new);
    }

    public void reset() {
        variableMap.clear();
    }

    private record ForeignVariable(int name) implements Variable {
        @Override
        @SuppressWarnings("unchecked")
        public Object evaluate(final @NotNull ExecutionContext<?> context) {
            IForeignVariableStorage storage = ((IContext<Object>) context.entity()).foreignStorage();
            if (storage != null) {
                return storage.getPublic(name);
            } else {
                return null;
            }
        }
    }
}

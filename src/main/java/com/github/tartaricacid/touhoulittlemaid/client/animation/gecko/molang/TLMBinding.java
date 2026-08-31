package com.github.tartaricacid.touhoulittlemaid.client.animation.gecko.molang;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.molang.binding.ContextBinding;
import net.minecraft.world.entity.LivingEntity;

public final class TLMBinding extends ContextBinding {
    public static final TLMBinding INSTANCE = new TLMBinding();
    private TLMBinding() {
        livingEntityVar("is_sitting", ctx -> ctx.entity().isPassenger());
    }
}

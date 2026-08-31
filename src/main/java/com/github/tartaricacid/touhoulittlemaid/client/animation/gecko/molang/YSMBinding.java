package com.github.tartaricacid.touhoulittlemaid.client.animation.gecko.molang;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.molang.binding.ContextBinding;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.molang.builtin.query.EmptyFunction;

public final class YSMBinding extends ContextBinding {
    public static final YSMBinding INSTANCE = new YSMBinding();
    private YSMBinding() {
        function("dump_equipped_item", new EmptyFunction());
        function("dump_relative_block", new EmptyFunction());
        function("bone_pivot_abs", new EmptyFunction());
        var("dump_mods", ctx -> 0);
        var("texture_name", ctx -> "");
        var("first_person_mod_hide", ctx -> false);
    }
}

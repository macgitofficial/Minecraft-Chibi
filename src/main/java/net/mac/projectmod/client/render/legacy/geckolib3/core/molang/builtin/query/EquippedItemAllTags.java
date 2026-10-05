package net.mac.projectmod.client.render.legacy.geckolib3.core.molang.builtin.query;

import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.context.IContext;
import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.function.entity.LivingEntityFunction;
import net.mac.projectmod.client.render.legacy.geckolib3.util.MolangUtils;
import net.mac.projectmod.client.render.legacy.molang.runtime.ExecutionContext;
import net.mac.projectmod.client.render.legacy.EquipmentUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class EquippedItemAllTags extends LivingEntityFunction {
    @Override
    protected Object eval(ExecutionContext<IContext<LivingEntity>> context, ArgumentCollection arguments) {
        EquipmentSlot slotType = MolangUtils.parseSlotType(context.entity(), arguments.getAsString(context, 0));
        if (slotType == null) {
            return null;
        }

        ItemStack itemStack = EquipmentUtil.getEquippedItem(context.entity().entity(), slotType);
        if(itemStack.isEmpty()) {
            return false;
        }

        for (int i = 1; i < arguments.size(); i++) {
            ResourceLocation id = MolangUtils.parseResourceLocation(context.entity(), arguments.getAsString(context, i));
            if (id == null) {
                return null;
            }
            TagKey<Item> tag = TagKey.create(Registries.ITEM, id);
            if (!itemStack.is(tag)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size >= 2;
    }
}

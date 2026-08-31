package com.github.tartaricacid.touhoulittlemaid.util;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class EquipmentUtil {
    private EquipmentUtil() {}

    public static ItemStack getEquippedItem(LivingEntity entity, EquipmentSlot slot) {
        return entity == null ? ItemStack.EMPTY : entity.getItemBySlot(slot);
    }

    public static ItemStack getEquippedElytraItem(LivingEntity entity) {
        ItemStack stack = getEquippedItem(entity, EquipmentSlot.CHEST);
        return stack.is(Items.ELYTRA) ? stack : ItemStack.EMPTY;
    }
}

package net.mac.projectmod.gui;

import net.mac.projectmod.entity.ChibiEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ChibiInventory implements Container {

    private final ChibiEntity chibi;
    private final NonNullList<ItemStack> items;

    public ChibiInventory(ChibiEntity chibi) {
        this.chibi = chibi;
        this.items = NonNullList.withSize(18, ItemStack.EMPTY);
    }

    public static final int STORAGE_START = 0;
    public static final int STORAGE_SIZE = 12;

    public static final int HELMET_SLOT = 12;
    public static final int CHEST_SLOT = 13;
    public static final int LEGS_SLOT = 14;
    public static final int BOOTS_SLOT = 15;

    public static final int MAIN_HAND_SLOT = 16;
    public static final int OFF_HAND_SLOT = 17;

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < STORAGE_SIZE; i++) {
            if (!getItem(i).isEmpty()) {
                return false;
            }
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!getEquipmentItem(slot).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(int slot) {

        switch (slot) {
            case HELMET_SLOT:
                return getEquipmentItem(EquipmentSlot.HEAD);

            case CHEST_SLOT:
                return getEquipmentItem(EquipmentSlot.CHEST);

            case LEGS_SLOT:
                return getEquipmentItem(EquipmentSlot.LEGS);

            case BOOTS_SLOT:
                return getEquipmentItem(EquipmentSlot.FEET);

            case MAIN_HAND_SLOT:
                return getEquipmentItem(EquipmentSlot.MAINHAND);

            case OFF_HAND_SLOT:
                return getEquipmentItem(EquipmentSlot.OFFHAND);

            default:
                return items.get(slot);
        }
    }

    private ItemStack getEquipmentItem(EquipmentSlot slot) {
        return chibi.getChibiEquipment(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {

        if (isEquipmentSlot(slot)) {

            ItemStack current = getItem(slot);

            if (current.isEmpty()) {
                return ItemStack.EMPTY;
            }

            ItemStack result = current.split(amount);

            if (current.isEmpty()) {
                setEquipmentItem(slot, ItemStack.EMPTY);
            }

            setChanged();
            return result;
        }

        ItemStack result = ContainerHelper.removeItem(items, slot, amount);

        if (!result.isEmpty()) {
            setChanged();
        }

        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {

        if (isEquipmentSlot(slot)) {
            ItemStack result = getItem(slot);
            setEquipmentItem(slot, ItemStack.EMPTY);
            return result;
        }

        return items.set(slot, ItemStack.EMPTY);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {

        if (isEquipmentSlot(slot)) {
            setEquipmentItem(slot, stack);
            return;
        }

        items.set(slot, stack);

        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }

        setChanged();
    }

    private void setEquipmentItem(int slot, ItemStack stack) {

        switch (slot) {
            case HELMET_SLOT:
                chibi.setChibiEquipment(EquipmentSlot.HEAD, stack);
                break;

            case CHEST_SLOT:
                chibi.setChibiEquipment(EquipmentSlot.CHEST, stack);
                break;

            case LEGS_SLOT:
                chibi.setChibiEquipment(EquipmentSlot.LEGS, stack);
                break;

            case BOOTS_SLOT:
                chibi.setChibiEquipment(EquipmentSlot.FEET, stack);
                break;

            case MAIN_HAND_SLOT:
                chibi.setChibiEquipment(EquipmentSlot.MAINHAND, stack);
                break;

            case OFF_HAND_SLOT:
                chibi.setChibiEquipment(EquipmentSlot.OFFHAND, stack);
                break;
        }

        setChanged();
    }

    private boolean isEquipmentSlot(int slot) {
        return slot >= HELMET_SLOT && slot <= OFF_HAND_SLOT;
    }

    @Override
    public void setChanged() {
        // ChibiEntity เป็นคนถือ equipment จริง
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {

        for (int i = STORAGE_START; i < STORAGE_START + STORAGE_SIZE; i++) {
            items.set(i, ItemStack.EMPTY);
        }

        chibi.setChibiEquipment(EquipmentSlot.HEAD, ItemStack.EMPTY);
        chibi.setChibiEquipment(EquipmentSlot.CHEST, ItemStack.EMPTY);
        chibi.setChibiEquipment(EquipmentSlot.LEGS, ItemStack.EMPTY);
        chibi.setChibiEquipment(EquipmentSlot.FEET, ItemStack.EMPTY);
        chibi.setChibiEquipment(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        chibi.setChibiEquipment(EquipmentSlot.OFFHAND, ItemStack.EMPTY);

        setChanged();
    }
}

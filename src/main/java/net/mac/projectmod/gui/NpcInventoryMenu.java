package net.mac.projectmod.gui;

import net.mac.projectmod.entity.ChibiEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class NpcInventoryMenu extends AbstractContainerMenu {

    private final ChibiEntity chibi;

    public NpcInventoryMenu(int containerId, Inventory playerInventory, ChibiEntity chibi) {
        super(ModMenus.NPC_INVENTORY.get(), containerId);
        this.chibi = chibi;

        addSlots(playerInventory);
    }

    public NpcInventoryMenu(int containerId, Inventory playerInventory, FriendlyByteBuf data) {
        super(ModMenus.NPC_INVENTORY.get(), containerId);
        int entityId = data.readInt();
        Entity entity = playerInventory.player.level().getEntity(entityId);
        this.chibi = entity instanceof ChibiEntity chibi ? chibi : null;

        addSlots(playerInventory);
    }

    private void addSlots(Inventory playerInventory) {
        // Chibi MainHand (slot index 0)
        this.addSlot(new Slot(new SimpleContainer(1), 0, 80, 8) {

            @Override
            public ItemStack getItem() {
                return chibi.getItemBySlot(EquipmentSlot.MAINHAND);
            }

            @Override
            public void set(ItemStack stack) {
                chibi.setItemSlot(EquipmentSlot.MAINHAND, stack);
            }

            @Override
            public ItemStack remove(int amount) {
                ItemStack current = chibi.getItemBySlot(EquipmentSlot.MAINHAND);
                if (current.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                ItemStack split = current.split(amount);
                chibi.setItemSlot(EquipmentSlot.MAINHAND, current);
                return split;
            }


            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.isEmpty()
                        || stack.getItem() instanceof AxeItem;
            }

            @Override
            public boolean mayPickup(Player player) {
                return true;
            }
        });

        // Chibi Inventory (slot indices 1-9)
        for (int i = 0; i < 9; i++) {
            final int slotIndex = i;
            this.addSlot(new SlotItemHandler(
                    chibi.getInventory(),
                    slotIndex,
                    8 + slotIndex * 18,
                    28
            ));
        }

        // Player Inventory (slot indices 10-36)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(
                        playerInventory,
                        col + row * 9 + 9,
                        8 + col * 18,
                        78 + row * 18
                ));
            }
        }

        // Hotbar (slot indices 37-45)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(
                    playerInventory,
                    col,
                    8 + col * 18,
                    136
            ));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();

            // Chibi (mainhand + inventory, indices 0-9) -> Player (indices 10-45)
            if (index < 10) {
                if (!this.moveItemStackTo(stack, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // Player -> Chibi inventory only (indices 1-9), skip mainhand slot 0
            else {
                if (!this.moveItemStackTo(stack, 1, 9, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return chibi != null
                && chibi.isAlive()
                && player.distanceToSqr(chibi) <= 16.0D;
    }
}
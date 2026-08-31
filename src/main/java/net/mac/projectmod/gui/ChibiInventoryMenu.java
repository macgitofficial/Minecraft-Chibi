package net.mac.projectmod.gui;

import net.mac.projectmod.entity.ChibiEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ChibiInventoryMenu extends AbstractContainerMenu {

    private final ChibiEntity chibi;
    private final ChibiInventory chibiInventory;

    // =========================
    // Slot index
    // =========================

    public static final int CHIBI_STORAGE_START = 0;
    public static final int CHIBI_STORAGE_END = 12;

    public static final int ARMOR_START = 27;
    public static final int ARMOR_END = 31;

    public static final int MAIN_HAND = 31;
    public static final int OFF_HAND = 32;

    public static final int PLAYER_INV_START = 18;
    public static final int PLAYER_INV_END = 53;

    private Slot createArmorSlot(
            int inventorySlot,
            EquipmentSlot equipmentSlot,
            int x,
            int y
    ) {
        return new Slot(
                chibiInventory,
                inventorySlot,
                x,
                y
        ) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.canEquip(
                        equipmentSlot,
                        chibi
                );
            }
        };
    }


    // =========================
    // Client
    // =========================

    public ChibiInventoryMenu(
            int containerId,
            Inventory playerInventory,
            FriendlyByteBuf data
    ) {
        this(
                containerId,
                playerInventory,
                getChibi(playerInventory, data.readInt())
        );
    }

    private static ChibiEntity getChibi(
            Inventory playerInventory,
            int entityId
    ) {
        return (ChibiEntity) playerInventory.player
                .level()
                .getEntity(entityId);
    }

    // =========================
    // Server
    // =========================

    public ChibiInventoryMenu(
            int containerId,
            Inventory playerInventory,
            ChibiEntity chibi
    ) {
        super(ModMenus.CHIBI_INVENTORY.get(), containerId);

        this.chibi = chibi;
        this.chibiInventory = chibi.getChibiInventory();

        // ==================================================
        // Chibi Inventory
        // ==================================================

        for (int row = 0; row < 3; row++) {

            for (int col = 0; col < 4; col++) {

                this.addSlot(
                        new Slot(
                                chibiInventory,
                                col + row * 4,
                                40 + col * 18,
                                130 + row * 18
                        )
                );
            }
        }

        // ==================================================
        // Armor 2x2
        // ==================================================

        // Helmet
        this.addSlot(createArmorSlot(
                ChibiInventory.HELMET_SLOT,
                EquipmentSlot.HEAD,
                220,
                50
        ));

        // Chestplate
        this.addSlot(createArmorSlot(
                ChibiInventory.CHEST_SLOT,
                EquipmentSlot.CHEST,
                238,
                50
        ));

        // Leggings
        this.addSlot(createArmorSlot(
                ChibiInventory.LEGS_SLOT,
                EquipmentSlot.LEGS,
                220,
                68
        ));

        // Boots
        this.addSlot(createArmorSlot(
                ChibiInventory.BOOTS_SLOT,
                EquipmentSlot.FEET,
                238,
                68
        ));


        // ==================================================
        // Main Hand / Off Hand
        // ==================================================

        this.addSlot(new Slot(
                chibiInventory,
                ChibiInventory.MAIN_HAND_SLOT,
                220,
                92
        ));

        this.addSlot(new Slot(
                chibiInventory,
                ChibiInventory.OFF_HAND_SLOT,
                238,
                92
        ));

        // ==================================================
        // Player Inventory
        // ==================================================

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {

                this.addSlot(new Slot(
                        playerInventory,
                        col + row * 9 + 9,
                        130 + col * 18,
                        130 + row * 18
                ));
            }
        }

        // ==================================================
        // Player Hotbar
        // ==================================================

        for (int col = 0; col < 9; col++) {

            this.addSlot(
                    new Slot(
                            playerInventory,
                            col,
                            130 + col * 18,
                            190
                    )
            );
        }
    }

    // ==================================================
    // Valid
    // ==================================================

    @Override
    public boolean stillValid(Player player) {

        return chibi != null
                && chibi.isAlive()
                && chibi.distanceToSqr(player) <= 64.0D;
    }

    // ==================================================
    // Shift Click
    // ==================================================

    @Override
    public ItemStack quickMoveStack(
            Player player,
            int index
    ) {

        Slot slot = this.slots.get(index);

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        // ==============================================
        // Chibi -> Player
        // ==============================================

        if (index < PLAYER_INV_START) {

            if (!this.moveItemStackTo(
                    stack,
                    PLAYER_INV_START,
                    this.slots.size(),
                    false

            )) {
                return ItemStack.EMPTY;
            }
        }

        // ==============================================
        // Player -> Chibi
        // ==============================================

        // -------------------------
        // Armor
        // -------------------------

        if (stack.canEquip(
                EquipmentSlot.HEAD,
                chibi
        )) {

            if (!moveItemStackTo(
                    stack,
                    ChibiInventory.HELMET_SLOT,
                    ChibiInventory.HELMET_SLOT + 1,
                    false
            )) {
                return ItemStack.EMPTY;
            }

        } else if (stack.canEquip(
                EquipmentSlot.CHEST,
                chibi
        )) {

            if (!moveItemStackTo(
                    stack,
                    ChibiInventory.CHEST_SLOT,
                    ChibiInventory.CHEST_SLOT + 1,
                    false
            )) {
                return ItemStack.EMPTY;
            }

        } else if (stack.canEquip(
                EquipmentSlot.LEGS,
                chibi
        )) {

            if (!moveItemStackTo(
                    stack,
                    ChibiInventory.LEGS_SLOT,
                    ChibiInventory.LEGS_SLOT + 1,
                    false
            )) {
                return ItemStack.EMPTY;
            }

        } else if (stack.canEquip(
                EquipmentSlot.FEET,
                chibi
        )) {

            if (!moveItemStackTo(
                    stack,
                    ChibiInventory.BOOTS_SLOT,
                    ChibiInventory.BOOTS_SLOT + 1,
                    false
            )) {
                return ItemStack.EMPTY;
            }

        }


        // -------------------------
        // Normal item
        // -------------------------

        else {

            if (!this.moveItemStackTo(
                    stack,
                    CHIBI_STORAGE_START,
                    CHIBI_STORAGE_END,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return original;
    }
}
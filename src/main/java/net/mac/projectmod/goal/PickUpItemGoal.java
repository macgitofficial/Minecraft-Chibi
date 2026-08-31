package net.mac.projectmod.goal;

import net.mac.projectmod.entity.ChibiEntity;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;


import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class PickUpItemGoal extends Goal {

    private static final double SEARCH_RANGE = 6.0D;
    private static final double MOVE_SPEED = 1.2D;
    private final ChibiEntity chibi;
    private ItemEntity targetItem;

    public PickUpItemGoal(ChibiEntity chibi) {
        this.chibi = chibi;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {

        if (!chibi.isTame()) {
            return false;
        }

        if (isInventoryFull()) {
            return false;
        }

        List<ItemEntity> nearby = chibi.level().getEntitiesOfClass(
                ItemEntity.class,
                chibi.getBoundingBox().inflate(SEARCH_RANGE),
                item ->
                        item.isAlive()
                                && !item.getItem().isEmpty()
                                && !item.hasPickUpDelay()
        );

        targetItem = nearby.stream()
                .min(Comparator.comparingDouble(chibi::distanceToSqr))
                .orElse(null);

        return targetItem != null;
    }

    @Override
    public boolean canContinueToUse() {

        if (!chibi.isTame()) {
            return false;
        }

        if (chibi.getOwner() != null
                && chibi.distanceToSqr(chibi.getOwner()) >= 10.0D * 10.0D) {
            return false;
        }

        if (targetItem == null || !targetItem.isAlive()) {
            return false;
        }

        return !isInventoryFull();
    }

    @Override
    public void tick() {

        if (targetItem == null || !targetItem.isAlive()) {
            return;
        }

        if (isInventoryFull()) {
            stop();
            return;
        }

        if (chibi.getBoundingBox()
                .inflate(1.0D)
                .intersects(targetItem.getBoundingBox())) {

            pickupItem(targetItem);
            return;
        }

        double distSqr = chibi.distanceToSqr(targetItem);

        chibi.getNavigation().moveTo(targetItem, MOVE_SPEED);
        chibi.getLookControl().setLookAt(targetItem, 30.0F, 30.0F);
    }

// =========================================================
// PICKUP
// =========================================================

    /**
     * เก็บ ItemEntity เข้า ChibiInventory
     *
     * ทำงานคล้าย Player pickup:
     *
     * 1. อ่าน ItemStack
     * 2. พยายามใส่ Inventory
     * 3. คำนวณว่าหยิบไปกี่ชิ้น
     * 4. ส่ง TakeItemEntityPacket
     * 5. เล่นเสียง
     * 6. ถ้าเก็บหมด -> discard ItemEntity
     * 7. ถ้าเก็บไม่หมด -> วางส่วนที่เหลือกลับบนพื้น
     */
    private void pickupItem(ItemEntity item) {

        if (item == null || !item.isAlive()) {
            return;
        }

        ItemStack stack = item.getItem();

        if (stack.isEmpty()) {
            return;
        }

        int originalCount = stack.getCount();

        ItemStack leftover = addItem(
                chibi.getChibiInventory(),
                stack
        );

        int pickedUpCount =
                originalCount - leftover.getCount();

        if (pickedUpCount <= 0) {
            return;
        }

        take(item, pickedUpCount);

        chibi.level().playSound(
                null,
                chibi.blockPosition(),
                net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
                net.minecraft.sounds.SoundSource.NEUTRAL,
                0.2F,
                1.0F
        );

        if (leftover.isEmpty()) {

            item.discard();
            targetItem = null;

        } else {

            item.setItem(leftover);
        }
    }

    private void take(ItemEntity item, int quantity) {

        if (!chibi.isAlive()) {
            return;
        }

        if (chibi.level().isClientSide()) {
            return;
        }

        if (!(chibi.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.getChunkSource().broadcast(
                chibi,
                new ClientboundTakeItemEntityPacket(
                        item.getId(),
                        chibi.getId(),
                        quantity
                )
        );
    }

    private ItemStack addItem(
            Container inventory,
            ItemStack stack
    ) {

        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        for (int i = 0;
             i < inventory.getContainerSize();
             i++) {

            ItemStack slotStack =
                    inventory.getItem(i);

            if (!slotStack.isEmpty()
                    && ItemStack.isSameItemSameComponents(
                    slotStack,
                    stack
            )
                    && slotStack.getCount()
                    < slotStack.getMaxStackSize()) {

                int space =
                        slotStack.getMaxStackSize()
                                - slotStack.getCount();

                int moved =
                        Math.min(
                                space,
                                stack.getCount()
                        );

                slotStack.grow(moved);
                stack.shrink(moved);

                inventory.setChanged();

                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }

        for (int i = 0;
             i < inventory.getContainerSize();
             i++) {

            if (inventory.getItem(i).isEmpty()) {

                inventory.setItem(
                        i,
                        stack.copy()
                );

                stack.setCount(0);

                inventory.setChanged();

                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    private boolean isInventoryFull() {

        Container inventory =
                chibi.getChibiInventory();

        for (int i = 0;
             i < inventory.getContainerSize();
             i++) {

            ItemStack stack =
                    inventory.getItem(i);


            if (stack.isEmpty()) {
                return false;
            }

            if (stack.getCount()
                    < stack.getMaxStackSize()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void stop() {
        targetItem = null;
        chibi.getNavigation().stop();
    }

}
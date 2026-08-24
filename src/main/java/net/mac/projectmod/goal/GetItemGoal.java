package net.mac.projectmod.goal;

import net.mac.projectmod.entity.ChibiEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.List;

public class GetItemGoal extends Goal {
    private final ChibiEntity npc;
    private ItemEntity target;
    private final double speed;
    private final double searchRadius;

    public GetItemGoal(ChibiEntity npc, double speed, double searchRadius) {
        this.speed = speed;
        this.npc = npc;
        this.searchRadius = searchRadius;

        this.setFlags(EnumSet.of(
                Goal.Flag.MOVE
        ));
    }

    @Override
    public boolean canUse() {

        List<ItemEntity> items = npc.level().getEntitiesOfClass(
                ItemEntity.class,
                npc.getBoundingBox().inflate(searchRadius),
                item -> {
                    ItemStack stack = item.getItem();

                    return item.isAlive()
                            && !stack.isEmpty()
                            && canPickup(stack);
                }
        );

        if (items.isEmpty()) {
            return false;
        }

        // หา Item ที่ใกล้ที่สุด
        target = items.get(0);

        double closestDistance =
                npc.distanceToSqr(target);

        for (ItemEntity item : items) {

            double distance =
                    npc.distanceToSqr(item);

            if (distance < closestDistance) {
                target = item;
                closestDistance = distance;
            }
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {

        return target != null
                && target.isAlive()
                && !target.getItem().isEmpty()
                && npc.distanceToSqr(target)
                <= searchRadius * searchRadius;
    }

    @Override
    public void start() {

        if (target != null) {
            npc.getNavigation().moveTo(
                    target,
                    speed
            );
        }
    }

    @Override
    public void tick() {

        if (target == null || !target.isAlive()) {
            return;
        }

        npc.getNavigation().moveTo(
                target,
                speed
        );

        // ระยะที่สามารถเก็บ Item ได้
        if (npc.distanceToSqr(target) <= 2.25D) {
            pullItemToChibi();
            pickupItem();
        }
    }
    private void pullItemToChibi() {

        if (target == null || !target.isAlive()) {
            return;
        }

        // ป้องกัน Player/Entity อื่น pickup ระหว่างกำลังดูด
        target.setPickUpDelay(10);

        double dx = npc.getX() - target.getX();
        double dy = (npc.getY() + 0.8D) - target.getY();
        double dz = npc.getZ() - target.getZ();

        double distance = Math.sqrt(
                dx * dx +
                        dy * dy +
                        dz * dz
        );

        if (distance <= 0.01D) {
            return;
        }

        double speed = 0.25D;

        double vx = dx / distance * speed;
        double vy = dy / distance * speed;
        double vz = dz / distance * speed;

        target.setDeltaMovement(
                vx,
                vy,
                vz
        );

        target.hasImpulse = true;
    }
    private boolean canPickup(ItemStack stack) {

        for (int slot = 0;
             slot < npc.getInventory().getSlots();
             slot++) {

            ItemStack remaining =
                    npc.getInventory().insertItem(
                            slot,
                            stack.copy(),
                            true
                    );

            // มีบางส่วนถูกใส่ได้
            if (remaining.getCount() < stack.getCount()) {
                return true;
            }
        }

        return false;
    }

    private void pickupItem() {

        if (target == null || !target.isAlive()) {
            return;
        }

        ItemStack remaining =
                target.getItem().copy();

        for (int slot = 0;
             slot < npc.getInventory().getSlots();
             slot++) {

            remaining =
                    npc.getInventory().insertItem(
                            slot,
                            remaining,
                            false
                    );

            if (remaining.isEmpty()) {
                target.discard();
                return;
            }
        }

        // Inventory เต็มบางส่วน
        target.setItem(remaining);
    }

    @Override
    public void stop() {

        target = null;

        npc.getNavigation().stop();
    }
}

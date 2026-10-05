/*
 * Fishing goal adapted from Touhou Little Maid 1.21.1.
 *
 * TLM 1.21.1 implements fishing through TaskFishing + MaidRideFindWaterTask
 * (brain/behavior AI). This project intentionally ports that fishing logic to
 * GoalSelector because Chibi uses classic Mob Goal AI.
 *
 * MIT-derived adaptation. See THIRD_PARTY_LICENSES/TouhouLittleMaid-LICENSE-MIT.txt
 */
package net.mac.projectmod.goal;

import net.mac.projectmod.entity.ChibiEntity;
import net.mac.projectmod.fishing.ProjectFishingHook;
import net.mac.projectmod.gui.ChibiInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ToolActions;

import java.util.EnumSet;

/**
 * TLM-style autonomous fishing goal for Chibi.
 *
 * Port notes:
 * - TLM 1.21.1 does not have a class literally named FishingGoal.
 * - Its fishing task is implemented by TaskFishing + MaidRideFindWaterTask.
 * - The destination search and fishing conditions below follow that logic.
 * - This version uses GoalSelector, walks Chibi to a shore position, then
 *   launches ProjectFishingHook (the project's vanilla-fishing implementation).
 * - Only vanilla Minecraft fishing rods are equipped by this goal.
 */
public class FishingGoal extends Goal {
    private static final int SEARCH_RANGE = 6;
    private static final int VERTICAL_SEARCH_RANGE = 3;
    private static final int CHECK_DELAY = 20;
    private static final double MOVE_SPEED = 0.6D;
    private static final double CAST_DISTANCE = 3.5D;
    private static final double HOOK_TIMEOUT_SQR = 256.0D;

    private final ChibiEntity chibi;

    private BlockPos waterPos;
    private BlockPos standPos;
    private ProjectFishingHook fishingHook;
    private int nextCheckTick;
    private int failedMoveTicks;

    public FishingGoal(ChibiEntity chibi) {
        this.chibi = chibi;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (chibi.level().isClientSide()) {
            return false;
        }
        if (!chibi.isAlive() || !chibi.isTame() || chibi.isOrderedToSit()) {
            return false;
        }
        if (chibi.getTarget() != null && chibi.getTarget().isAlive()) {
            return false;
        }
        if (fishingHook != null && fishingHook.isAlive()) {
            return false;
        }
        if (!hasOrCanEquipFishingRod()) {
            return false;
        }

        if (nextCheckTick > 0) {
            nextCheckTick--;
            return false;
        }

        if (!isWaterPosValid()) {
            searchForDestination();
        }
        if (waterPos == null) {
            return false;
        }

        if (standPos == null || !canUseStandPos(standPos)) {
            standPos = findNearestStandSpot(waterPos);
            failedMoveTicks = 0;
        }

        return standPos != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (chibi.level().isClientSide()) {
            return false;
        }
        if (!chibi.isAlive() || !chibi.isTame() || chibi.isOrderedToSit()) {
            return false;
        }
        if (chibi.getTarget() != null && chibi.getTarget().isAlive()) {
            return false;
        }
        if (!hasOrCanEquipFishingRod()) {
            return false;
        }

        // Important: keep the goal running while it is still searching for
        // water, walking to the shore, or preparing the cast. The previous
        // implementation returned false until a hook existed, so GoalSelector
        // stopped it before tick() could ever reach castFishingHook().
        return fishingHook == null || fishingHook.isAlive();
    }

    @Override
    public void start() {
        if (!hasFishingRodInMainHand() && !equipFishingRod()) {
            stop();
            return;
        }

        if (waterPos == null || !isWaterPosValid()) {
            searchForDestination();
        }

        standPos = findNearestStandSpot(waterPos);
        failedMoveTicks = 0;
    }

    @Override
    public void tick() {
        if (chibi.level().isClientSide()) {
            return;
        }

        if (fishingHook != null && fishingHook.isAlive()) {
            if (chibi.distanceToSqr(fishingHook) > HOOK_TIMEOUT_SQR) {
                fishingHook.discard();
                fishingHook = null;
                chibi.setFishingActive(false);
            } else {
                chibi.getLookControl().setLookAt(fishingHook, 30.0F, 30.0F);
            }
            return;
        }

        if (!hasFishingRodInMainHand() && !equipFishingRod()) {
            stop();
            return;
        }

        if (!isWaterPosValid()) {
            searchForDestination();
            standPos = findNearestStandSpot(waterPos);
        }

        if (waterPos == null) {
            stop();
            return;
        }

        if (standPos == null) {
            standPos = findNearestStandSpot(waterPos);
        }

        if (standPos == null || !canUseStandPos(standPos)) {
            standPos = findNearestStandSpot(waterPos);
            failedMoveTicks = 0;
        }

        if (standPos == null) {
            return;
        }

        double distance = chibi.distanceToSqr(
                standPos.getX() + 0.5D,
                standPos.getY(),
                standPos.getZ() + 0.5D
        );

        if (distance > CAST_DISTANCE * CAST_DISTANCE) {
            boolean moved = chibi.getNavigation().moveTo(
                    standPos.getX() + 0.5D,
                    standPos.getY(),
                    standPos.getZ() + 0.5D,
                    MOVE_SPEED
            );
            failedMoveTicks = moved ? 0 : failedMoveTicks + 1;

            if (!moved || (chibi.getNavigation().isDone() && failedMoveTicks > 10)) {
                standPos = findNearestStandSpot(waterPos);
                failedMoveTicks = 0;
            }

            lookAtWater();
            return;
        }

        chibi.getNavigation().stop();
        lookAtWater();
        castFishingHook();
    }

    @Override
    public void stop() {
        chibi.getNavigation().stop();

        if (fishingHook != null && fishingHook.isAlive()) {
            fishingHook.discard();
        }

        fishingHook = null;
        chibi.setFishingActive(false);
        standPos = null;
        waterPos = null;
        nextCheckTick = CHECK_DELAY;
        failedMoveTicks = 0;
    }

    /**
     * Port of TLM's MaidRideFindWaterTask destination search.
     */
    private void searchForDestination() {
        BlockPos center = chibi.blockPosition();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        // Match TLM's traversal order exactly: 0, +1, -1, +2, -2, ...
        int verticalSearchStart = 0;
        for (int y = verticalSearchStart; y <= VERTICAL_SEARCH_RANGE;
             y = y > 0 ? -y : 1 - y) {
            for (int i = 0; i < SEARCH_RANGE; ++i) {
                for (int x = 0; x <= i; x = x > 0 ? -x : 1 - x) {
                    for (int z = x < i && x > -i ? i : 0;
                         z <= i;
                         z = z > 0 ? -z : 1 - z) {

                        mutable.setWithOffset(center, x, y - 1, z);

                        if (chibi.hasRestriction() && !chibi.isWithinRestriction(mutable)) {
                            continue;
                        }

                        if (isSuitableFishingWater(mutable)) {
                            waterPos = mutable.immutable();
                            chibi.getLookControl().setLookAt(
                                    waterPos.getX(),
                                    waterPos.getY(),
                                    waterPos.getZ(),
                                    30.0F,
                                    30.0F
                            );
                            return;
                        }
                    }
                }
            }
        }

        waterPos = null;
    }

    private boolean isSuitableFishingWater(BlockPos pos) {
        // Port of IFishingType.suitableFishingHook() for the vanilla rod.
        return chibi.level().getFluidState(pos).is(FluidTags.WATER);
    }

    private boolean isWaterPosValid() {
        if (waterPos == null) {
            return false;
        }
        if (!isSuitableFishingWater(waterPos)) {
            return false;
        }

        int maxDistance = SEARCH_RANGE * SEARCH_RANGE;
        Vec3 waterVec = new Vec3(waterPos.getX(), waterPos.getY(), waterPos.getZ());
        if (chibi.distanceToSqr(waterVec) >= maxDistance) {
            return false;
        }

        return !chibi.hasRestriction() || chibi.isWithinRestriction(waterPos);
    }

    private BlockPos findNearestStandSpot(BlockPos water) {
        if (water == null) {
            return null;
        }

        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                // Water block is at the block level where the shore begins.
                // The Chibi stands in the air block ABOVE a solid shoreline block.
                BlockPos candidate = water.offset(dx, 0, dz).above();
                if (!canStandAt(candidate)) {
                    continue;
                }
                if (!isAdjacentToWater(candidate)) {
                    continue;
                }
                if (chibi.hasRestriction() && !chibi.isWithinRestriction(candidate)) {
                    continue;
                }

                var path = chibi.getNavigation().createPath(candidate, 0);
                if (path == null) {
                    continue;
                }

                double distance = candidate.distSqr(chibi.blockPosition());
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = candidate.immutable();
                }
            }
        }

        return best;
    }

    private boolean canUseStandPos(BlockPos pos) {
        return pos != null && canStandAt(pos) && isAdjacentToWater(pos);
    }

    private boolean isAdjacentToWater(BlockPos stand) {
        for (net.minecraft.core.Direction direction : new net.minecraft.core.Direction[]{
                net.minecraft.core.Direction.NORTH,
                net.minecraft.core.Direction.SOUTH,
                net.minecraft.core.Direction.EAST,
                net.minecraft.core.Direction.WEST}) {
            if (isSuitableFishingWater(stand.relative(direction).below())) {
                return true;
            }
        }
        return false;
    }

    private boolean canStandAt(BlockPos pos) {
        var level = chibi.level();
        var state = level.getBlockState(pos);
        var below = level.getBlockState(pos.below());

        return level.getFluidState(pos).isEmpty()
                && state.getCollisionShape(level, pos).isEmpty()
                && !below.getCollisionShape(level, pos.below()).isEmpty()
                && level.getFluidState(pos.below()).isEmpty();
    }

    private void lookAtWater() {
        if (waterPos == null) {
            return;
        }

        chibi.getLookControl().setLookAt(
                waterPos.getX() + 0.5D,
                waterPos.getY() + 0.25D,
                waterPos.getZ() + 0.5D,
                30.0F,
                30.0F
        );
    }

    /**
     * TLM's task creates its own MaidFishingHook at the chosen water position.
     * ProjectFishingHook keeps the same gameplay idea but uses a real flight
     * vector so the line/bobber reaches the water from Chibi's shore position.
     */
    private void castFishingHook() {
        if (!hasFishingRodInMainHand() || waterPos == null || standPos == null) {
            return;
        }
        if (chibi.distanceToSqr(standPos.getX() + 0.5D, standPos.getY(), standPos.getZ() + 0.5D) > CAST_DISTANCE * CAST_DISTANCE) {
            return;
        }

        Vec3 target = Vec3.atCenterOf(waterPos);
        Vec3 start = new Vec3(
                chibi.getX(),
                chibi.getY() + chibi.getEyeHeight() - 0.15D,
                chibi.getZ()
        );

        Vec3 delta = target.subtract(start);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        double speed = 0.65D;

        double vx = delta.x / Math.max(horizontal, 0.001D) * speed;
        double vz = delta.z / Math.max(horizontal, 0.001D) * speed;
        double vy = Mth.clamp(
                delta.y / Math.max(horizontal, 1.0D) * 0.45D + 0.20D,
                0.12D,
                0.55D
        );

        fishingHook = new ProjectFishingHook(
                chibi,
                chibi.level(),
                start,
                new Vec3(vx, vy, vz)
        );

        chibi.level().addFreshEntity(fishingHook);
        chibi.setFishingActive(true);

        chibi.swing(InteractionHand.MAIN_HAND);
        chibi.level().playSound(
                null,
                chibi.getX(),
                chibi.getY(),
                chibi.getZ(),
                SoundEvents.FISHING_BOBBER_THROW,
                SoundSource.NEUTRAL,
                0.5F,
                0.4F / (chibi.level().getRandom().nextFloat() * 0.4F + 0.8F)
        );
    }

    private boolean hasFishingRodInMainHand() {
        ItemStack stack = chibi.getMainHandItem();
        return stack.is(Items.FISHING_ROD)
                || stack.canPerformAction(ToolActions.FISHING_ROD_CAST);
    }

    private boolean hasOrCanEquipFishingRod() {
        if (hasFishingRodInMainHand()) {
            return true;
        }

        ChibiInventory inv = chibi.getChibiInventory();
        for (int i = ChibiInventory.STORAGE_START;
             i < ChibiInventory.STORAGE_START + ChibiInventory.STORAGE_SIZE;
             i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.is(Items.FISHING_ROD)) {
                return true;
            }
        }

        return false;
    }

    private boolean equipFishingRod() {
        if (hasFishingRodInMainHand()) {
            return true;
        }

        ChibiInventory inv = chibi.getChibiInventory();
        int rodSlot = -1;

        for (int i = ChibiInventory.STORAGE_START;
             i < ChibiInventory.STORAGE_START + ChibiInventory.STORAGE_SIZE;
             i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.is(Items.FISHING_ROD)) {
                rodSlot = i;
                break;
            }
        }

        if (rodSlot == -1) {
            return false;
        }

        ItemStack current = inv.getItem(ChibiInventory.MAIN_HAND_SLOT);
        if (!current.isEmpty()) {
            return false;
        }

        ItemStack rod = inv.removeItemNoUpdate(rodSlot);
        inv.setItem(ChibiInventory.MAIN_HAND_SLOT, rod);
        inv.setChanged();
        return true;
    }
}

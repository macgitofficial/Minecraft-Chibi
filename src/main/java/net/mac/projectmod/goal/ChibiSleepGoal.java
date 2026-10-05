package net.mac.projectmod.goal;

import net.mac.projectmod.entity.ChibiEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.tags.BlockTags;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * TLM-inspired rest behavior for Chibi.
 *
 * Uses normal Minecraft beds instead of TLM Maid Beds.
 * The bed search radius is intentionally limited to 10 blocks.
 */
public class ChibiSleepGoal extends Goal {
    private static final int BED_SEARCH_RADIUS = 10;
    private static final int CLOSE_ENOUGH = 2;

    private final ChibiEntity chibi;
    @Nullable
    private BlockPos targetBed;

    public ChibiSleepGoal(ChibiEntity chibi) {
        this.chibi = chibi;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /** Same basic time window in which players can normally sleep. */
    public static boolean isRestTime(ChibiEntity chibi) {
        if (chibi.level().isClientSide()) {
            return chibi.getPose() == Pose.SLEEPING || isNight(chibi);
        }
        return isNight(chibi);
    }

    private static boolean isNight(ChibiEntity chibi) {
        long time = chibi.level().getDayTime() % 24000L;
        // Vanilla player sleeping window: 12,542 <= time < 23,460.
        return time >= 12542L && time < 23460L;
    }

    @Override
    public boolean canUse() {
        if (chibi.isDeadOrDying()
                || chibi.isOrderedToSit()
                || chibi.isSleeping()
                || !isRestTime(chibi)) {
            return false;
        }

        targetBed = findNearestBed();
        return targetBed != null;
    }

    @Override
    public boolean canContinueToUse() {
        return !chibi.isDeadOrDying()
                && !chibi.isOrderedToSit()
                && isRestTime(chibi)
                && (chibi.isSleeping() || targetBed != null);
    }

    @Override
    public void start() {
        if (targetBed == null) {
            return;
        }

        if (isCloseEnough(targetBed)) {
            sleepAt(targetBed);
        } else {
            chibi.getNavigation().moveTo(
                    targetBed.getX() + 0.5D,
                    targetBed.getY(),
                    targetBed.getZ() + 0.5D,
                    0.8D
            );
        }
    }

    @Override
    public void tick() {
        if (!isRestTime(chibi)) {
            if (chibi.isSleeping()) {
                chibi.stopSleeping();
            }
            return;
        }

        if (chibi.isSleeping()) {

            chibi.getNavigation().stop();
            return;
        }

        if (targetBed == null || !isValidBed(targetBed)) {
            targetBed = findNearestBed();
            if (targetBed == null) {
                return;
            }
        }

        if (isCloseEnough(targetBed)) {
            chibi.getNavigation().stop();
            sleepAt(targetBed);
        } else {
            chibi.getNavigation().moveTo(
                    targetBed.getX() + 0.5D,
                    targetBed.getY(),
                    targetBed.getZ() + 0.5D,
                    0.8D
            );
        }
    }

    @Override
    public void stop() {
        chibi.getNavigation().stop();
        targetBed = null;

        // TLM MaidClearSleepTask equivalent: leaving the rest period wakes Chibi.
        if (chibi.isSleeping() && !isRestTime(chibi)) {
            chibi.stopSleeping();
        }
    }

    private void sleepAt(BlockPos bedPos) {
        if (!isValidBed(bedPos) || chibi.isSleeping()) {
            return;
        }

        chibi.startSleeping(bedPos);
        chibi.setPos(
                bedPos.getX() + 0.5D,
                bedPos.getY() + 0.8D,
                bedPos.getZ() + 0.5D
        );
        chibi.getNavigation().stop();
    }

    private boolean isCloseEnough(BlockPos pos) {
        return pos.distToCenterSqr(chibi.position()) <= CLOSE_ENOUGH * CLOSE_ENOUGH;
    }

    @Nullable
    private BlockPos findNearestBed() {
        BlockPos origin = chibi.blockPosition();
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-BED_SEARCH_RADIUS, -BED_SEARCH_RADIUS, -BED_SEARCH_RADIUS),
                origin.offset(BED_SEARCH_RADIUS, BED_SEARCH_RADIUS, BED_SEARCH_RADIUS))) {
            if (!isValidBed(pos)) {
                continue;
            }

            double distance = pos.distSqr(origin);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = pos.immutable();
            }
        }

        return nearest;
    }

    private boolean isValidBed(BlockPos pos) {
        BlockState state = chibi.level().getBlockState(pos);

        // Only use the head half, matching how vanilla/TLM place an entity on a bed.
        if (!state.is(BlockTags.BEDS)
                || !state.hasProperty(BedBlock.PART)
                || state.getValue(BedBlock.PART) != BedPart.HEAD) {
            return false;
        }

        // Do not take a bed that a player/entity is currently occupying.
        return !state.hasProperty(BedBlock.OCCUPIED)
                || !state.getValue(BedBlock.OCCUPIED);
    }
}

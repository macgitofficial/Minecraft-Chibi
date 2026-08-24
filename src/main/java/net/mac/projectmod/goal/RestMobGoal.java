package net.mac.projectmod.goal;

import net.mac.projectmod.entity.ChibiEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.Optional;

public class RestMobGoal extends Goal {

    private final ChibiEntity npc;
    private final double pBedDistance;

    public RestMobGoal(ChibiEntity npc, double pBedDistance) {
        this.npc = npc;
        this.pBedDistance = pBedDistance;

        this.setFlags(EnumSet.of(
                Goal.Flag.MOVE,
                Goal.Flag.LOOK
        ));
    }
    private BlockPos nearestBed = null;
    private boolean shouldRest;

    @Override
    public boolean canUse() {
        Player owner = this.npc.getOwner();
        long time = this.npc.level().getDayTime() % 24000;
        if(owner==null)
            return false;
        shouldRest = time >= 16000 || time < 1 || owner.isSleeping();

        BlockPos center = this.npc.blockPosition();
        int radius = 16;

        double nearestDist = Double.MAX_VALUE;
        if (owner.isSleeping()) {
            Optional<BlockPos> ownerSleepingPos = owner.getSleepingPos();
            nearestBed = ownerSleepingPos.get();
        } else
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -4, -radius),
                center.offset(radius, 4, radius))) {

            if (this.npc.level().getBlockState(pos).is(BlockTags.BEDS)) {
                double dist = pos.distSqr(center);

                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearestBed = pos.immutable();
                }
            }
        }

        if (!shouldRest)
            return false;

        return nearestBed != null;
    }

    @Override
    public void tick() {
        npc.getNavigation().moveTo(
                nearestBed.getX() + 0.5,
                nearestBed.getY(),
                nearestBed.getZ() + 0.5,
                1.0
        );
        if (shouldRest && npc.blockPosition().closerThan(nearestBed, 0.5)) {
            npc.getNavigation().stop();

            // ชั่วคราวไว้ก่อน
            npc.setDeltaMovement(0, 0, 0);

            // ภายหลังค่อยเล่น animation นอน
        }
        if (!npc.level().getBlockState(nearestBed).is(BlockTags.BEDS)) {
            stop();
        }
    }

    @Override
    public boolean canContinueToUse() {
        long time = npc.level().getDayTime() % 24000;
        Player owner = npc.getOwner();
        shouldRest = time >= 16000 || time < 1 || owner.isSleeping();
        return shouldRest
                && nearestBed != null
                && !npc.blockPosition().closerThan(nearestBed, 2);
    }

    @Override
    public void stop() {
        nearestBed = null;
    }
}

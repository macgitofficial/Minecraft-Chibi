package net.mac.projectmod.goal;

import net.mac.projectmod.entity.ChibiEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class FollowPlayerGoal extends Goal {

    private final double pSpeed;
    private final float pStartDistance;
    private final float pStopDistance;
//    private final double pTeleportDistance;
    private int timeToRecalcPath;
    private final ChibiEntity pMob;
    private Player owner;

    public FollowPlayerGoal(
            ChibiEntity pMob,
            double pSpeed,
            float pStartDistance,
            float pStopDistance
    ) {
        this.pMob = pMob;
        this.pSpeed = pSpeed;
        this.pStartDistance = pStartDistance;
        this.pStopDistance = pStopDistance;
//        this.pTeleportDistance = pTeleportDistance;

        this.setFlags(EnumSet.of(
                Goal.Flag.MOVE,
                Goal.Flag.LOOK
                ));
    }

    @Override
    public boolean canUse() {
        Player owner = this.pMob.getOwner();

        if (owner==null||!owner.isAlive()) {
            return false;
        }

        this.owner = owner;

        return !owner.isSpectator()
                && this.pMob.distanceToSqr(this.owner) > this.pStartDistance * this.pStartDistance;

    }

    @Override
    public boolean canContinueToUse() {
        return this.owner != null
                && this.owner.isAlive()
                && !this.owner.isSpectator()
                && this.pMob.distanceToSqr(this.owner)
                > this.pStopDistance * this.pStopDistance;
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
        this.pMob.getNavigation().moveTo(
                this.owner,
                this.pSpeed
        );
    }

    @Override
    public void tick() {
        boolean flag = this.pMob.shouldTryTeleportToOwner();

        if(!flag) {
            this.pMob.getLookControl().setLookAt(
                    this.owner,
                    10.0F,
                    this.pMob.getMaxHeadXRot()
            );
        }

        if (this.owner != null) {
            this.pMob.getNavigation().moveTo(
                    this.owner,
                    this.pSpeed
            );

            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = this.adjustedTickDelay(10);
                if (flag) {
                    this.pMob.tryToTeleportToOwner();
                    this.pMob.getNavigation().stop();
                }
            }
        }

    }

    @Override
    public void stop() {
        this.owner = null;
        this.pMob.getNavigation().stop();
    }


}


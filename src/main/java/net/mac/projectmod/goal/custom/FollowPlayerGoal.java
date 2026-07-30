package net.mac.projectmod.goal.custom;

import net.mac.projectmod.entity.custom.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class FollowPlayerGoal extends Goal {

    private final NpcEntity pMob;
    private final double pSpeed;
    private final float pStartDistance;
    private final float pStopDistance;

    private Player target;

    public FollowPlayerGoal(
            NpcEntity pMob,
            double pSpeed,
            float pStartDistance,
            float pStopDistance
    ) {
        this.pMob = pMob;
        this.pSpeed = pSpeed;
        this.pStartDistance = pStartDistance;
        this.pStopDistance = pStopDistance;

        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        this.target = this.pMob.level().getNearestPlayer(
                this.pMob,
                64.0D
        );

        return this.target != null
                && !this.target.isSpectator()
                && this.pMob.distanceToSqr(this.target) > this.pStartDistance * this.pStartDistance;

    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null
                && this.target.isAlive()
                && !this.target.isSpectator()
                && this.pMob.distanceToSqr(this.target)
                > this.pStopDistance * this.pStopDistance;
    }

    @Override
    public void start() {
        this.pMob.getNavigation().moveTo(
                this.target,
                this.pSpeed
        );
    }

    @Override
    public void tick() {
//        boolean flag = this.pMob.shouldTryTeleportToOwner();
        if (this.target != null) {
            this.pMob.getNavigation().moveTo(
                    this.target,
                    this.pSpeed
            );
            this.pMob.getLookControl().setLookAt(
                    this.target);
        }
    }

    @Override
    public void stop() {
        this.target = null;
        this.pMob.getNavigation().stop();
    }

    //TeleportToOwner
//    public boolean shouldTryTeleportToOwner() {
//        LivingEntity livingentity = this.getOwner();
//        return livingentity != null && this.distanceToSqr(this.getOwner()) >= 144.0;
//    }
//
//    private void teleportToAroundBlockPos(BlockPos pPos) {
//        for (int i = 0; i < 10; i++) {
//            int j = this.random.nextIntBetweenInclusive(-3, 3);
//            int k = this.random.nextIntBetweenInclusive(-3, 3);
//            if (Math.abs(j) >= 2 || Math.abs(k) >= 2) {
//                int l = this.random.nextIntBetweenInclusive(-1, 1);
//                if (this.maybeTeleportTo(pPos.getX() + j, pPos.getY() + l, pPos.getZ() + k)) {
//                    return;
//                }
//            }
//        }
//    }
//
//    private boolean maybeTeleportTo(int pX, int pY, int pZ) {
//        if (!this.canTeleportTo(new BlockPos(pX, pY, pZ))) {
//            return false;
//        } else {
//            this.moveTo((double)pX + 0.5, (double)pY, (double)pZ + 0.5, this.getYRot(), this.getXRot());
//            this.navigation.stop();
//            return true;
//        }
//    }
}


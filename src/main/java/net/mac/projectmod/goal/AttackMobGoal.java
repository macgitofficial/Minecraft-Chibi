package net.mac.projectmod.goal;

import net.mac.projectmod.entity.ChibiEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.List;

public class AttackMobGoal extends Goal {
    private final ChibiEntity pMob;
    private final double pSpeed;
    private final double pAttackRange;
    private final double pOwnerMaxDistance;
    private final double pNearbyDistance;

    private int attackCooldown;

    public AttackMobGoal(
            ChibiEntity pMob,
            double pSpeed,
            double pAttackRange,
            double pOwnerMaxDistance,
            double pNearbyDistance
    ) {

        this.pMob = pMob;
        this.pSpeed = pSpeed;
        this.pAttackRange = pAttackRange;
        this.pOwnerMaxDistance = pOwnerMaxDistance;
        this.pNearbyDistance = pNearbyDistance;

        this.setFlags(EnumSet.of(
                Goal.Flag.MOVE,
                Goal.Flag.LOOK
        ));
    }

    //หา Mob ในระยะ 8.0 blocks
    private LivingEntity findNearbyMob(double nearbyDistance) {
        List<LivingEntity> entities =
                this.pMob.level().getEntitiesOfClass(
                        LivingEntity.class,
                        this.pMob.getBoundingBox().inflate(nearbyDistance),
                        entity -> {
                            if (!entity.isAlive()) {
                                return false;
                            }
                            if (entity == this.pMob) {
                                return false;
                            }
                            if (entity == this.pMob.getOwner()) {
                                return false;
                            }

                            return entity instanceof Monster;
                        }
                );

        return entities.isEmpty()
                ? null
                : entities.get(0);
    }

    @Override
    public boolean canUse() {
        //Owner ห่างไกล
        Player owner = this.pMob.getOwner();
        if(owner==null||!owner.isAlive()){
            return false;
        }
        if(this.pMob.distanceToSqr(owner)>pOwnerMaxDistance*pOwnerMaxDistance){
            return false;
        }

        //หา Mob ใกล้เคียงอัตโนมัติ
        if(this.pMob.getCombatTarget() == null) {
            this.pMob.setCombatTarget(findNearbyMob(this.pNearbyDistance));
        }
        return this.pMob.getCombatTarget()!=null;
    }

    @Override
    public void tick() {
        if (this.pMob.getCombatTarget() == null) {
            return;
        }

        Player owner = this.pMob.getOwner();
        if (owner == null) {
            return;
        }

        //เจอ Mob ใกล้ผู้เล่นสุด
        LivingEntity target = findNearbyMob(4);
        if (
                target != null &&
                        this.pMob.getCombatTarget().distanceToSqr(this.pMob.getOwner())
                                < this.pMob.getOwner().distanceToSqr(target)
        ) {
            this.pMob.setCombatTarget(target);
        }

        // มองไปที่ Mob
        this.pMob.getLookControl().setLookAt(
                this.pMob.getCombatTarget(),
                30.0F,
                30.0F
        );

        // ยังอยู่ไกล → เดินเข้าไป
        double distance =
                this.pMob.distanceToSqr(this.pMob.getCombatTarget());
        if (distance > pAttackRange * pAttackRange) {
            this.pMob.getNavigation().moveTo(
                    this.pMob.getCombatTarget(),
                    this.pSpeed
            );
            return;
        }

        // อยู่ในระยะโจมตี
        this.pMob.getNavigation().stop();
        if (this.attackCooldown <= 0) {
            this.pMob.doHurtTarget(this.pMob.getCombatTarget());
            if(this.pMob.level().isClientSide())
                this.pMob.swing(InteractionHand.MAIN_HAND);
            this.attackCooldown = 5;
        }
        this.attackCooldown--;
    }

    @Override
    public boolean canContinueToUse() {
        Player owner = this.pMob.getOwner();

        if (owner == null) {
            return false;
        }
        if (!owner.isAlive()) {
            return false;
        }
        if (this.pMob.getCombatTarget() == null) {
            return false;
        }
        if (!this.pMob.getCombatTarget().isAlive()) {
            return false;
        }

        // Owner ไกลเกินไป
        if (this.pMob.distanceToSqr(owner)
                > pOwnerMaxDistance * pOwnerMaxDistance) {
            return false;
        }

        return true;
    }

    @Override
    public void stop() {

        // หยุดเดิน
        this.pMob.getNavigation().stop();
        // ล้าง Target
        this.pMob.clearCombatTarget();

        this.attackCooldown = 0;
    }
}

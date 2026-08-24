package net.mac.projectmod.entity;

import net.mac.projectmod.goal.*;
import net.mac.projectmod.gui.NpcInventoryMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemStackHandler;

import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class ChibiEntity extends PathfinderMob implements GeoAnimatable {

    public ChibiEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /*
    Animation Controller
     */
    private PlayState idleAnimation(AnimationState<ChibiEntity> state) {
        state.getController().setAnimation(
                RawAnimation.begin().thenLoop("idle")
        );

        return PlayState.CONTINUE;
    }

    private final AnimatableInstanceCache geoCache =
            GeckoLibUtil.createInstanceCache(this);

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(
                new AnimationController<>(
                        this,
                        "idle_controller",
                        0,
                        this::idleAnimation
                )
        );
    }

    @Override
    public double getTick(Object object) {
        return tickCount;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /*
    Chibi Goals
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        //Combat
        this.goalSelector.addGoal(1, new AttackMobGoal(this, 1.5D,2D,16D,8D));
        //Follow
        this.goalSelector.addGoal(2, new FollowPlayerGoal(this,2D, 8.0f, 3.0f));
        //Work
        this.goalSelector.addGoal(3, new ChopTreeGoal(this));
        this.goalSelector.addGoal(4, new GetItemGoal(this, 2d,8d));
        //Rest
        this.goalSelector.addGoal(5, new RestMobGoal(this, 16d));
        //Idle
        this.goalSelector.addGoal(6, new PanicGoal(this, 2.5D));
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));


    }
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.ATTACK_SPEED, 1.0D);
    }


    /*
    คลิกขวา Chibi แล้ว InvScreen ขึ้นมา
     */
    @Override
    public InteractionResult interactAt(
            Player player,
            Vec3 hitPos,
            InteractionHand hand) {

        if (!this.level().isClientSide()
                && hand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer)
        {
            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (containerId, playerInventory, playerEntity) ->
                                    new NpcInventoryMenu(
                                            containerId,
                                            playerInventory,
                                            this
                                    ),
                            Component.literal("Chibi Inventory")
                    ),
                    buf -> buf.writeInt(this.getId())
            );
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }


    @Override
    public void tick() {
        super.tick();

    }
    /*
     * =========================================================
     * SPIDER STYLE CLIMB เปลี่ยนเป็น Scaffolding
     * =========================================================
     */

    private boolean climbing;
    private double climbTargetY;

    public boolean isClimbing() {
        return climbing;
    }

    @Override
    public boolean onClimbable() {
        return this.isClimbing();
    }

    public void setClimbing(boolean value) {
        climbing = value;

        if (value) {
            getNavigation().stop();
        } else {
            Vec3 v = getDeltaMovement();

            setDeltaMovement(
                    v.x,
                    0,
                    v.z
            );
        }
    }

    public void setClimbTargetY(double y) {
        climbTargetY = y;
    }

    private void tickClimbing() {

        /*
         * ต้องเช็คว่ากำลัง "ปีน" อยู่จริงก่อน
         * ไม่งั้น hasWall() เป็น true แทบตลอดเวลาที่เดินอยู่ใกล้
         * บล็อกอะไรก็ได้ (กำแพง รั้ว เนินดิน ต้นไม้) ทำให้โค้ดข้างล่าง
         * แย่งควบคุมความเร็วแนวตั้งไปจาก pathfinding ทั้งที่ไม่ได้ตั้งใจปีน
         */
        if (!climbing) {
            return;
        }

        if (!hasWall()) {
            setClimbing(false);
            return;
        }

        /*
         * อะไรชนหัวตอนปีน → ทุบ
         * (ยกเว้น log ปล่อยให้ ChopTreeGoal เป็นคนตัดตอน BREAK เอง
         *  ไม่งั้น chibi จะทำลาย target ของตัวเองไปก่อนถึงยอด)
         */
        BlockPos head = blockPosition().above();
        BlockState headState = level().getBlockState(head);

        if (!headState.isAir() && !headState.is(BlockTags.LOGS)) {
            level().destroyBlock(
                    head,
                    true,
                    this
            );
        }

        double dy = climbTargetY - getY();

        if (Math.abs(dy) < 0.1D) {
            setDeltaMovement(
                    getDeltaMovement().x,
                    0,
                    getDeltaMovement().z
            );
            return;
        }

        double speed =
                Math.max(
                        -0.12D,
                        Math.min(
                                0.18D,
                                dy * 0.25D
                        )
                );

        Vec3 v = getDeltaMovement();

        setDeltaMovement(
                v.x,
                speed,
                v.z
        );
    }

    private boolean hasWall() {

        BlockPos pos = blockPosition();

        for (var dir :
                net.minecraft.core.Direction.Plane.HORIZONTAL) {

            if (!level()
                    .getBlockState(
                            pos.relative(dir)
                    )
                    .isAir()) {

                return true;
            }
        }

        return false;
    }

    /*
     * INVENTORY / AXE
     */
    private final ItemStackHandler inventory = new ItemStackHandler(9);

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public int findAxeSlot() {

        for (int i = 0;
             i < inventory.getSlots();
             i++) {

            if (inventory.getStackInSlot(i)
                    .getItem() instanceof AxeItem) {

                return i;
            }
        }

        return -1;
    }

    public boolean equipAxe() {

        if (getMainHandItem().getItem()
                instanceof AxeItem) {

            return true;
        }

        int slot = findAxeSlot();

        if (slot == -1) {
            return false;
        }

        setItemSlot(
                EquipmentSlot.MAINHAND,
                inventory.getStackInSlot(slot).copy()
        );

        inventory.setStackInSlot(
                slot,
                ItemStack.EMPTY
        );

        return true;
    }

    /*
    Chibi มี owner และ teleportToOwner ได้ด้วย
    Save ข้อมูล
     */

    private UUID ownerUUID;
    public UUID getOwnerUUID() {
        return this.ownerUUID;
    }
    public void setOwnerUUID(UUID uuid) {
        this.ownerUUID = uuid;
    }
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        if (this.ownerUUID != null) {
            tag.putUUID("Owner", this.ownerUUID);
        }

        tag.put(
                "Inventory",
                inventory.serializeNBT(
                        this.registryAccess()
                )
        );
        tag.put(
                "MainHand",
                getMainHandItem().save(
                        this.registryAccess()
                )
        );
    }
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }

        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(
                    this.registryAccess(),
                    tag.getCompound("Inventory")
            );
        }
        if (tag.contains("MainHand")) {
            ItemStack stack =
                    ItemStack.parse(
                            this.registryAccess(),
                            tag.getCompound("MainHand")
                    ).orElse(ItemStack.EMPTY);

            setItemSlot(
                    EquipmentSlot.MAINHAND,
                    stack
            );
        }
    }
    public Player getOwner() {
        if (this.ownerUUID == null) {
            return null;
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            return serverLevel.getPlayerByUUID(this.ownerUUID);
        }

        return null;
    }

    /*
    Chibi วาร์ปหาผู้เล่นที่ห่างไกล
     */
    public void tryToTeleportToOwner() {
        LivingEntity livingentity = this.getOwner();
        if (livingentity != null) {
            this.teleportToAroundBlockPos(livingentity.blockPosition());
        }
    }
    public boolean shouldTryTeleportToOwner() {
        LivingEntity livingentity = this.getOwner();
        return livingentity != null && this.distanceToSqr(this.getOwner()) >= 400.0;
    }
    private void teleportToAroundBlockPos(BlockPos pPos) {
        for (int i = 0; i < 10; i++) {
            int j = this.random.nextIntBetweenInclusive(-3, 3);
            int k = this.random.nextIntBetweenInclusive(-3, 3);
            if (Math.abs(j) >= 2 || Math.abs(k) >= 2) {
                int l = this.random.nextIntBetweenInclusive(-1, 1);
                if (this.maybeTeleportTo(pPos.getX() + j, pPos.getY() + l, pPos.getZ() + k)) {
                    return;
                }
            }
        }
    }
    private boolean maybeTeleportTo(int pX, int pY, int pZ) {
        if (!this.canTeleportTo(new BlockPos(pX, pY, pZ))) {
            return false;
        } else {
            this.moveTo((double)pX + 0.5, (double)pY, (double)pZ + 0.5, this.getYRot(), this.getXRot());
            this.navigation.stop();
            return true;
        }
    }
    private boolean canTeleportTo(BlockPos pPos) {
        PathType pathtype = WalkNodeEvaluator.getPathTypeStatic(this, pPos);
        if (pathtype != PathType.WALKABLE) {
            return false;
        } else {
            BlockState blockstate = this.level().getBlockState(pPos.below());
            if (!this.canFlyToOwner() && blockstate.getBlock() instanceof LeavesBlock) {
                return false;
            } else {
                BlockPos blockpos = pPos.subtract(this.blockPosition());
                return this.level().noCollision(this, this.getBoundingBox().move(blockpos));
            }
        }
    }
    protected boolean canFlyToOwner() {
        return false;
    }

    /*
    Target ของ chibi
     */
    private LivingEntity combatTarget;

    public LivingEntity getCombatTarget() {
        return this.combatTarget;
    }
    public void setCombatTarget(LivingEntity target) {
        this.combatTarget = target;
    }
    public void clearCombatTarget() {
        this.combatTarget = null;
    }

}
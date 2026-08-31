package net.mac.projectmod.entity;

import net.mac.projectmod.goal.FishingGoal;
import net.mac.projectmod.goal.PickUpItemGoal;
import net.mac.projectmod.goal.TreeChopGoal;
import net.mac.projectmod.gui.ChibiInventory;
import net.mac.projectmod.gui.ChibiInventoryMenu;
import net.mac.projectmod.sound.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;


import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import net.mac.projectmod.client.animation.ChibiAnimationManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ChibiEntity extends TamableAnimal implements GeoEntity {

    public ChibiEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }


    /*
     * Sync ว่า AI (goal) กำลังสั่งให้เดินอยู่หรือไม่ จาก server ไป client
     * เพราะ getNavigation().isDone() อ่านได้แค่ฝั่ง server เท่านั้น
     * ใช้ตัวนี้แทน เพื่อให้ walk animation เล่นเฉพาะตอน chibi เดินเอง
     * ไม่ใช่ตอนโดนผลัก/knockback/กระแสน้ำ
     */
    private static final EntityDataAccessor<Boolean> DATA_WALKING =
            SynchedEntityData.defineId(ChibiEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SITTING =
            SynchedEntityData.defineId(ChibiEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_HUNGER =
            SynchedEntityData.defineId(ChibiEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_FISHING =
            SynchedEntityData.defineId(ChibiEntity.class,EntityDataSerializers.BOOLEAN);
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_WALKING, false);
        builder.define(DATA_SITTING, false);
        builder.define(DATA_HUNGER, 20);
        builder.define(DATA_FISHING, false);
    }

    public int getHunger() {
        return this.entityData.get(DATA_HUNGER);
    }

    public void setHunger(int hunger) {
        this.entityData.set(
                DATA_HUNGER,
                Mth.clamp(hunger, 0, 20)
        );
    }

    public boolean isWalkingByAI() {
        return this.entityData.get(DATA_WALKING);
    }
    public boolean isSittingByServer() { return this.entityData.get(DATA_SITTING); }

    /** TLM-style synced state: the Chibi currently has an active fishing hook. */
    public boolean hasFishingHook() {
        return this.entityData.get(DATA_FISHING);
    }

    public void setFishingActive(boolean fishing) {
        this.entityData.set(DATA_FISHING, fishing);
    }


    /*
     * GeckoLib animation controllers
     *
     * The controller layout is delegated to ChibiAnimationManager so the
     * model/animation system can follow the same separation used by
     * Touhou Little Maid.
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        ChibiAnimationManager.registerControllers(this, controllers);
    }

    @Override
    public double getTick(Object object) {
        return tickCount;
    }

    private final AnimatableInstanceCache geoCache =
            GeckoLibUtil.createInstanceCache(this);

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }


    /*
    Chibi Goals
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new SitWhenOrderedToGoal(this));
//        this.goalSelector.addGoal(1, new TreeChopGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(3, new FishingGoal(this));
        this.goalSelector.addGoal(5, new FloatGoal(this));
        this.goalSelector.addGoal(6, new PanicGoal(this, 1.2D));
        this.goalSelector.addGoal(7, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F));
        this.goalSelector.addGoal(8, new PickUpItemGoal(this));
        this.goalSelector.addGoal(9, new RandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 6.0F));

        // ต้องเพิ่ม targetSelector ด้วย ไม่งั้น MeleeAttackGoal ไม่มี target ให้ทำงาน
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));  // เจ้าของโดนตี -> ช่วยตี
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));    // เจ้าของตีใคร -> ช่วยตี
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, true)); // ถ้าอยากให้ auto-hostile ด้วย
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.ATTACK_SPEED, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 32D);
    }


    /*
     * TamableAnimal -> Animal -> AgeableMob บังคับให้ implement 2 ตัวนี้
     * Chibi ไม่มีระบบผสมพันธุ์ แต่กำหนดว่า "อาหาร" ของ Chibi คือ Cake
     */
    @Override
    public boolean isFood(ItemStack pStack) {
        return pStack.is(Items.CAKE);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
        return null;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.CHIBI_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.CHIBI_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.CHIBI_DEATH.get();
    }

    /*
    ป้อน Cake ให้ Chibi ที่ยังไม่มีเจ้าของ -> tame ทันที (ไม่สุ่ม) แล้ว Player เป็นเจ้าของ
    ถ้าไม่ใช่กรณีนี้ -> คลิกขวา Chibi แล้ว InvScreen ขึ้นมาเหมือนเดิม
     */
    @Override
    public InteractionResult interactAt(
            Player player,
            Vec3 hitPos,
            InteractionHand hand) {

        ItemStack itemInHand = player.getItemInHand(hand);

        if (!this.isTame() && this.isFood(itemInHand)) {
            if (!this.level().isClientSide()) {
                if (!player.getAbilities().instabuild) {
                    itemInHand.shrink(1);
                }

                this.tame(player);
                this.level().broadcastEntityEvent(this, (byte) 7); // หัวใจ (tame สำเร็จ)
                this.navigation.stop();
                this.setTarget(null);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        if (!this.isTame()) {
            // ยังไม่เชื่อง -> ไม่เปิด GUI ปล่อยให้ทำงานเหมือน entity ทั่วไป
            return InteractionResult.PASS;
        }

        if (this.isOwnedBy(player) && player.isShiftKeyDown()) {
            // Shift + คลิกขวา -> สั่งนั่ง/ลุก (เฉพาะเจ้าของ)
            if (!this.level().isClientSide()) {
                this.setOrderedToSit(!this.isOrderedToSit());
                this.getNavigation().stop();
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        if (!this.level().isClientSide()
                && hand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer) {

            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (containerId, playerInventory, playerEntity) ->
                                    new ChibiInventoryMenu(
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

    /*
    เพิ่ม Inv ให้ ChibiEntity
     */
    private final ChibiInventory chibiInventory = new ChibiInventory(this);

    public ChibiInventory getChibiInventory() {
        return chibiInventory;
    }

    public ItemStack getChibiEquipment(EquipmentSlot slot) {
        return super.getItemBySlot(slot);
    }

    public void setChibiEquipment(EquipmentSlot slot, ItemStack stack) {
        super.setItemSlot(slot, stack);
    }

    @Override
    public boolean doHurtTarget(Entity target) {

        equipWeaponForAttack();
        triggerAnim("main","swing_hand");
        boolean result = super.doHurtTarget(target);

        return result;
    }

    /**
     * หา weapon ใน storage slot ของ ChibiInventory แล้วย้ายมาไว้ที่ MAIN_HAND_SLOT
     * ถ้ามือถืออยู่แล้ว (เช่นเป็นอาวุธอยู่แล้ว) จะไม่ทำอะไรซ้ำ
     */
    private void equipWeaponForAttack() {

        ChibiInventory inv = this.getChibiInventory();

        ItemStack currentMainHand = inv.getItem(ChibiInventory.MAIN_HAND_SLOT);

        // ถ้ามือถืออาวุธอยู่แล้ว ไม่ต้องสลับ
        if (!currentMainHand.isEmpty() && currentMainHand.getItem() instanceof SwordItem) {
            return;
        }

        int weaponSlot = findWeaponSlot(inv);

        if (weaponSlot == -1) {
            return; // ไม่มีอาวุธใน storage เลย
        }

        ItemStack weapon = inv.removeItemNoUpdate(weaponSlot);

        // ถ้ามือมีของถืออยู่ (ไม่ใช่อาวุธ) ให้เอาไปเก็บที่ slot เดิมที่อาวุธเพิ่งว่างลง
        if (!currentMainHand.isEmpty()) {
            inv.setItem(weaponSlot, currentMainHand);
        }

        inv.setItem(ChibiInventory.MAIN_HAND_SLOT, weapon);
    }

    /**
     * วนหา slot แรกใน storage (0-11) ที่เป็นอาวุธ
     */
    private int findWeaponSlot(ChibiInventory inv) {

        for (int i = ChibiInventory.STORAGE_START;
             i < ChibiInventory.STORAGE_START + ChibiInventory.STORAGE_SIZE;
             i++) {

            ItemStack stack = inv.getItem(i);

            if (!stack.isEmpty() && stack.getItem() instanceof SwordItem) {
                return i;
            }
        }

        return -1;
    }
    private void unequipWeaponIfIdle() {
        if (this.getTarget() != null) {
            return; // กำลังสู้อยู่ ห้ามถอดอาวุธออกจากมือ
        }

        ChibiInventory inv = this.getChibiInventory();

        ItemStack mainHand = inv.getItem(ChibiInventory.MAIN_HAND_SLOT);

        if (mainHand.isEmpty() || !(mainHand.getItem() instanceof SwordItem)) {
            return; // ไม่มีอะไรถือ หรือถือของอื่นที่ผู้เล่นใส่เองอยู่ ไม่ควรไปยุ่ง
        }

        int emptySlot = findEmptyStorageSlot(inv);

        if (emptySlot == -1) {
            return; // storage เต็ม ปล่อยให้ถือไว้ก่อน
        }

        ItemStack weapon = inv.removeItemNoUpdate(ChibiInventory.MAIN_HAND_SLOT);
        inv.setItem(emptySlot, weapon);
    }

    private int findEmptyStorageSlot(ChibiInventory inv) {

        for (int i = ChibiInventory.STORAGE_START;
             i < ChibiInventory.STORAGE_START + ChibiInventory.STORAGE_SIZE;
             i++) {

            if (inv.getItem(i).isEmpty()) {
                return i;
            }
        }

        return -1;
    }
    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
//            unequipWeaponIfIdle();
            this.entityData.set(DATA_WALKING, !this.getNavigation().isDone());
            this.entityData.set(DATA_SITTING, this.isOrderedToSit());
        }
    }

}
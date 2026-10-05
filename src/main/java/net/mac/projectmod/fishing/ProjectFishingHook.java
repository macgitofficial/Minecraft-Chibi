/*
 * Fishing behaviour adapted from Touhou Little Maid (MIT-licensed code by tartaric_acid).
 * Adapted for Project Mod / Minecraft 1.21.x and ChibiEntity.
 * See THIRD_PARTY_LICENSES/TouhouLittleMaid-LICENSE-MIT.txt
 */
package net.mac.projectmod.fishing;

import net.mac.projectmod.entity.ChibiEntity;
import net.mac.projectmod.gui.ChibiInventory;
import net.mac.projectmod.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Fishing hook adapted from Touhou Little Maid for Project Mod / Forge 1.21.
 *
 * Only the fishing behaviour is kept:
 * - flies to water
 * - bobs on water
 * - waits for a bite
 * - creates fishing particles
 * - uses vanilla fishing loot
 * - returns loot to ChibiInventory
 */
public class ProjectFishingHook extends Projectile {
    private static final EntityDataAccessor<Boolean> DATA_BITING =
            SynchedEntityData.defineId(ProjectFishingHook.class, EntityDataSerializers.BOOLEAN);

    private static final int MAX_OUT_OF_WATER_TIME = 10;

    private final RandomSource synchronizedRandom = RandomSource.create();
    private final int luck;
    private final int lureSpeed;

    private boolean biting;
    private int nibble;
    private int timeUntilLured;
    private int timeUntilHooked;
    private int outOfWaterTime;
    private int life;
    private float fishAngle;
    private boolean openWater = true;
    private HookState currentState = HookState.FLYING;

    public ProjectFishingHook(EntityType<? extends ProjectFishingHook> type, Level level) {
        this(type, level, 0, 0);
    }

    protected ProjectFishingHook(EntityType<? extends ProjectFishingHook> type, Level level, int luck, int lureSpeed) {
        super(type, level);
        this.noCulling = true;
        this.luck = Math.max(0, luck);
        this.lureSpeed = Math.max(0, lureSpeed);
    }

    public ProjectFishingHook(ChibiEntity chibi, Level level, Vec3 pos, Vec3 velocity) {
        this(ModEntities.FISHING_HOOK.get(), level, 0, 0);
        this.setOwner(chibi);
        this.setPos(pos.x, pos.y, pos.z);
        this.setDeltaMovement(velocity);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_BITING, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (DATA_BITING.equals(key)) {
            this.biting = this.getEntityData().get(DATA_BITING);
            if (this.biting) {
                this.setDeltaMovement(
                        this.getDeltaMovement().x,
                        -0.4D * Mth.nextFloat(this.synchronizedRandom, 0.6F, 1.0F),
                        this.getDeltaMovement().z
                );
            }
        }
        super.onSyncedDataUpdated(key);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 64.0D * 64.0D;
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        // Match TLM's MaidFishingHook: this entity simulates its own motion
        // client-side, so vanilla network interpolation would fight that motion.
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        Entity owner = getOwner();
        return new ClientboundAddEntityPacket(
                this,
                serverEntity,
                owner == null ? this.getId() : owner.getId()
        );
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        if (getChibiOwner() == null) {
            discard();
        }
    }

    @Override
    public void tick() {
        super.tick();

        ChibiEntity chibi = getChibiOwner();
        if (chibi == null || !chibi.isAlive() || chibi.isRemoved()) {
            discard();
            return;
        }

        if (chibi.distanceToSqr(this) > 256.0D) {
            discard();
            return;
        }

        if (level().isClientSide()) {
            fishingTick();
            return;
        }

        if (!isHoldingRod(chibi)) {
            discard();
            return;
        }

        synchronizedRandom.setSeed(getUUID().getLeastSignificantBits() ^ level().getGameTime());

        if (lifeTick()) {
            return;
        }

        chibi.getLookControl().setLookAt(this);
        fishingTick();
    }

    private boolean lifeTick() {
        if (onGround()) {
            ++life;
            if (life >= 100) {
                discard();
                return true;
            }
        } else {
            life = 0;
        }
        return false;
    }

    private void fishingTick() {
        BlockPos blockPos = blockPosition();
        FluidState fluidState = level().getFluidState(blockPos);
        float fluidHeight = getFluidHeight(fluidState, blockPos);
        boolean onWaterSurface = fluidHeight > 0;

        if (currentState == HookState.FLYING) {
            if (onWaterSurface) {
                waterSurfaceTick();
                return;
            }
        } else if (currentState == HookState.BOBBING) {
            bobbingTick(blockPos, fluidHeight);
            checkOpenWater(blockPos);

            if (onWaterSurface) {
                bitingTick(blockPos);
            } else {
                outOfWaterTime = Math.min(MAX_OUT_OF_WATER_TIME, outOfWaterTime + 1);
            }
        }

        if (!fluidState.is(FluidTags.WATER)) {
            setDeltaMovement(getDeltaMovement().add(0.0D, -0.03D, 0.0D));
        }

        move(MoverType.SELF, getDeltaMovement());
        updateRotation();

        if (currentState == HookState.FLYING && (onGround() || horizontalCollision)) {
            setDeltaMovement(Vec3.ZERO);
        }

        setDeltaMovement(getDeltaMovement().scale(0.92D));
        reapplyPosition();
    }

    private float getFluidHeight(FluidState fluidState, BlockPos pos) {
        return fluidState.is(FluidTags.WATER)
                ? fluidState.getHeight(level(), pos)
                : 0.0F;
    }

    private void waterSurfaceTick() {
        setDeltaMovement(getDeltaMovement().multiply(0.3D, 0.2D, 0.3D));
        currentState = HookState.BOBBING;
    }

    private void bobbingTick(BlockPos pos, float fluidHeight) {
        Vec3 movement = getDeltaMovement();
        double bobbingY = getY() + movement.y - pos.getY() - fluidHeight;

        if (Math.abs(bobbingY) < 0.01D) {
            bobbingY += Math.signum(bobbingY) * 0.1D;
        }

        setDeltaMovement(
                movement.x * 0.9D,
                movement.y - bobbingY * random.nextFloat() * 0.2D,
                movement.z * 0.9D
        );
    }

    private void bitingTick(BlockPos pos) {
        outOfWaterTime = Math.max(0, outOfWaterTime - 1);

        if (biting) {
            setDeltaMovement(getDeltaMovement().add(
                    0.0D,
                    -0.1D * synchronizedRandom.nextFloat() * synchronizedRandom.nextFloat(),
                    0.0D
            ));
        }

        if (!level().isClientSide() && level() instanceof ServerLevel serverLevel) {
            catchingFish(pos, serverLevel);
        }
    }

    private void catchingFish(BlockPos pos, ServerLevel level) {
        int time = 1;
        BlockPos abovePos = pos.above();

        if (random.nextFloat() < 0.25F && level.isRainingAt(abovePos)) {
            ++time;
        }

        if (random.nextFloat() < 0.5F && !level.canSeeSky(abovePos)) {
            --time;
        }

        if (nibble > 0) {
            --nibble;
            onNibble(level);
        } else if (timeUntilHooked > 0) {
            timeUntilHooked -= time;

            if (timeUntilHooked > 0) {
                fishAngle += (float) random.triangle(0.0D, 9.188D);
                float angle = fishAngle * ((float) Math.PI / 180F);
                float sin = Mth.sin(angle);
                float cos = Mth.cos(angle);

                double x = getX() + sin * timeUntilHooked * 0.1D;
                double y = Mth.floor(getY()) + 1.0D;
                double z = getZ() + cos * timeUntilHooked * 0.1D;

                BlockState state = level.getBlockState(BlockPos.containing(x, y - 1.0D, z));
                spawnFishingParticle(level, state, x, y, z, sin, cos);
            } else {
                spawnNibbleParticle(level);
                nibble = Mth.nextInt(random, 20, 40);
                entityData.set(DATA_BITING, true);
            }
        } else if (timeUntilLured > 0) {
            timeUntilLured -= time;

            float probability = 0.15F;
            if (timeUntilLured < 20) {
                probability += (20 - timeUntilLured) * 0.05F;
            } else if (timeUntilLured < 40) {
                probability += (40 - timeUntilLured) * 0.02F;
            } else if (timeUntilLured < 60) {
                probability += (60 - timeUntilLured) * 0.01F;
            }

            if (random.nextFloat() < probability) {
                float rot = Mth.nextFloat(random, 0.0F, 360.0F) * ((float) Math.PI / 180F);
                float amount = Mth.nextFloat(random, 25.0F, 60.0F);

                double x = getX() + Mth.sin(rot) * amount * 0.1D;
                double y = Mth.floor(getY()) + 1.0D;
                double z = getZ() + Mth.cos(rot) * amount * 0.1D;

                BlockState state = level.getBlockState(BlockPos.containing(x, y - 1.0D, z));
                spawnSplashParticle(level, state, x, y, z);
            }

            if (timeUntilLured <= 0) {
                fishAngle = Mth.nextFloat(random, 0.0F, 360.0F);
                timeUntilHooked = Mth.nextInt(random, 20, 80);
            }
        } else {
            timeUntilLured = Mth.nextInt(random, 100, 600);
            timeUntilLured -= lureSpeed * 20 * 5;
        }
    }

    private void spawnSplashParticle(ServerLevel level, BlockState state, double x, double y, double z) {
        if (state.is(Blocks.WATER)) {
            level.sendParticles(ParticleTypes.SPLASH, x, y, z,
                    2 + random.nextInt(2), 0.1F, 0.0D, 0.1F, 0.0D);
        }
    }

    private void spawnNibbleParticle(ServerLevel level) {
        double y = getY() + 0.5D;
        float width = getBbWidth();

        playSound(SoundEvents.FISHING_BOBBER_SPLASH, 0.25F,
                1.0F + (random.nextFloat() - random.nextFloat()) * 0.4F);

        level.sendParticles(ParticleTypes.BUBBLE, getX(), y, getZ(),
                (int) (1.0F + width * 20.0F), width, 0.0D, width, 0.2F);

        level.sendParticles(ParticleTypes.FISHING, getX(), y, getZ(),
                (int) (1.0F + width * 20.0F), width, 0.0D, width, 0.2F);
    }

    private void spawnFishingParticle(ServerLevel level, BlockState state,
                                      double x, double y, double z, float sin, float cos) {
        if (state.is(Blocks.WATER)) {
            if (random.nextFloat() < 0.15F) {
                level.sendParticles(ParticleTypes.BUBBLE, x, y - 0.1D, z,
                        1, sin, 0.1D, cos, 0.0D);
            }

            float sinOffset = sin * 0.04F;
            float cosOffset = cos * 0.04F;

            level.sendParticles(ParticleTypes.FISHING, x, y, z,
                    0, cosOffset, 0.01D, -sinOffset, 1.0D);
            level.sendParticles(ParticleTypes.FISHING, x, y, z,
                    0, -cosOffset, 0.01D, sinOffset, 1.0D);
        }
    }

    private void onNibble(ServerLevel level) {
        ChibiEntity chibi = getChibiOwner();
        if (chibi == null || nibble <= 0) {
            return;
        }

        int retrieveTime = Mth.nextInt(random, 2, 10);

        // Same basic auto-retrieve behaviour as Touhou Little Maid.
        if (nibble <= retrieveTime) {
            ItemStack rod = chibi.getMainHandItem();
            retrieve(rod);

            chibi.setFishingActive(false);
            chibi.swing(InteractionHand.MAIN_HAND);
            level.playSound(
                    null,
                    chibi.getX(), chibi.getY(), chibi.getZ(),
                    SoundEvents.FISHING_BOBBER_RETRIEVE,
                    SoundSource.NEUTRAL,
                    1.0F,
                    0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F)
            );
        }
    }

    public int retrieve(ItemStack rod) {
        ChibiEntity chibi = getChibiOwner();

        if (chibi == null || level().isClientSide() || !isHoldingRod(chibi)) {
            return 0;
        }

        int rodDamage = 0;

        if (nibble > 0 && level() instanceof ServerLevel serverLevel) {
            LootParams params = new LootParams.Builder(serverLevel)
                    .withParameter(LootContextParams.ORIGIN, position())
                    .withParameter(LootContextParams.TOOL, rod)
                    .withParameter(LootContextParams.THIS_ENTITY, this)
                    .withLuck(luck)
                    .create(LootContextParamSets.FISHING);

            LootTable table = serverLevel.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
            List<ItemStack> loot = table.getRandomItems(params);

            for (ItemStack stack : loot) {
                if (!insertIntoChibiInventory(chibi, stack.copy())) {
                    ItemEntity item = new ItemEntity(
                            serverLevel,
                            chibi.getX(),
                            chibi.getY() + 0.5D,
                            chibi.getZ(),
                            stack.copy()
                    );
                    item.setDeltaMovement(0.0D, 0.1D, 0.0D);
                    serverLevel.addFreshEntity(item);
                }
            }

            rodDamage = 1;
        }

        if (onGround()) {
            rodDamage = 2;
        }

        if (rodDamage > 0 && !rod.isEmpty()) {
            rod.hurtAndBreak(rodDamage, chibi, EquipmentSlot.MAINHAND);
        }

        discard();
        chibi.setFishingActive(false);
        return rodDamage;
    }

    private boolean insertIntoChibiInventory(ChibiEntity chibi, ItemStack stack) {
        ChibiInventory inv = chibi.getChibiInventory();

        for (int i = ChibiInventory.STORAGE_START;
             i < ChibiInventory.STORAGE_START + ChibiInventory.STORAGE_SIZE; i++) {

            ItemStack existing = inv.getItem(i);

            if (!existing.isEmpty()
                    && ItemStack.isSameItemSameComponents(existing, stack)
                    && existing.getCount() < existing.getMaxStackSize()) {

                int move = Math.min(
                        existing.getMaxStackSize() - existing.getCount(),
                        stack.getCount()
                );

                existing.grow(move);
                stack.shrink(move);

                if (stack.isEmpty()) {
                    inv.setChanged();
                    return true;
                }
            }
        }

        for (int i = ChibiInventory.STORAGE_START;
             i < ChibiInventory.STORAGE_START + ChibiInventory.STORAGE_SIZE; i++) {

            if (inv.getItem(i).isEmpty()) {
                inv.setItem(i, stack);
                inv.setChanged();
                return true;
            }
        }

        inv.setChanged();
        return stack.isEmpty();
    }

    private boolean isHoldingRod(ChibiEntity chibi) {
        return chibi.getMainHandItem().canPerformAction(
                net.minecraftforge.common.ToolActions.FISHING_ROD_CAST
        );
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        ChibiEntity chibi = getChibiOwner();
        if (chibi != null) {
            chibi.setFishingActive(false);
        }
        super.remove(reason);
    }

    @Nullable
    public ChibiEntity getChibiOwner() {
        Entity owner = getOwner();
        return owner instanceof ChibiEntity chibi ? chibi : null;
    }

    private void checkOpenWater(BlockPos pos) {
        if (nibble <= 0 && timeUntilHooked <= 0) {
            openWater = true;
        } else {
            openWater = openWater && outOfWaterTime < 10 && calculateOpenWater(pos);
        }
    }

    private boolean calculateOpenWater(BlockPos pos) {
        OpenWaterType type = OpenWaterType.INVALID;

        for (int y = -1; y <= 2; y++) {
            OpenWaterType area = getOpenWaterTypeForArea(
                    pos.offset(-2, y, -2),
                    pos.offset(2, y, 2)
            );

            switch (area) {
                case INVALID -> { return false; }
                case ABOVE_WATER -> {
                    if (type == OpenWaterType.INVALID) return false;
                }
                case INSIDE_WATER -> {
                    if (type == OpenWaterType.ABOVE_WATER) return false;
                }
            }

            type = area;
        }

        return true;
    }

    private OpenWaterType getOpenWaterTypeForArea(BlockPos first, BlockPos second) {
        return BlockPos.betweenClosedStream(first, second)
                .map(this::getOpenWaterTypeForBlock)
                .reduce((a, b) -> a == b ? a : OpenWaterType.INVALID)
                .orElse(OpenWaterType.INVALID);
    }

    private OpenWaterType getOpenWaterTypeForBlock(BlockPos pos) {
        BlockState state = level().getBlockState(pos);

        if (!state.isAir() && !state.is(Blocks.LILY_PAD)) {
            FluidState fluid = state.getFluidState();

            return fluid.is(FluidTags.WATER)
                    && fluid.isSource()
                    && state.getCollisionShape(level(), pos).isEmpty()
                    ? OpenWaterType.INSIDE_WATER
                    : OpenWaterType.INVALID;
        }

        return OpenWaterType.ABOVE_WATER;
    }

    public boolean isOpenWaterFishing() {
        return openWater;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    private enum HookState {
        FLYING,
        BOBBING
    }

    private enum OpenWaterType {
        ABOVE_WATER,
        INSIDE_WATER,
        INVALID
    }
}

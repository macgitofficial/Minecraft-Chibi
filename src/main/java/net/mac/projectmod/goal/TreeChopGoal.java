package net.mac.projectmod.goal;

import net.mac.projectmod.entity.ChibiEntity;
import net.mac.projectmod.memory.BlockValidationMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * Tree chopping FSM for Forge 1.20.1.
 *
 * Main flow:
 * SEARCH_TREE -> GO_TO_TREE -> CUT_LEVEL
 * CUT_LEVEL -> MOVE_AROUND / CLIMB / CLEAR_OBSTACLE
 * CLIMB -> CUT_LEVEL
 * last level -> FINISH_TREE -> SEARCH_TREE
 *
 * A selected tree remains locked until every connected log is removed.
 */
public class TreeChopGoal extends Goal {

    private static final int SEARCH_RADIUS = 16;
    private static final int TREE_SCAN_HEIGHT = 24;

    /** Maximum distance from Chibi to the log center for chopping. */
    private static final double REACH_DISTANCE = 4.0D;

    /** Maximum distance to a stand position before we consider ourselves there. */
    private static final double STAND_DISTANCE = 1.7D;

    /** Search area used when trying to find a walkable position. */
    private static final int STAND_SEARCH_RADIUS = 3;

    /** Working area around Chibi. */
    private static final int CLEAR_RADIUS = 2;

    /** Maximum number of connected logs belonging to one target. */
    private static final int MAX_TREE_LOGS = 256;

    /** How many ticks we wait before trying another movement strategy. */
    private static final int MOVE_TIMEOUT = 50;

    private enum State {
        SEARCH_TREE,
        GO_TO_TREE,
        CUT_LEVEL,
        MOVE_AROUND,
        CLIMB,
        CLEAR_OBSTACLE,
        FINISH_TREE
    }

    private final ChibiEntity chibi;

    private State state = State.SEARCH_TREE;

    /** Locked logs of the current tree. */
    private final Set<BlockPos> treeLogs = new HashSet<>();

    /** Remaining logs grouped by Y. */
    private final Map<Integer, List<BlockPos>> levels = new TreeMap<>();

    private BlockPos treeRoot;
    private BlockPos currentLog;
    private int currentY = Integer.MIN_VALUE;

    private int actionCooldown;
    private int stuckTicks;

    protected boolean isValidNatureTree(ChibiEntity chibi, BlockPos startPos) {
        HashSet<BlockPos> vis = new HashSet<>();
        BlockValidationMemory validationMemory = null; //= MemoryUtil.getBlockValidationMemory(chibi)
        boolean validNatureTree = isValidNatureTree(chibi, startPos, vis, 0, validationMemory);
        for (BlockPos pos : vis) {
            BlockState blockState = chibi.level().getBlockState(pos);
            if (!blockState.is(BlockTags.LEAVES) && !blockState.is(BlockTags.LOGS))
                continue;
            if (validNatureTree)
                validationMemory.setValid(pos);
            else
                validationMemory.setInvalid(pos);
        }
        return validNatureTree;
    }

    protected boolean isValidNatureTree(ChibiEntity maid, BlockPos startPos, Set<BlockPos> visited, int depth, BlockValidationMemory validationMemory) {
        if (validationMemory.hasRecord(startPos))
            return validationMemory.isValid(startPos, false);
        if (visited.contains(startPos))
            return false;
        if (depth > 100) return false;
        visited.add(startPos);
        boolean valid = false;
        final int[] dv = {0, 1, -1};
        for (int dx : dv) {
            for (int dz : dv) {
                for (int dy : dv) {
                    BlockPos offset = startPos.offset(dx, dy, dz);
                    BlockState blockState = maid.level().getBlockState(offset);
                    if (blockState.is(BlockTags.LEAVES) && blockState.hasProperty(LeavesBlock.PERSISTENT) && !blockState.getValue(LeavesBlock.PERSISTENT)) {
                        valid = true;
                    }
                    if (blockState.is(BlockTags.LOGS) && isValidNatureTree(maid, offset, visited, depth + 1, validationMemory)) {
                        valid = true;
                    }
                }
            }
        }
        if (valid)
            validationMemory.setValid(startPos);
        else
            validationMemory.setInvalid(startPos);
        return valid;
    }



    /**
     * When climbing, this is the Y level we are trying to reach.
     * It prevents the FSM from immediately selecting another old level.
     */
    private int targetClimbY = Integer.MIN_VALUE;

    public TreeChopGoal(ChibiEntity chibi) {
        this.chibi = chibi;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (chibi.level().isClientSide()) {
            return false;
        }

        if (chibi.isOrderedToSit()) {
            return false;
        }

        return findAndLockTree();
    }

    @Override
    public boolean canContinueToUse() {
        if (chibi.level().isClientSide() || chibi.isOrderedToSit()) {
            return false;
        }

        // Keep the goal alive while a tree is locked.
        return !treeLogs.isEmpty() || state != State.SEARCH_TREE;
    }

    @Override
    public void start() {
        state = State.GO_TO_TREE;
        actionCooldown = 0;
        stuckTicks = 0;
    }

    @Override
    public void stop() {
        chibi.getNavigation().stop();

        treeRoot = null;
        currentLog = null;
        treeLogs.clear();
        levels.clear();

        currentY = Integer.MIN_VALUE;
        targetClimbY = Integer.MIN_VALUE;

        state = State.SEARCH_TREE;
        actionCooldown = 0;
        stuckTicks = 0;
    }

    @Override
    public void tick() {
        if (!(chibi.level() instanceof ServerLevel level)) {
            return;
        }

        if (actionCooldown > 0) {
            actionCooldown--;
        }

        pruneDestroyedLogs(level);

        if (treeLogs.isEmpty() && state != State.SEARCH_TREE && state != State.FINISH_TREE) {
            state = State.SEARCH_TREE;
        }

        switch (state) {
            case SEARCH_TREE -> tickSearchTree(level);
            case GO_TO_TREE -> tickGoToTree(level);
            case CUT_LEVEL -> tickCutLevel(level);
            case MOVE_AROUND -> tickMoveAround(level);
            case CLIMB -> tickClimb(level);
            case CLEAR_OBSTACLE -> tickClearObstacle(level);
            case FINISH_TREE -> finishTree(level);
        }
    }

    /* ============================================================
       SEARCH
       ============================================================ */

    private void tickSearchTree(ServerLevel level) {
        if (findAndLockTree()) {
            state = State.GO_TO_TREE;
        }
    }

    private boolean findAndLockTree() {
        if (!(chibi.level() instanceof ServerLevel level)) {
            return false;
        }

        BlockPos origin = chibi.blockPosition();

        BlockPos bestRoot = null;
        int bestDistance = Integer.MAX_VALUE;

        /*
         * Prefer logs on Chibi's current Y first.
         * Then search progressively above/below.
         */
        List<Integer> yOffsets = new ArrayList<>();
        yOffsets.add(0);

        for (int d = 1; d <= TREE_SCAN_HEIGHT; d++) {
            yOffsets.add(d);
            yOffsets.add(-d);
        }

        for (int dy : yOffsets) {
            for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
                for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                    if (dx * dx + dz * dz > SEARCH_RADIUS * SEARCH_RADIUS) {
                        continue;
                    }

                    BlockPos pos = origin.offset(dx, dy, dz);

                    if (!isLog(level.getBlockState(pos))) {
                        continue;
                    }

                    if (!hasLeavesAroundTree(level, pos)) {
                        continue;
                    }

                    int distance = origin.distManhattan(pos);

                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestRoot = pos;
                    }
                }
            }

            // Same-Y tree wins over a tree on another layer.
            if (bestRoot != null && dy == 0) {
                break;
            }
        }

        return bestRoot != null && lockTree(level, bestRoot);
    }

    private boolean lockTree(ServerLevel level, BlockPos seed) {
        Set<BlockPos> connected = collectTreeLogs(level, seed);

        if (connected.isEmpty()) {
            return false;
        }

        /*
         * Reject an isolated leftover log.
         * The leaves check is performed against the selected connected
         * structure, not merely against an arbitrary nearby log.
         */
        boolean hasLeaves = connected.stream()
                .anyMatch(pos -> hasLeavesAroundTree(level, pos));

        if (!hasLeaves) {
            return false;
        }

        treeRoot = seed;

        treeLogs.clear();
        treeLogs.addAll(connected);

        rebuildLevels();

        currentY = getLowestY();
        currentLog = null;

        targetClimbY = Integer.MIN_VALUE;
        stuckTicks = 0;

        return !treeLogs.isEmpty();
    }

    private Set<BlockPos> collectTreeLogs(ServerLevel level, BlockPos seed) {
        Set<BlockPos> result = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();

        result.add(seed);
        queue.add(seed);

        while (!queue.isEmpty() && result.size() < MAX_TREE_LOGS) {
            BlockPos pos = queue.poll();

            for (BlockPos next : sixNeighbors(pos)) {
                if (result.contains(next)) {
                    continue;
                }

                if (isLog(level.getBlockState(next))) {
                    result.add(next);
                    queue.add(next);
                }
            }
        }

        return result;
    }

    private boolean hasLeavesAroundTree(ServerLevel level, BlockPos center) {
        final int radius = 4;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }

                    if (isLeaves(level.getBlockState(center.offset(dx, dy, dz)))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /* ============================================================
       GO TO TREE
       ============================================================ */

    private void tickGoToTree(ServerLevel level) {
        if (treeLogs.isEmpty()) {
            state = State.SEARCH_TREE;
            return;
        }

        int lowestY = getLowestY();

        if (lowestY == Integer.MIN_VALUE) {
            state = State.FINISH_TREE;
            return;
        }

        currentY = lowestY;

        currentLog = chooseNearestLog(level, levels.get(currentY));

        if (currentLog == null) {
            removeEmptyLevel(currentY);

            if (levels.isEmpty()) {
                state = State.FINISH_TREE;
            }

            return;
        }

        if (canReach(currentLog)) {
            state = State.CUT_LEVEL;
            stuckTicks = 0;
            return;
        }

        BlockPos stand = findStandPosition(level, currentLog);

        if (stand == null) {
            state = State.CLEAR_OBSTACLE;
            return;
        }

        moveTo(stand, currentLog);

        if (isCloseEnough(stand)) {
            state = State.CUT_LEVEL;
            stuckTicks = 0;
            return;
        }

        stuckTicks++;

        if (stuckTicks > MOVE_TIMEOUT) {
            stuckTicks = 0;
            state = State.MOVE_AROUND;
        }
    }

    /* ============================================================
       CUT ONE Y LEVEL AT A TIME
       ============================================================ */

    private void tickCutLevel(ServerLevel level) {
        if (treeLogs.isEmpty()) {
            state = State.FINISH_TREE;
            return;
        }

        /*
         * IMPORTANT:
         * Always work on the lowest remaining Y.
         * We never move to a higher level until this entire level is gone.
         */
        if (currentY == Integer.MIN_VALUE || !levels.containsKey(currentY)) {
            currentY = getLowestY();
        }

        if (currentY == Integer.MIN_VALUE) {
            state = State.FINISH_TREE;
            return;
        }

        List<BlockPos> levelLogs = levels.get(currentY);

        if (levelLogs == null || levelLogs.isEmpty()) {
            removeEmptyLevel(currentY);

            if (levels.isEmpty()) {
                state = State.FINISH_TREE;
            } else {
                prepareNextLevel();
            }

            return;
        }

        currentLog = chooseNearestLog(level, levelLogs);

        if (currentLog == null) {
            removeEmptyLevel(currentY);

            if (levels.isEmpty()) {
                state = State.FINISH_TREE;
            } else {
                prepareNextLevel();
            }

            return;
        }

        /*
         * If the current log is too high/far, don't just keep trying to
         * navigate horizontally. First attempt to get a reachable
         * position; if that is impossible, climb.
         */
        if (!canReach(currentLog)) {
            BlockPos stand = findStandPosition(level, currentLog);

            if (stand != null) {
                moveTo(stand, currentLog);

                if (isCloseEnough(stand)) {
                    stuckTicks = 0;
                } else {
                    stuckTicks++;

                    if (stuckTicks > MOVE_TIMEOUT) {
                        stuckTicks = 0;
                        state = State.CLIMB;
                        targetClimbY = currentLog.getY();
                    }
                }

                return;
            }

            state = State.CLIMB;
            targetClimbY = currentLog.getY();
            return;
        }

        stuckTicks = 0;

        if (actionCooldown > 0) {
            return;
        }

        chibi.getLookControl().setLookAt(
                currentLog.getX() + 0.5D,
                currentLog.getY() + 0.5D,
                currentLog.getZ() + 0.5D
        );

        breakBlock(level, currentLog);
        actionCooldown = 4;

        /*
         * If this was the final log of this Y level, prepare climbing
         * BEFORE selecting another log.
         */
        if (!hasLogsAtY(currentY)) {
            if (levels.isEmpty()) {
                state = State.FINISH_TREE;
            } else {
                prepareNextLevel();
            }
        }
    }

    private void prepareNextLevel() {
        int nextY = getNextHigherY(currentY);

        if (nextY == Integer.MIN_VALUE) {
            state = State.FINISH_TREE;
            return;
        }

        currentY = nextY;
        currentLog = chooseNearestLog(
                (ServerLevel) chibi.level(),
                levels.get(currentY)
        );

        if (currentLog == null) {
            removeEmptyLevel(currentY);

            if (levels.isEmpty()) {
                state = State.FINISH_TREE;
            }

            return;
        }

        /*
         * This is the key fix:
         * after finishing one Y layer, explicitly enter CLIMB when the
         * next layer is not reachable.
         */
        if (!canReachAnyLogOnLevel(currentY)) {
            targetClimbY = currentY;
            state = State.CLIMB;
        } else {
            targetClimbY = Integer.MIN_VALUE;
            state = State.CUT_LEVEL;
        }

        stuckTicks = 0;
    }

    private int getNextHigherY(int y) {
        return levels.keySet().stream()
                .filter(next -> next > y)
                .min(Integer::compareTo)
                .orElse(Integer.MIN_VALUE);
    }

    private boolean hasLogsAtY(int y) {
        List<BlockPos> list = levels.get(y);
        return list != null && !list.isEmpty();
    }

    private boolean canReachAnyLogOnLevel(int y) {
        List<BlockPos> list = levels.get(y);

        if (list == null) {
            return false;
        }

        for (BlockPos pos : list) {
            if (canReach(pos)) {
                return true;
            }
        }

        return false;
    }

    private void removeEmptyLevel(int y) {
        levels.remove(y);

        if (currentY == y) {
            currentY = getLowestY();
        }
    }

    /* ============================================================
       CLIMB
       ============================================================ */

    private void tickClimb(ServerLevel level) {
        if (treeLogs.isEmpty()) {
            state = State.FINISH_TREE;
            return;
        }

        if (targetClimbY == Integer.MIN_VALUE) {
            if (currentLog != null) {
                targetClimbY = currentLog.getY();
            } else {
                targetClimbY = currentY;
            }
        }

        /*
         * If any log on the target level becomes reachable, stop climbing
         * and let CUT_LEVEL handle the entire level.
         */
        if (canReachAnyLogOnLevel(targetClimbY)) {
            currentY = targetClimbY;
            currentLog = chooseNearestReachableLog(level, levels.get(currentY));

            if (currentLog != null) {
                targetClimbY = Integer.MIN_VALUE;
                stuckTicks = 0;
                state = State.CUT_LEVEL;
                return;
            }
        }

        BlockPos feet = chibi.blockPosition();

        /*
         * If the block immediately above Chibi blocks the climb, clear it.
         */
        if (!isPassable(level.getBlockState(feet.above()))
                || !isPassable(level.getBlockState(feet.above(2)))) {
            state = State.CLEAR_OBSTACLE;
            return;
        }

        /*
         * First try to move toward the target tree while climbing.
         * This keeps the climb attached to the tree instead of building
         * a tower in a random direction.
         */
        BlockPos guide = findNearestLogOnLevel(level, targetClimbY);

        if (guide != null) {
            BlockPos sideStand = findStandPosition(level, guide);

            if (sideStand != null
                    && sideStand.getY() <= feet.getY() + 1
                    && !isSameBlockXZ(sideStand, feet)) {

                chibi.getLookControl().setLookAt(
                        guide.getX() + 0.5D,
                        guide.getY() + 0.5D,
                        guide.getZ() + 0.5D
                );

                chibi.getNavigation().moveTo(
                        sideStand.getX() + 0.5D,
                        sideStand.getY(),
                        sideStand.getZ() + 0.5D,
                        1.0D
                );

                if (isCloseEnough(sideStand)) {
                    stuckTicks = 0;
                } else {
                    stuckTicks++;

                    if (stuckTicks > 20) {
                        stuckTicks = 0;
                    }
                }
            }
        }

        /*
         * Core climbing mechanic:
         *
         *   feet block = current position
         *   support   = block directly BELOW feet
         *
         * To climb one block:
         *   1. Place a solid block at feet.below()
         *   2. Move Chibi upward by one block using normal entity movement.
         *
         * We do NOT teleport by setPos anymore.
         */
        BlockPos supportPos = feet.below();

        if (canPlaceSupport(level, supportPos)) {
            if (placeSupportBlock(level, supportPos)) {
                /*
                 * Move one block upward only after the support block was
                 * actually placed.
                 */
                chibi.getNavigation().stop();

                chibi.setPos(
                        chibi.getX(),
                        chibi.getY() + 1.0D,
                        chibi.getZ()
                );

                chibi.resetFallDistance();

                actionCooldown = 5;
                stuckTicks = 0;
                return;
            }
        }

        /*
         * We cannot build directly under the current position.
         * Try to make a side route / clear an obstruction first.
         */
        state = State.MOVE_AROUND;
    }

    /* ============================================================
       MOVE AROUND
       ============================================================ */

    private void tickMoveAround(ServerLevel level) {
        if (treeLogs.isEmpty()) {
            state = State.FINISH_TREE;
            return;
        }

        if (currentY == Integer.MIN_VALUE) {
            currentY = getLowestY();
        }

        /*
         * If a target on the current level is reachable, immediately return
         * to chopping.
         */
        if (canReachAnyLogOnLevel(currentY)) {
            currentLog = chooseNearestReachableLog(level, levels.get(currentY));

            if (currentLog != null) {
                state = State.CUT_LEVEL;
                stuckTicks = 0;
                return;
            }
        }

        /*
         * Find a nearby stand position around the target.
         */
        BlockPos guide = findNearestLogOnLevel(level, currentY);

        if (guide == null) {
            prepareNextLevel();
            return;
        }

        BlockPos stand = findStandPosition(level, guide);

        if (stand != null) {
            moveTo(stand, guide);

            if (isCloseEnough(stand)) {
                state = State.CUT_LEVEL;
                stuckTicks = 0;
                return;
            }

            stuckTicks++;

            if (stuckTicks > MOVE_TIMEOUT) {
                stuckTicks = 0;
                state = State.CLIMB;
                targetClimbY = currentY;
            }

            return;
        }

        /*
         * No horizontal route:
         * clear an actual obstruction, then try again.
         */
        state = State.CLEAR_OBSTACLE;
    }

    /* ============================================================
       CLEAR OBSTACLE
       ============================================================ */

    private void tickClearObstacle(ServerLevel level) {
        BlockPos center = chibi.blockPosition();

        BlockPos obstacle = findBlockingBlock(level, center);

        if (obstacle == null) {
            if (targetClimbY != Integer.MIN_VALUE) {
                state = State.CLIMB;
            } else {
                state = State.MOVE_AROUND;
            }

            return;
        }

        if (actionCooldown > 0) {
            return;
        }

        chibi.getLookControl().setLookAt(
                obstacle.getX() + 0.5D,
                obstacle.getY() + 0.5D,
                obstacle.getZ() + 0.5D
        );

        breakBlock(level, obstacle);
        actionCooldown = 4;
    }

    /**
     * Find the nearest collision block that is actually useful to clear.
     *
     * We avoid:
     * - air
     * - leaves
     * - the currently targeted log
     * - the block supporting Chibi
     */
    private BlockPos findBlockingBlock(ServerLevel level, BlockPos center) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int dx = -CLEAR_RADIUS; dx <= CLEAR_RADIUS; dx++) {
            for (int dy = 0; dy <= CLEAR_RADIUS; dy++) {
                for (int dz = -CLEAR_RADIUS; dz <= CLEAR_RADIUS; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }

                    BlockPos pos = center.offset(dx, dy, dz);

                    if (currentLog != null && pos.equals(currentLog)) {
                        continue;
                    }

                    if (pos.equals(center.below())) {
                        continue;
                    }

                    BlockState state = level.getBlockState(pos);

                    if (state.isAir() || isLeaves(state)) {
                        continue;
                    }

                    if (state.getCollisionShape(level, pos).isEmpty()) {
                        continue;
                    }

                    /*
                     * Only clear blocks in/near the body and head space.
                     * Do not randomly mine the ground around Chibi.
                     */
                    double d = pos.distSqr(center);

                    if (d < bestDistance) {
                        bestDistance = d;
                        best = pos;
                    }
                }
            }
        }

        return best;
    }

    /* ============================================================
       FINISH
       ============================================================ */

    private void finishTree(ServerLevel level) {
        pruneDestroyedLogs(level);

        if (!treeLogs.isEmpty()) {
            rebuildLevels();

            currentY = getLowestY();

            if (currentY == Integer.MIN_VALUE) {
                treeLogs.clear();
                levels.clear();
            } else {
                state = State.CUT_LEVEL;
                return;
            }
        }

        /*
         * The current tree is completely gone.
         * Only NOW may we search for another tree.
         */
        treeRoot = null;
        currentLog = null;
        levels.clear();
        currentY = Integer.MIN_VALUE;
        targetClimbY = Integer.MIN_VALUE;

        if (findAndLockTree()) {
            state = State.GO_TO_TREE;
        } else {
            state = State.SEARCH_TREE;
        }
    }

    /* ============================================================
       BREAKING
       ============================================================ */

    private void breakBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (state.isAir()) {
            treeLogs.remove(pos);
            removeLogFromLevel(pos);
            return;
        }

        chibi.triggerAnim("main_controller", "swing_hand");

        /*
         * Keep the fastest-tool decision available for later real mining
         * timing. Instant destroy is still used here so the FSM remains
         * deterministic.
         */
        chooseFastestTool(state);

        level.destroyBlock(pos, true);

        treeLogs.remove(pos);
        removeLogFromLevel(pos);
    }

    private void removeLogFromLevel(BlockPos pos) {
        List<BlockPos> list = levels.get(pos.getY());

        if (list == null) {
            return;
        }

        list.remove(pos);

        if (list.isEmpty()) {
            levels.remove(pos.getY());
        }
    }

    private ItemStack chooseFastestTool(BlockState state) {
        ItemStack best = ItemStack.EMPTY;
        float bestSpeed = 0.0F;

        for (ItemStack stack : getAvailableTools()) {
            float speed = stack.getDestroySpeed(state);

            if (speed > bestSpeed) {
                bestSpeed = speed;
                best = stack;
            }
        }

        return best;
    }

    private List<ItemStack> getAvailableTools() {
        List<ItemStack> tools = new ArrayList<>();

        ItemStack main = chibi.getChibiEquipment(EquipmentSlot.MAINHAND);
        ItemStack off = chibi.getChibiEquipment(EquipmentSlot.OFFHAND);

        if (!main.isEmpty()) {
            tools.add(main);
        }

        if (!off.isEmpty()) {
            tools.add(off);
        }

        /*
         * Also inspect the Chibi inventory for axes.
         * This does not move the item into the hand yet; it only lets the
         * selection logic know which tools are available.
         */
        for (int i = 0; i < chibi.getChibiInventory().getContainerSize(); i++) {
            ItemStack stack = chibi.getChibiInventory().getItem(i);

            if (!stack.isEmpty()) {
                tools.add(stack);
            }
        }

        // Bare hand.
        tools.add(ItemStack.EMPTY);

        return tools;
    }

    /* ============================================================
       SUPPORT BLOCK
       ============================================================ */

    private boolean canPlaceSupport(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir()) {
            return false;
        }

        ItemStack support = findSupportBlock();

        if (support.isEmpty()) {
            return false;
        }

        Block block = Block.byItem(support.getItem());

        if (block == Blocks.AIR) {
            return false;
        }

        /*
         * There must be a solid surface below the support block.
         */
        return !level.getBlockState(pos.below())
                .getCollisionShape(level, pos.below())
                .isEmpty();
    }

    private boolean placeSupportBlock(ServerLevel level, BlockPos pos) {
        ItemStack support = findSupportBlock();

        if (support.isEmpty()) {
            return false;
        }

        Block block = Block.byItem(support.getItem());

        if (block == Blocks.AIR) {
            return false;
        }

        if (!level.getBlockState(pos).isAir()) {
            return false;
        }

        boolean placed = level.setBlock(
                pos,
                block.defaultBlockState(),
                3
        );

        if (!placed) {
            return false;
        }

        /*
         * Actually consume one block from the Chibi inventory.
         */
        consumeSupportBlock(support);

        return true;
    }

    private ItemStack findSupportBlock() {
        for (int i = 0; i < chibi.getChibiInventory().getContainerSize(); i++) {
            ItemStack stack = chibi.getChibiInventory().getItem(i);

            if (stack.isEmpty()) {
                continue;
            }

            Block block = Block.byItem(stack.getItem());

            if (block == Blocks.AIR) {
                continue;
            }

            if (block.defaultBlockState().getCollisionShape(
                    chibi.level(),
                    BlockPos.ZERO
            ).isEmpty()) {
                continue;
            }

            /*
             * Do not use logs/leaves as the building material.
             * Prefer ordinary solid blocks.
             */
            if (isLog(stack)) {
                continue;
            }

            return stack;
        }

        return ItemStack.EMPTY;
    }

    private void consumeSupportBlock(ItemStack selected) {
        for (int i = 0; i < chibi.getChibiInventory().getContainerSize(); i++) {
            ItemStack stack = chibi.getChibiInventory().getItem(i);

            if (stack.isEmpty()) {
                continue;
            }

            if (stack.getItem() == selected.getItem()) {
                stack.shrink(1);

                if (stack.isEmpty()) {
                    chibi.getChibiInventory().setItem(i, ItemStack.EMPTY);
                }

                return;
            }
        }
    }

    private boolean isLog(ItemStack stack) {
        Block block = Block.byItem(stack.getItem());

        return block == Blocks.OAK_LOG
                || block == Blocks.SPRUCE_LOG
                || block == Blocks.BIRCH_LOG
                || block == Blocks.JUNGLE_LOG
                || block == Blocks.ACACIA_LOG
                || block == Blocks.DARK_OAK_LOG
                || block == Blocks.MANGROVE_LOG
                || block == Blocks.CHERRY_LOG
                || block == Blocks.OAK_WOOD
                || block == Blocks.SPRUCE_WOOD
                || block == Blocks.BIRCH_WOOD
                || block == Blocks.JUNGLE_WOOD
                || block == Blocks.ACACIA_WOOD
                || block == Blocks.DARK_OAK_WOOD
                || block == Blocks.MANGROVE_WOOD
                || block == Blocks.CHERRY_WOOD;
    }

    /* ============================================================
       LEVELS / TARGETS
       ============================================================ */

    private void rebuildLevels() {
        levels.clear();

        for (BlockPos pos : treeLogs) {
            levels.computeIfAbsent(pos.getY(), y -> new ArrayList<>()).add(pos);
        }

        for (List<BlockPos> list : levels.values()) {
            list.sort(Comparator.comparingDouble(
                    pos -> pos.distSqr(chibi.blockPosition())
            ));
        }
    }

    private int getLowestY() {
        return treeLogs.stream()
                .mapToInt(BlockPos::getY)
                .min()
                .orElse(Integer.MIN_VALUE);
    }

    private BlockPos chooseNearestLog(ServerLevel level, List<BlockPos> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }

        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos pos : list) {
            if (!isLog(level.getBlockState(pos))) {
                continue;
            }

            double d = pos.distSqr(chibi.blockPosition());

            if (d < bestDistance) {
                bestDistance = d;
                best = pos;
            }
        }

        return best;
    }

    private BlockPos chooseNearestReachableLog(
            ServerLevel level,
            List<BlockPos> list
    ) {
        if (list == null) {
            return null;
        }

        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos pos : list) {
            if (!isLog(level.getBlockState(pos))) {
                continue;
            }

            if (!canReach(pos)) {
                continue;
            }

            double d = pos.distSqr(chibi.blockPosition());

            if (d < bestDistance) {
                bestDistance = d;
                best = pos;
            }
        }

        return best;
    }

    private BlockPos findNearestLogOnLevel(ServerLevel level, int y) {
        return chooseNearestLog(level, levels.get(y));
    }

    private void pruneDestroyedLogs(ServerLevel level) {
        Iterator<BlockPos> iterator = treeLogs.iterator();

        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();

            if (!isLog(level.getBlockState(pos))) {
                iterator.remove();
            }
        }

        rebuildLevels();

        if (currentY != Integer.MIN_VALUE && !levels.containsKey(currentY)) {
            currentY = getLowestY();
        }
    }

    /* ============================================================
       MOVEMENT / GEOMETRY
       ============================================================ */

    private boolean canReach(BlockPos pos) {
        return chibi.position().distanceToSqr(center(pos))
                <= REACH_DISTANCE * REACH_DISTANCE;
    }

    private boolean isCloseEnough(BlockPos pos) {
        return chibi.position().distanceToSqr(center(pos))
                <= STAND_DISTANCE * STAND_DISTANCE;
    }

    private void moveTo(BlockPos stand, BlockPos lookAt) {
        chibi.getLookControl().setLookAt(
                lookAt.getX() + 0.5D,
                lookAt.getY() + 0.5D,
                lookAt.getZ() + 0.5D
        );

        chibi.getNavigation().moveTo(
                stand.getX() + 0.5D,
                stand.getY(),
                stand.getZ() + 0.5D,
                1.0D
        );
    }

    private BlockPos findStandPosition(ServerLevel level, BlockPos target) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int dx = -STAND_SEARCH_RADIUS;
             dx <= STAND_SEARCH_RADIUS;
             dx++) {

            for (int dy = -1; dy <= 1; dy++) {

                for (int dz = -STAND_SEARCH_RADIUS;
                     dz <= STAND_SEARCH_RADIUS;
                     dz++) {

                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }

                    BlockPos stand = target.offset(dx, dy, dz);

                    if (!isPassable(level.getBlockState(stand))) {
                        continue;
                    }

                    if (!isPassable(level.getBlockState(stand.above()))) {
                        continue;
                    }

                    if (!hasSolidFloor(level, stand.below())) {
                        continue;
                    }

                    /*
                     * Don't select a stand position that is too far from
                     * the actual log.
                     */
                    if (stand.distSqr(target) > 16.0D) {
                        continue;
                    }

                    double d = stand.distSqr(chibi.blockPosition());

                    if (d < bestDistance) {
                        bestDistance = d;
                        best = stand;
                    }
                }
            }
        }

        return best;
    }

    private boolean hasSolidFloor(ServerLevel level, BlockPos pos) {
        return !level.getBlockState(pos)
                .getCollisionShape(level, pos)
                .isEmpty();
    }

    private boolean isPassable(BlockState state) {
        return state.isAir() || isLeaves(state);
    }

    private boolean isSameBlockXZ(BlockPos a, BlockPos b) {
        return a.getX() == b.getX() && a.getZ() == b.getZ();
    }

    /* ============================================================
       BLOCK TYPES
       ============================================================ */

    private boolean isLog(BlockState state) {
        Block block = state.getBlock();

        return block == Blocks.OAK_LOG
                || block == Blocks.SPRUCE_LOG
                || block == Blocks.BIRCH_LOG
                || block == Blocks.JUNGLE_LOG
                || block == Blocks.ACACIA_LOG
                || block == Blocks.DARK_OAK_LOG
                || block == Blocks.MANGROVE_LOG
                || block == Blocks.CHERRY_LOG
                || block == Blocks.OAK_WOOD
                || block == Blocks.SPRUCE_WOOD
                || block == Blocks.BIRCH_WOOD
                || block == Blocks.JUNGLE_WOOD
                || block == Blocks.ACACIA_WOOD
                || block == Blocks.DARK_OAK_WOOD
                || block == Blocks.MANGROVE_WOOD
                || block == Blocks.CHERRY_WOOD;
    }

    private boolean isLeaves(BlockState state) {
        return state.getBlock() instanceof LeavesBlock;
    }

    private List<BlockPos> sixNeighbors(BlockPos pos) {
        return List.of(
                pos.above(),
                pos.below(),
                pos.north(),
                pos.south(),
                pos.east(),
                pos.west()
        );
    }

    private net.minecraft.world.phys.Vec3 center(BlockPos pos) {
        return new net.minecraft.world.phys.Vec3(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D
        );
    }
}

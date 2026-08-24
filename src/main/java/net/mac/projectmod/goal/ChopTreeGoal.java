package net.mac.projectmod.goal;

import net.mac.projectmod.data.PlacedLogData;
import net.mac.projectmod.entity.ChibiEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class ChopTreeGoal extends Goal {

    private final ChibiEntity npc;

    private static final int SEARCH = 16;
    private static final double BREAK = 4D;

    private final Set<BlockPos> tree =
            new HashSet<>();

    private BlockPos target;

    private State state = State.SEARCH;

    private int cooldown;

    private enum State {
        SEARCH,
        REACH,
        CLIMB,
        BREAK
    }

    public ChopTreeGoal(ChibiEntity npc) {

        this.npc = npc;

        setFlags(EnumSet.of(
                Flag.MOVE,
                Flag.LOOK
        ));
    }

    @Override
    public boolean canUse() {

        if (!npc.isAlive()) {
            return false;
        }

        /*
         * สำคัญ:
         * ต้องมีต้นไม้ก่อนจึงจะยึด MOVE
         * ไม่งั้น GetItemGoal จะทำงานไม่ได้
         */
        if (findNearestLog() == null) {
            return false;
        }

        return npc.equipAxe();
    }

    @Override
    public boolean canContinueToUse() {

        return npc.isAlive()
                && hasAxe()
                && !tree.isEmpty();
    }

    @Override
    public void start() {

        tree.clear();
        target = null;
        state = State.SEARCH;
        cooldown = 0;
    }

    @Override
    public void stop() {

        npc.getNavigation().stop();
        npc.setClimbing(false);

        tree.clear();
        target = null;
        state = State.SEARCH;
    }

    @Override
    public void tick() {

        if (!hasAxe()) {
            return;
        }

        if (cooldown > 0) {
            cooldown--;
        }

        switch (state) {

            case SEARCH:
                searchTree();
                break;

            case REACH:
                reachTarget();
                break;

            case CLIMB:
                climbTarget();
                break;

            case BREAK:
                breakTarget();
                break;
        }
    }

    /*
     * =========================================================
     * FIND TREE
     * =========================================================
     */

    private void searchTree() {

        if (!tree.isEmpty()) {
            chooseHighestLog();
            return;
        }

        BlockPos start = findNearestLog();

        if (start == null) {
            return;
        }

        findConnectedLogs(start);

        chooseHighestLog();
    }

    private BlockPos findNearestLog() {

        BlockPos origin =
                npc.blockPosition();

        BlockPos best = null;
        double bestDistance =
                Double.MAX_VALUE;

        for (int x = -SEARCH; x <= SEARCH; x++) {
            for (int y = -SEARCH; y <= SEARCH; y++) {
                for (int z = -SEARCH; z <= SEARCH; z++) {

                    BlockPos pos =
                            origin.offset(x, y, z);

                    if (!isLog(pos)) {
                        continue;
                    }

                    /*
                     * กันไม่ให้ตัด log ที่ผู้เล่นวางเอง
                     * เช็คจากข้อมูลที่บันทึกไว้ก่อน (แม่นสุด, เร็ว)
                     * ถ้าไม่มีข้อมูล (log เก่าก่อนติดตั้ง mod นี้)
                     * fallback ไปเช็คด้วย heuristic ใบไม้แทน
                     */
                    if (isPlayerPlaced(pos)) {
                        continue;
                    }

                    if (!isPartOfNaturalTree(pos)) {
                        continue;
                    }

                    double distance =
                            npc.distanceToSqr(
                                    pos.getX() + 0.5D,
                                    pos.getY() + 0.5D,
                                    pos.getZ() + 0.5D
                            );

                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = pos.immutable();
                    }
                }
            }
        }

        return best;
    }

    private void findConnectedLogs(BlockPos start) {

        Queue<BlockPos> queue =
                new ArrayDeque<>();

        queue.add(start);

        while (!queue.isEmpty()
                && tree.size() < 512) {

            BlockPos pos = queue.poll();

            if (tree.contains(pos)
                    || !isLog(pos)) {
                continue;
            }

            tree.add(pos);

            for (Direction dir :
                    Direction.values()) {

                BlockPos next =
                        pos.relative(dir);

                if (!tree.contains(next)
                        && isLog(next)
                        && !isPlayerPlaced(next)) {

                    queue.add(next);
                }
            }
        }
    }

    /*
     * =========================================================
     * TOP -> DOWN
     * =========================================================
     */

    private void chooseHighestLog() {

        tree.removeIf(pos -> !isLog(pos));

        if (tree.isEmpty()) {
            target = null;
            state = State.SEARCH;
            return;
        }

        target = tree.stream()
                .max(
                        Comparator
                                .<BlockPos>comparingInt(
                                        BlockPos::getY
                                )
                                .thenComparingDouble(
                                        pos ->
                                                -npc.distanceToSqr(
                                                        pos.getX() + 0.5D,
                                                        pos.getY() + 0.5D,
                                                        pos.getZ() + 0.5D
                                                )
                                )
                )
                .orElse(null);

        state = State.REACH;
    }

    /*
     * =========================================================
     * REACH
     * =========================================================
     */

    private void reachTarget() {

        if (target == null
                || !isLog(target)) {

            tree.remove(target);
            chooseHighestLog();
            return;
        }

        double distance =
                npc.distanceToSqr(
                        target.getX() + 0.5D,
                        target.getY() + 0.5D,
                        target.getZ() + 0.5D
                );

        /*
         * ถึงไม้แล้ว
         */
        if (distance <= BREAK * BREAK) {

            state = State.BREAK;
            return;
        }

        /*
         * ไม้อยู่สูง → ปีน
         * (ต้องเข้าใกล้โคนต้นในแนวราบก่อน ไม่งั้น navigation
         *  จะหาทางไปจุดลอยกลางอากาศไม่เจอ แล้วค้างสลับ state ไปมา)
         */
        double horizontalDistSqr =
                Math.pow(target.getX() + 0.5D - npc.getX(), 2)
                        + Math.pow(target.getZ() + 0.5D - npc.getZ(), 2);

        if (target.getY() > npc.getBlockY() + 2
                && horizontalDistSqr <= 4.0D) {

            state = State.CLIMB;
            return;
        }

        /*
         * เดินไปหาไม้
         * ใช้ Y ของตัว npc เอง ไม่ใช่ Y ของ target (ที่อาจลอยอยู่สูง)
         * เพราะ navigation เดินดินหาทางไปพิกัดที่ลอยกลางอากาศไม่ได้
         * มันจะพาไปยืนใกล้โคนต้นในระดับพื้นแทน แล้วค่อยสลับไป CLIMB
         */
        npc.getNavigation().moveTo(
                target.getX() + 0.5D,
                npc.getY(),
                target.getZ() + 0.5D,
                1.0D
        );

        npc.getLookControl().setLookAt(
                target.getX() + 0.5D,
                target.getY() + 0.5D,
                target.getZ() + 0.5D
        );
    }

    /*
     * =========================================================
     * CLIMB
     * =========================================================
     */

    private void climbTarget() {

        if (target == null
                || !isLog(target)) {

            npc.setClimbing(false);
            tree.remove(target);
            chooseHighestLog();
            return;
        }

        /*
         * ถ้ายังไม่ถึงลำต้น
         */
        if (!nearTrunk()) {

            BlockPos side =
                    findClimbSide();

            if (side == null) {
                state = State.REACH;
                return;
            }

            npc.getNavigation().moveTo(
                    side.getX() + 0.5D,
                    side.getY(),
                    side.getZ() + 0.5D,
                    1.0D
            );

            npc.getLookControl().setLookAt(
                    target.getX() + 0.5D,
                    target.getY() + 0.5D,
                    target.getZ() + 0.5D
            );

            return;
        }

        /*
         * เกาะลำต้นแล้วปีนขึ้น
         * ปีนเลยไปจนสูงกว่ายอด log 1 บล็อก (ระดับพื้นผิวด้านบน)
         * เพื่อให้ขึ้นไปยืน "บน" ยอดได้ ไม่ใช่แค่เกาะข้าง ๆ ระดับเดียวกัน
         */
        npc.getNavigation().stop();
        npc.setClimbing(true);
        npc.setClimbTargetY(
                target.getY() + 1.0D
        );

        /*
         * ถึงระดับเหนือยอดแล้ว → ขยับเข้าไปยืนบนบล็อกจริง ๆ
         */
        if (npc.getY()
                >= target.getY() + 1.0D - 0.2D) {

            npc.setClimbing(false);

            npc.moveTo(
                    target.getX() + 0.5D,
                    target.getY() + 1.0D,
                    target.getZ() + 0.5D,
                    npc.getYRot(),
                    npc.getXRot()
            );

            state = State.BREAK;
        }
    }

    private boolean nearTrunk() {

        for (Direction dir :
                Direction.Plane.HORIZONTAL) {

            BlockPos side =
                    npc.blockPosition()
                            .relative(dir);

            if (isLog(side)) {
                return true;
            }
        }

        return false;
    }

    private BlockPos findClimbSide() {

        /*
         * ใช้ความสูงปัจจุบันของ npc เอง ไม่ใช่ความสูงของ target
         * (target คือยอดไม้ซึ่งลอยอยู่สูง) ไม่งั้น navigation
         * เดินดินจะหาทางไปจุดที่คำนวณไว้ไม่เจอ ค้างอยู่กับที่
         */
        BlockPos base = new BlockPos(
                target.getX(),
                npc.getBlockY(),
                target.getZ()
        );

        BlockPos best = null;
        double distance =
                Double.MAX_VALUE;

        for (Direction dir :
                Direction.Plane.HORIZONTAL) {

            BlockPos side =
                    base.relative(dir);

            /*
             * ตรงข้างลำต้นต้องว่าง
             */
            if (!npc.level()
                    .getBlockState(side)
                    .isAir()) {
                continue;
            }

            double d =
                    npc.distanceToSqr(
                            side.getX() + 0.5D,
                            side.getY(),
                            side.getZ() + 0.5D
                    );

            if (d < distance) {
                distance = d;
                best = side;
            }
        }

        return best;
    }

    /*
     * =========================================================
     * BREAK
     * =========================================================
     */

    private void breakTarget() {

        if (target == null
                || !isLog(target)) {

            tree.remove(target);
            chooseHighestLog();
            return;
        }

        double distance =
                npc.distanceToSqr(
                        target.getX() + 0.5D,
                        target.getY() + 0.5D,
                        target.getZ() + 0.5D
                );

        if (distance > BREAK * BREAK) {

            state = State.REACH;
            return;
        }

        npc.getLookControl().setLookAt(
                target.getX() + 0.5D,
                target.getY() + 0.5D,
                target.getZ() + 0.5D
        );

        /*
         * ใบไม้บัง → ทุบก่อน
         */
        BlockPos leaf =
                findLeafBetween(target);

        if (leaf != null) {

            breakBlock(leaf);
            cooldown = 3;

            return;
        }

        if (cooldown > 0) {
            return;
        }

        /*
         * ตัด Log
         */
        breakBlock(target);

        tree.remove(target);
        target = null;

        cooldown = 5;

        chooseHighestLog();
    }

    private BlockPos findLeafBetween(BlockPos target) {

        double dx =
                target.getX() + 0.5D - npc.getX();

        double dy =
                target.getY() + 0.5D - npc.getEyeY();

        double dz =
                target.getZ() + 0.5D - npc.getZ();

        double length =
                Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (length == 0) {
            return null;
        }

        dx /= length;
        dy /= length;
        dz /= length;

        for (int i = 1;
             i < (int) length;
             i++) {

            BlockPos pos =
                    BlockPos.containing(
                            npc.getX() + dx * i,
                            npc.getEyeY() + dy * i,
                            npc.getZ() + dz * i
                    );

            if (pos.equals(target)) {
                return null;
            }

            if (isLeaf(pos)) {
                return pos;
            }
        }

        return null;
    }

    private void breakBlock(BlockPos pos) {

        if (pos == null) {
            return;
        }

        BlockState state =
                npc.level().getBlockState(pos);

        if (!state.isAir()) {

            npc.level().destroyBlock(
                    pos,
                    true,
                    npc
            );
        }
    }

    /*
     * =========================================================
     * HELPERS
     * =========================================================
     */

    private boolean hasAxe() {

        return npc.getMainHandItem()
                .getItem() instanceof AxeItem;
    }

    private boolean isLog(BlockPos pos) {

        return npc.level()
                .getBlockState(pos)
                .is(BlockTags.LOGS);
    }

    private boolean isLeaf(BlockPos pos) {

        return npc.level()
                .getBlockState(pos)
                .is(BlockTags.LEAVES);
    }

    /*
     * เช็คว่า log ตรงนี้เป็นส่วนหนึ่งของ "ต้นไม้จริง" ไหม
     * โดยไล่เช็คขึ้นไปตามลำต้น (สมมติว่าลำต้นตั้งตรง) จนกว่า
     * จะเจอใบไม้อยู่ใกล้ ๆ หรือหมด log แล้ว — ป้องกันไม่ให้
     * ตัด log ที่ผู้เล่นวางเอง (บ้าน เสา รั้ว) ซึ่งปกติไม่มี
     * ใบไม้ติดอยู่เลย
     */
    private boolean isPlayerPlaced(BlockPos pos) {

        if (npc.level() instanceof ServerLevel serverLevel) {
            return PlacedLogData.get(serverLevel)
                    .isPlayerPlaced(pos);
        }

        return false;
    }

    private boolean isPartOfNaturalTree(BlockPos pos) {

        BlockPos cur = pos;

        for (int i = 0;
             i < 40 && isLog(cur);
             i++) {

            if (hasNearbyLeaves(cur)) {
                return true;
            }

            cur = cur.above();
        }

        return false;
    }

    private boolean hasNearbyLeaves(BlockPos pos) {

        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 3; y++) {
                for (int z = -2; z <= 2; z++) {

                    if (isLeaf(pos.offset(x, y, z))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
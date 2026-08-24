package net.mac.projectmod.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/*
 * เก็บพิกัด log block ที่ "ผู้เล่นวางเอง" ไว้ต่อโลก (per-level)
 * ผูกกับ NBT ของ level เพื่อไม่ให้หายตอน save/load
 *
 * หมายเหตุ: เก็บได้แค่ log ที่ถูกวางหลังติดตั้งระบบนี้แล้วเท่านั้น
 * log เดิมที่มีอยู่ก่อนหน้า (จากธรรมชาติ หรือผู้เล่นวางไปก่อนหน้า)
 * จะไม่มีข้อมูลอยู่ในนี้ — ต้องพึ่ง heuristic อื่นแทน (ดู ChopTreeGoal)
 */
public class PlacedLogData extends SavedData {

    private static final String ID = "projectmod_placed_logs";

    private final Set<Long> placedLogs = new HashSet<>();

    public static PlacedLogData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new Factory<>(
                        PlacedLogData::new,
                        (tag, provider) -> load(tag),
                        null
                ),
                ID
        );
    }

    public void markPlaced(BlockPos pos) {
        if (placedLogs.add(pos.asLong())) {
            setDirty();
        }
    }

    public void markRemoved(BlockPos pos) {
        if (placedLogs.remove(pos.asLong())) {
            setDirty();
        }
    }

    public boolean isPlayerPlaced(BlockPos pos) {
        return placedLogs.contains(pos.asLong());
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider pRegistries) {

        long[] array = new long[placedLogs.size()];
        int i = 0;

        for (long value : placedLogs) {
            array[i++] = value;
        }

        tag.putLongArray("PlacedLogs", array);
        return tag;
    }

    private static PlacedLogData load(CompoundTag tag) {

        PlacedLogData data = new PlacedLogData();

        for (long value : tag.getLongArray("PlacedLogs")) {
            data.placedLogs.add(value);
        }

        return data;
    }
}

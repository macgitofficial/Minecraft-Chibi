package net.mac.projectmod.data;

import net.mac.projectmod.data.PlacedLogData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/*
 * ดักตอนผู้เล่น (หรือ entity) วางบล็อก log แล้วบันทึกพิกัดไว้ใน
 * PlacedLogData เพื่อให้ ChopTreeGoal รู้ว่าบล็อกนี้ไม่ใช่ต้นไม้จริง
 *
 * ต้องลงทะเบียนเองใน constructor ของ mod หลัก:
 *   MinecraftForge.EVENT_BUS.register(new PlacedLogListener());
 */
public class PlacedLogListener {

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {

        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!event.getPlacedBlock().is(BlockTags.LOGS)) {
            return;
        }

        BlockPos pos = event.getPos();

        PlacedLogData.get(serverLevel).markPlaced(pos);
    }

    /*
     * ถ้าอยากให้ข้อมูลสะอาดขึ้น (ไม่เก็บพิกัดค้างหลังบล็อกถูกทำลาย)
     * สามารถเพิ่ม listener สำหรับ BlockEvent.BreakEvent แล้วเรียก
     * markRemoved(pos) ได้เช่นกัน — ไม่จำเป็นต่อ logic หลัก
     * เพราะ ChopTreeGoal เช็คแค่ isLog(pos) ควบคู่อยู่แล้ว
     * (บล็อกที่ถูกทำลายไปจะไม่ใช่ log อีกต่อไป เช็คนี้จะ false เอง)
     */
}

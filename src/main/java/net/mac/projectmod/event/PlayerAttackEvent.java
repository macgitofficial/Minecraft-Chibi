package net.mac.projectmod.event;

import net.mac.projectmod.entity.ChibiEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerAttackEvent {

    @SubscribeEvent
    public static void onPlayerAttack(LivingAttackEvent event) {
        //Mob ที่ถูกตี
        LivingEntity target = event.getEntity();
        // คนที่โจมตี
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        // หา Chibi ที่เป็นของ Player
        for (ChibiEntity chibi : player.level().getEntitiesOfClass(
                ChibiEntity.class,
                player.getBoundingBox().inflate(16.0D),
                npc -> npc.getOwnerUUID() != null
                        && npc.getOwnerUUID().equals(player.getUUID())
        )) {

            // ป้องกันไม่ให้ Chibi target ตัวเอง
            if (target == chibi) {
                continue;
            }

            // ตั้งเป้าหมายให้ Chibi
            chibi.setCombatTarget(target);
        }
    }
}

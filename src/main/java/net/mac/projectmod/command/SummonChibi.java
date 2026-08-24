package net.mac.projectmod.command;

import com.mojang.brigadier.CommandDispatcher;
import net.mac.projectmod.entity.ChibiEntity;
import net.mac.projectmod.entity.ModEntities;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class SummonChibi {
    public static void register( CommandDispatcher<CommandSourceStack> dispatcher ) {
        dispatcher.register(
                Commands.literal("summonchibi")
                .requires(source -> source.hasPermission(0))
                .executes(context -> {
                    ServerPlayer player =
                            context.getSource().getPlayerOrException();

                    // สร้าง NPC
                    ChibiEntity npc =
                            ModEntities.CHIBI.get()
                                    .create(player.level());
                    if (npc == null) { return 0; }

                    // ตำแหน่ง NPC
                    npc.moveTo(
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            player.getYRot(),
                            0.0F );

                    // กำหนด Owner
                    npc.setOwnerUUID(
                            player.getUUID()
                    );

                    // เพิ่ม NPC เข้าโลก
                    player.level().addFreshEntity(npc);
                    return 1;
                })
        );
    }
}

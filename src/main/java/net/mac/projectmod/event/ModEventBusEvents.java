package net.mac.projectmod.event;

import net.mac.projectmod.ProjectMod;
import net.mac.projectmod.entity.ChibiEntity;
import net.mac.projectmod.client.render.ProjectChibiRenderer;
import net.mac.projectmod.entity.ModEntities;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ProjectMod.MOD_ID , bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                ModEntities.CHIBI.get(),
                ProjectChibiRenderer::new
        );
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.CHIBI.get(),
                ChibiEntity.createAttributes().build());
    }

}

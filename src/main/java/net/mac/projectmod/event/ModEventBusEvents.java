package net.mac.projectmod.event;

import net.mac.projectmod.ProjectMod;
import net.mac.projectmod.entity.ModEntities;
import net.mac.projectmod.entity.client.NpcModel;
import net.mac.projectmod.entity.client.NpcRenderer;
import net.mac.projectmod.entity.custom.NpcEntity;
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
                NpcRenderer::new
        );
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(
                NpcModel.LAYER_LOCATION,
                NpcModel::createBodyLayer
        );
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.CHIBI.get(),
                NpcEntity.createAttributes().build());
    }
}

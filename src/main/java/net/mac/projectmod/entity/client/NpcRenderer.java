package net.mac.projectmod.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mac.projectmod.ProjectMod;
import net.mac.projectmod.entity.custom.NpcEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class NpcRenderer extends MobRenderer<NpcEntity, NpcModel<NpcEntity>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    ProjectMod.MOD_ID,
                    "textures/entity/chibi_npc.png"
            );

    public NpcRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new NpcModel<>(context.bakeLayer(NpcModel.LAYER_LOCATION)),
                0.5F
        );
    }

    @Override
    public ResourceLocation getTextureLocation(NpcEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(NpcEntity entity, PoseStack poseStack, float partialTick) {
        // ตอนนี้ใช้ขนาดปกติ
        // ถ้าจะทำ Chibi ทีหลัง เปลี่ยนเป็น poseStack.scale(0.7F, 0.7F, 0.7F);
        poseStack.scale(0.5F, 0.5F, 0.5F);

        super.scale(entity, poseStack, partialTick);
    }
}

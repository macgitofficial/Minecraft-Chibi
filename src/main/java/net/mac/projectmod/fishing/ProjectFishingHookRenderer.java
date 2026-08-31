/*
 * Fishing behaviour adapted from Touhou Little Maid (MIT-licensed code by tartaric_acid).
 * Adapted for Project Mod / Minecraft 1.21.x and ChibiEntity.
 * See THIRD_PARTY_LICENSES/TouhouLittleMaid-LICENSE-MIT.txt
 */
package net.mac.projectmod.fishing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mac.projectmod.entity.ChibiEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Fishing bobber + line renderer adapted from Touhou Little Maid.
 */
public class ProjectFishingHookRenderer extends EntityRenderer<ProjectFishingHook> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/fishing_hook.png");

    private static final RenderType RENDER_TYPE = RenderType.entityCutout(TEXTURE);

    public ProjectFishingHookRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(
            ProjectFishingHook hook,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        ChibiEntity chibi = hook.getChibiOwner();
        if (chibi == null) return;

        poseStack.pushPose();

        renderBobber(hook, poseStack, buffer, packedLight);
        renderFishingLine(hook, partialTick, poseStack, buffer, chibi);

        poseStack.popPose();

        super.render(hook, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private void renderBobber(
            ProjectFishingHook hook,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        PoseStack.Pose pose = poseStack.last();

        VertexConsumer consumer = buffer.getBuffer(RENDER_TYPE);

        vertex(consumer, pose, packedLight, 0.0F, 0, 0, 1);
        vertex(consumer, pose, packedLight, 1.0F, 0, 1, 1);
        vertex(consumer, pose, packedLight, 1.0F, 1, 1, 0);
        vertex(consumer, pose, packedLight, 0.0F, 1, 0, 0);

        poseStack.popPose();
    }

    private void renderFishingLine(
            ProjectFishingHook hook,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            ChibiEntity chibi
    ) {
        float bodyRot = Mth.lerp(
                partialTick,
                chibi.yBodyRotO,
                chibi.yBodyRot
        ) * ((float) Math.PI / 180F);

        double sin = Mth.sin(bodyRot);
        double cos = Mth.cos(bodyRot);

        double x1 = Mth.lerp(partialTick, chibi.xo, chibi.getX())
                - cos * 0.35D - sin * 0.8D;

        double y1 = Mth.lerp(partialTick, chibi.yo, chibi.getY())
                + chibi.getEyeHeight() - 0.45D;

        double z1 = Mth.lerp(partialTick, chibi.zo, chibi.getZ())
                - sin * 0.35D + cos * 0.8D;

        double x2 = Mth.lerp(partialTick, hook.xo, hook.getX());
        double y2 = Mth.lerp(partialTick, hook.yo, hook.getY()) + 0.25D;
        double z2 = Mth.lerp(partialTick, hook.zo, hook.getZ());

        float x = (float) (x1 - x2);
        float y = (float) (y1 - y2) - 0.1875F;
        float z = (float) (z1 - z2);

        VertexConsumer line = buffer.getBuffer(RenderType.lineStrip());
        PoseStack.Pose pose = poseStack.last();

        for (int i = 0; i <= 16; i++) {
            float f1 = i / 16.0F;
            float f2 = (i + 1) / 16.0F;

            float px = x * f1;
            float py = y * (f1 * f1 + f1) * 0.5F + 0.25F;
            float pz = z * f1;

            float nx = x * f2 - px;
            float ny = y * (f2 * f2 + f2) * 0.5F + 0.25F - py;
            float nz = z * f2 - pz;

            float length = Mth.sqrt(nx * nx + ny * ny + nz * nz);
            if (length == 0.0F) continue;

            nx /= length;
            ny /= length;
            nz /= length;

            line.addVertex(pose.pose(), px, py, pz)
                    .setColor(0, 0, 0, 255)
                    .setNormal(pose, nx, ny, nz);
        }
    }

    private static void vertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int light,
            float x,
            int y,
            int u,
            int v
    ) {
        consumer.addVertex(pose, x - 0.5F, y - 0.5F, 0.0F)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(ProjectFishingHook entity) {
        return TEXTURE;
    }
}

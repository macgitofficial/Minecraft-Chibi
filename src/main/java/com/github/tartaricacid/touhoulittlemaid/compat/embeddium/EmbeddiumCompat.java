package com.github.tartaricacid.touhoulittlemaid.compat.embeddium;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.AnimatedGeoBone;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public final class EmbeddiumCompat {
    private EmbeddiumCompat() {}
    public static boolean embeddiumRenderCubesOfBone(AnimatedGeoBone bone, PoseStack poseStack, VertexConsumer buffer,
                                                     int packedLight, int packedOverlay,
                                                     float red, float green, float blue, float alpha) {
        return false;
    }
}

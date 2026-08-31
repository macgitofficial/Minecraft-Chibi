package com.github.tartaricacid.touhoulittlemaid.compat.sodium;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.AnimatedGeoBone;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public final class SodiumCompat {
    private SodiumCompat() {}
    public static boolean sodiumRenderCubesOfBone(AnimatedGeoBone bone, PoseStack poseStack, VertexConsumer buffer,
                                                  int packedLight, int packedOverlay,
                                                  float red, float green, float blue, float alpha) {
        return false;
    }
}

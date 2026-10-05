/**
 * ProjectMod adaptation of rendering/animation code originally derived from
 * Touhou Little Maid. See LICENSE-TLM-MIT.txt and THIRD-PARTY-NOTICES.md for
 * upstream attribution and license. ProjectMod changes are maintained by the
 * ProjectMod project.
 */
package net.mac.projectmod.client.render;

import net.mac.projectmod.client.render.legacy.geckolib3.core.builder.Animation;
import net.mac.projectmod.client.render.legacy.geckolib3.file.AnimationFile;
import net.mac.projectmod.client.render.legacy.geckolib3.geo.raw.pojo.Converter;
import net.mac.projectmod.client.render.legacy.geckolib3.geo.raw.pojo.FormatVersion;
import net.mac.projectmod.client.render.legacy.geckolib3.geo.raw.pojo.RawGeoModel;
import net.mac.projectmod.client.render.legacy.geckolib3.geo.raw.tree.RawGeometryTree;
import net.mac.projectmod.client.render.legacy.geckolib3.geo.render.GeoBuilder;
import net.mac.projectmod.client.render.legacy.geckolib3.geo.render.built.GeoModel;
import net.mac.projectmod.client.render.legacy.geckolib3.resource.GeckoLibCache;
import net.mac.projectmod.client.render.legacy.geckolib3.core.molang.MolangParser;
import net.mac.projectmod.client.render.legacy.geckolib3.util.json.JsonAnimationUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.mac.projectmod.ProjectMod;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class ProjectModelLoader {
    private static boolean loaded;

    private ProjectModelLoader() {}

    public static synchronized void ensureLoaded() {
        if (loaded) return;
        try {
            var rm = Minecraft.getInstance().getResourceManager();
            ResourceLocation geoLocation = ResourceLocation.fromNamespaceAndPath(ProjectMod.MOD_ID, "geo/guga.geo.json");
            ResourceLocation animLocation = ResourceLocation.fromNamespaceAndPath(ProjectMod.MOD_ID, "animations/guga.animation.json");

            try (InputStream in = rm.open(geoLocation)) {
                registerGeo(ResourceLocation.fromNamespaceAndPath(ProjectMod.MOD_ID, "guga"), in);
            }
            try (InputStream in = rm.open(animLocation)) {
                registerAnimation(ResourceLocation.fromNamespaceAndPath(ProjectMod.MOD_ID, "guga"), in);
            }
            loaded = true;
            ProjectMod.LOGGER.info("Loaded TLM-style animation/model core for Chibi");
        } catch (IOException e) {
            ProjectMod.LOGGER.error("Failed to load Chibi TLM model/animation resources", e);
        }
    }

    private static void registerGeo(ResourceLocation id, InputStream inputStream) {
        RawGeoModel rawModel = Converter.fromInputStream(inputStream);
        if (rawModel.getFormatVersion() != FormatVersion.NEW) {
            throw new IllegalStateException("Unsupported geometry format for " + id);
        }
        RawGeometryTree tree = RawGeometryTree.parseHierarchy(rawModel);
        GeoModel model = GeoBuilder.getGeoBuilder().constructGeoModel(tree);
        GeckoLibCache.getInstance().getGeoModels().put(id, model);
    }

    private static void registerAnimation(ResourceLocation id, InputStream inputStream) {
        JsonObject root = new com.google.gson.GsonBuilder().create().fromJson(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8),
                JsonObject.class
        );
        AnimationFile file = new AnimationFile();
        MolangParser parser = GeckoLibCache.getInstance().parser;
        for (Map.Entry<String, JsonElement> entry : JsonAnimationUtils.getAnimations(root)) {
            String name = entry.getKey();
            try {
                Animation animation = JsonAnimationUtils.deserializeJsonToAnimation(
                        JsonAnimationUtils.getAnimation(root, name), parser
                );
                file.putAnimation(name, animation);
            } catch (Exception ex) {
                ProjectMod.LOGGER.error("Failed to load animation {}", name, ex);
            }
        }
        GeckoLibCache.getInstance().getAnimations().put(id, file);
    }
}

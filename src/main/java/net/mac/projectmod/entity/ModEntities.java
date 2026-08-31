package net.mac.projectmod.entity;

import net.mac.projectmod.ProjectMod;
import net.mac.projectmod.fishing.ProjectFishingHook;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ProjectMod.MOD_ID);

    public static final Supplier<EntityType<ChibiEntity>> CHIBI =
            ENTITY_TYPES.register("chibi",
                    () -> EntityType.Builder.of(
                                    ChibiEntity::new,
                                    MobCategory.CREATURE
                            )
                            .sized(0.6F, 1.0F)
                            .build("chibi"));
    public static final Supplier<EntityType<ProjectFishingHook>> FISHING_HOOK =
            ENTITY_TYPES.register("fishing_hook",
                    () -> EntityType.Builder.<ProjectFishingHook>of(
                                    ProjectFishingHook::new,
                                    MobCategory.MISC
                            )
                            .noSave()
                            .noSummon()
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(4)
                            .updateInterval(5)
                            .build("fishing_hook"));

}

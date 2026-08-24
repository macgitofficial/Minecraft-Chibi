package net.mac.projectmod.entity;

import net.mac.projectmod.ProjectMod;
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
                            .sized(0.6F, 1.8F)
                            .build("chibi"));
}

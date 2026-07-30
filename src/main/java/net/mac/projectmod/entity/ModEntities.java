package net.mac.projectmod.entity;

import net.mac.projectmod.entity.custom.NpcEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import static net.mac.projectmod.ProjectMod.MOD_ID;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);

    public static final RegistryObject<EntityType<NpcEntity>> CHIBI =
            ENTITY_TYPES.register("chibi",
                    () -> EntityType.Builder.of(NpcEntity::new, MobCategory.CREATURE)
                            .sized(0.5F, 0.7F)
                            .build("chibi"));
}

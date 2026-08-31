package net.mac.projectmod.sound;

import net.mac.projectmod.ProjectMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, ProjectMod.MOD_ID);

    public static final Supplier<SoundEvent> CHIBI_AMBIENT =
            SOUNDS.register("chibi_ambient",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(
                                    ProjectMod.MOD_ID,
                                    "chibi_ambient"
                            )));

    public static final Supplier<SoundEvent> CHIBI_HURT =
            SOUNDS.register("chibi_hurt",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(
                                    ProjectMod.MOD_ID,
                                    "chibi_hurt"
                            )));

    public static final Supplier<SoundEvent> CHIBI_DEATH =
            SOUNDS.register("chibi_death",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(
                                    ProjectMod.MOD_ID,
                                    "chibi_death"
                            )));
}

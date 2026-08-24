package net.mac.projectmod.gui;

import net.mac.projectmod.ProjectMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;


public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ProjectMod.MOD_ID);

    public static final RegistryObject<MenuType<NpcInventoryMenu>> NPC_INVENTORY =
            MENUS.register("npc_inventory",
                    () -> IForgeMenuType.create(NpcInventoryMenu::new));

}

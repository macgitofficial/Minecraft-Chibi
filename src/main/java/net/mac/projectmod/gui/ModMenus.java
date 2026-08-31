package net.mac.projectmod.gui;

import net.mac.projectmod.ProjectMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ProjectMod.MOD_ID);

    public static final RegistryObject<MenuType<ChibiInventoryMenu>> CHIBI_INVENTORY =
            MENUS.register("chibi_inventory",
                    () -> IForgeMenuType.create(ChibiInventoryMenu::new));
}

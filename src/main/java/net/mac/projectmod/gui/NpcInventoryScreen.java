package net.mac.projectmod.gui;

import net.mac.projectmod.ProjectMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class NpcInventoryScreen extends AbstractContainerScreen<NpcInventoryMenu> {
    public NpcInventoryScreen(
            NpcInventoryMenu menu,
            Inventory playerInventory,
            Component title
    ) {
        super(menu, playerInventory, title);

        this.imageWidth = 176;
        this.imageHeight = 170;
    }

    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    ProjectMod.MOD_ID,
                    "textures/gui/chibi_inventory.png"
            );

    @Override
    protected void renderBg(
            GuiGraphics guiGraphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {
        guiGraphics.blit(GUI_TEXTURE,
                this.leftPos,
                this.topPos,
                0,
                0,
                176,
                170,
                176,
                170);
    }

    @Override
    protected void renderLabels(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY
    ) {
        guiGraphics.drawString(
                this.font,
                "CHIBI INVENTORY",
                8,
                8,
                0xFFFFFF
        );

        guiGraphics.drawString(
                this.font,
                "Player Inventory",
                8,
                67,
                0xAAAAAA
        );
    }
}

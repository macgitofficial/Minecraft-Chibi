package net.mac.projectmod.gui;

import net.mac.projectmod.ProjectMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ChibiInventoryScreen extends AbstractContainerScreen<ChibiInventoryMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    ProjectMod.MOD_ID,
                    "textures/gui/chibi_inventory.png"
            );

    public ChibiInventoryScreen(
            ChibiInventoryMenu menu,
            Inventory playerInventory,
            Component title
    ) {
        super(menu, playerInventory, title);

        /*
         * ขนาดของ GUI logical
         *
         * เดี๋ยวเราปรับอีกทีให้เกือบเต็มจอ
         */
        this.imageWidth = 320;
        this.imageHeight = 220;
    }

    @Override
    protected void init() {
        super.init();

        /*
         * จุดเริ่มต้นของ GUI
         *
         * Minecraft จะวาง GUI ไว้ตรงกลางจอ
         */
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
    }

    @Override
    protected void renderBg(
            GuiGraphics guiGraphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {
        /*
         * วาดพื้นหลัง GUI
         */
        guiGraphics.blit(
                TEXTURE,
                this.leftPos,
                this.topPos,
                0,
                0,
                this.imageWidth,
                this.imageHeight,
                this.imageWidth,
                this.imageHeight
        );
    }

    @Override
    protected void renderLabels(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY
    ) {
        /*
         * ยังไม่วาด label ตรงนี้
         *
         * เพราะเราจะใส่ข้อความลงใน PNG
         * เพื่อควบคุมหน้าตาแบบ Vanilla เอง
         */
    }

    @Override
    public void render(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
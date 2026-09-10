package io.ticticboom.mods.mm.port.nuclear.radiation.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.nuclear.radiation.NuclearRadiationPortStorage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.player.Inventory;

public class NuclearRadiationPortScreen extends AbstractContainerScreen<NuclearRadiationPortMenu> {

    private static final int MAX_NAME = 55;

    private final FormattedText header;
    private final NuclearRadiationPortBlockEntity be;

    public NuclearRadiationPortScreen(NuclearRadiationPortMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageHeight = 222;
        this.imageWidth = 174;
        String name = menu.getModel().displayName().getString();
        header = FormattedText.of(name.length() > MAX_NAME ? name.substring(0, MAX_NAME) + "..." : name);
        be = menu.getBlockEntity();
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        gfx.blit(Ref.UiTextures.PORT_GUI, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        if (be.isInput()) {
            gfx.blit(Ref.UiTextures.SLOT_PARTS, this.leftPos + NuclearRadiationPortStorage.SLOT_X,
                    this.topPos + NuclearRadiationPortStorage.SLOT_Y, 0, 26, 18, 18);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.drawWordWrap(this.font, header, 8, 8, 158, 0x404040);
        int y = 24;
        for (Component line : be.getRadiationStorage().guiLines()) {
            gfx.drawString(this.font, line, 8, y, 0x404040, false);
            y += 11;
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx, mouseX, mouseY, partialTick);
        super.render(gfx, mouseX, mouseY, partialTick);
        renderTooltip(gfx, mouseX, mouseY);
    }
}

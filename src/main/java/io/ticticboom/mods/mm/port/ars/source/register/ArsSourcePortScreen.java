package io.ticticboom.mods.mm.port.ars.source.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.ars.source.ArsSourcePortStorage;
import io.ticticboom.mods.mm.util.NumberText;
import io.ticticboom.mods.mm.util.WidgetUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;

public class ArsSourcePortScreen extends AbstractContainerScreen<ArsSourcePortMenu> {

    private final FormattedText header;

    public ArsSourcePortScreen(ArsSourcePortMenu menu, Inventory inv, Component displayName) {
        super(menu, inv, displayName);
        this.imageHeight = 222;
        this.imageWidth = 174;
        String name = menu.getModel().displayName().getString();
        int subStrLength = Math.min(55, name.length());
        header = FormattedText.of(name.substring(0, subStrLength) + (subStrLength < 55 ? "" : "..."));
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTicks, int mouseX, int mouseY) {
        gfx.blit(Ref.UiTextures.PORT_GUI, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.drawWordWrap(this.font, header, 8, 8, 150, 0x404040);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx, mouseX, mouseY, partialTick);
        super.render(gfx, mouseX, mouseY, partialTick);
        renderTooltip(gfx, mouseX, mouseY);
        gfx.blit(Ref.UiTextures.SLOT_PARTS, this.leftPos + 7, this.topPos + 50, 89, 78, 162, 80);
        ArsSourcePortBlockEntity be = menu.getBlockEntity();
        ArsSourcePortStorage storage = be.getSourceStorage();
        var filledValue = (double) storage.getStored() / (double) storage.getCapacity();
        var filledHeight = (int) (Math.min(filledValue, 1) * 78);
        var start = 129 - filledHeight;
        gfx.blit(Ref.UiTextures.SLOT_PARTS, this.leftPos + 8, this.topPos + start, 90, 0, 160, filledHeight);
        if (WidgetUtils.isPointerWithinSized(mouseX, mouseY, this.leftPos + 7, this.topPos + 50, 162, 80)) {
            var tooltip = new ArrayList<Component>();
            tooltip.add(Component.translatable("gui.mm.port.ars_source.storage",
                    NumberText.grouped(storage.getStored()), NumberText.grouped(storage.getCapacity())));
            gfx.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }
}

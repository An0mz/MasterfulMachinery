package io.ticticboom.mods.mm.port.mekanism.heat.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.mekanism.heat.MekanismHeatPortStorage;
import io.ticticboom.mods.mm.port.mekanism.heat.MekanismHeatPortStorageModel;
import io.ticticboom.mods.mm.util.WidgetUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;

public class MekanismHeatPortScreen extends AbstractContainerScreen<MekanismHeatPortMenu> {

    private final FormattedText header;

    public MekanismHeatPortScreen(MekanismHeatPortMenu menu, Inventory inv, Component displayName) {
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
        MekanismHeatPortBlockEntity be = menu.getBlockEntity();
        MekanismHeatPortStorage storage = (MekanismHeatPortStorage) be.getStorage();
        MekanismHeatPortStorageModel storageModel = be.getStorageModel();
        var filledValue = (double) storage.getStoredHeat() / (double) storageModel.capacity();
        var filledHeight = (int) (Math.min(filledValue, 1) * 78);
        var start = 129 - filledHeight;
        gfx.blit(Ref.UiTextures.SLOT_PARTS, this.leftPos + 8, this.topPos + start, 90, 0, 160, filledHeight);
        if (WidgetUtils.isPointerWithinSized(mouseX, mouseY, this.leftPos + 7, this.topPos + 50, 162, 80)) {
            var tooltip = new ArrayList<Component>();
            tooltip.add(Component.translatable("gui.mm.port.mekanism_heat.storage", storage.getStoredHeat(), storageModel.capacity()));
            tooltip.add(Component.translatable("gui.mm.port.mekanism_heat.temperature", String.format("%.1f", storage.getTemperature())));
            gfx.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }
}

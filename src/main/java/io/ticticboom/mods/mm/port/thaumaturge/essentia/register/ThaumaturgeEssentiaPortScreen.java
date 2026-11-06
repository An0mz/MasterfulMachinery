package io.ticticboom.mods.mm.port.thaumaturge.essentia.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.gui.widgets.PortConfigPanel;
import io.ticticboom.mods.mm.client.gui.widgets.TankGauge;
import io.ticticboom.mods.mm.client.util.CountFormat;
import io.ticticboom.mods.mm.port.thaumaturge.essentia.EssentiaAspects;
import io.ticticboom.mods.mm.port.thaumaturge.essentia.ThaumaturgeEssentiaPortStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;

public class ThaumaturgeEssentiaPortScreen extends AbstractContainerScreen<ThaumaturgeEssentiaPortMenu> {

    private final ThaumaturgeEssentiaPortStorage storage;
    private final PortConfigPanel configPanel;

    public ThaumaturgeEssentiaPortScreen(ThaumaturgeEssentiaPortMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageHeight = 222;
        this.imageWidth = 174;
        ThaumaturgeEssentiaPortBlockEntity be = menu.getBlockEntity();
        storage = be.getEssentiaStorage();
        configPanel = new PortConfigPanel(be);
    }

    @Override
    protected void init() {
        super.init();
        configPanel.setPosition(this.leftPos, this.topPos, this.imageWidth);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (configPanel.mouseClicked(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        return super.hasClickedOutside(mouseX, mouseY, left, top, button) && !configPanel.isWithin(mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        gfx.blit(Ref.UiTextures.PORT_GUI, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        int x = this.leftPos + TankGauge.X;
        int y = this.topPos + TankGauge.Y;
        TankGauge.drawFrame(gfx, x, y);
        var aspect = storage.getAspect();
        int capacity = storage.getCapacity();
        if (aspect != null && storage.getAmount() > 0 && capacity > 0) {
            int inner = TankGauge.H - 2;
            int filled = (int) Math.round(inner * Math.min(1, (double) storage.getAmount() / capacity));
            int bottom = y + 1 + inner;
            gfx.fill(x + 1, bottom - filled, x + TankGauge.W - 1, bottom, EssentiaAspects.color(minecraft.level, aspect));
        }
        Component name = aspect == null ? null : EssentiaAspects.name(aspect);
        TankGauge.drawLabel(gfx, this.font, x, y, name, storage.getAmount(), capacity, "");
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        PortConfigPanel.drawTitle(gfx, this.font, menu.getModel().displayName(), this.imageWidth);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx, mouseX, mouseY, partialTick);
        super.render(gfx, mouseX, mouseY, partialTick);
        configPanel.render(gfx, this.font, mouseX, mouseY);
        if (configPanel.isWithin(mouseX, mouseY)) {
            configPanel.renderTooltip(gfx, this.font, mouseX, mouseY);
            return;
        }
        renderTooltip(gfx, mouseX, mouseY);
        if (TankGauge.isHovered(mouseX, mouseY, this.leftPos + TankGauge.X, this.topPos + TankGauge.Y)) {
            var aspect = storage.getAspect();
            var tooltip = new ArrayList<Component>();
            tooltip.add(aspect == null ? Component.translatable("gui.mm.port.tank.empty") : EssentiaAspects.name(aspect));
            tooltip.add(Component.literal(CountFormat.grouped(storage.getAmount()) + " / "
                    + CountFormat.grouped(storage.getCapacity())).withStyle(ChatFormatting.GRAY));
            if (storage.isLocked()) {
                tooltip.add(Component.translatable("gui.mm.port.tank.locked_any").withStyle(ChatFormatting.GOLD));
            }
            gfx.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }
}

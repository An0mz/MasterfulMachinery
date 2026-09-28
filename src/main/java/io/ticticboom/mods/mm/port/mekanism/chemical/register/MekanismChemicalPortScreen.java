package io.ticticboom.mods.mm.port.mekanism.chemical.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.gui.widgets.PortConfigPanel;
import io.ticticboom.mods.mm.client.gui.widgets.TankGauge;
import io.ticticboom.mods.mm.client.util.CountFormat;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorage;
import mekanism.client.render.MekanismRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;

public class MekanismChemicalPortScreen<T extends MekanismChemicalPortMenu> extends AbstractContainerScreen<T> {

    protected final MekanismChemicalPortBlockEntity be;
    protected final MekanismChemicalPortStorage storage;
    private final PortConfigPanel configPanel;

    public MekanismChemicalPortScreen(T menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageHeight = 222;
        this.imageWidth = 174;
        be = this.menu.getBlockEntity();
        storage = (MekanismChemicalPortStorage) be.getStorage();
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
        var stack = storage.chemicalTank.getStack();
        long capacity = storage.chemicalTank.getCapacity();
        if (!stack.isEmpty() && capacity > 0) {
            TankGauge.drawFill(gfx, x, y, (double) stack.getAmount() / capacity,
                    MekanismRenderer.getSprite(stack.getChemical().getIcon()), stack.getChemical().getTint());
        }
        Component name = stack.isEmpty() ? null : stack.getChemical().getTextComponent();
        TankGauge.drawLabel(gfx, this.font, x, y, name, stack.getAmount(), capacity, "mB");
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
            var stack = storage.chemicalTank.getStack();
            var tooltip = new ArrayList<Component>();
            tooltip.add(stack.isEmpty() ? Component.translatable("gui.mm.port.tank.empty") : stack.getChemical().getTextComponent());
            tooltip.add(Component.literal(CountFormat.grouped(stack.getAmount()) + " / "
                    + CountFormat.grouped(storage.chemicalTank.getCapacity()) + " mB").withStyle(ChatFormatting.GRAY));
            if (storage.isLocked()) {
                tooltip.add(Component.translatable("gui.mm.port.tank.locked_any").withStyle(ChatFormatting.GOLD));
            }
            gfx.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }
}

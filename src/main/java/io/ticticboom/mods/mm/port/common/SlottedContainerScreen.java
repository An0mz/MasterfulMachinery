package io.ticticboom.mods.mm.port.common;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.IPortMenu;
import io.ticticboom.mods.mm.util.BlockUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.phys.Vec2;

import java.util.ArrayList;

public class SlottedContainerScreen<T extends AbstractContainerMenu & IPortMenu> extends AbstractContainerScreen<T> {

    protected final T menu;
    protected final FormattedText header;
    protected ArrayList<Vec2> slots = new ArrayList<>();

    public SlottedContainerScreen(T menu, Inventory inv, Component displayName) {
        super(menu, inv, displayName);
        this.menu = menu;
        this.imageHeight = 222;
        this.imageWidth = 174;
        String name = menu.getModel().displayName().getString();
        int subStrLength = Math.min(55, name.length());
        header = FormattedText.of(name.substring(0, subStrLength) + (subStrLength < 55 ? "" : "..."));
        setupSlots();
    }

    private void setupSlots() {
        IPortBlockEntity blockEntity = menu.getBlockEntity();
        var storage = blockEntity.getStorage();
        var model = (ISlottedPortStorageModel) storage.getStorageModel();

        var columns = model.columns();
        var rows = model.rows();

        // The 18x18 slot background has a 1px border around the 16x16 content area, so it is drawn
        // one pixel up and left of where the menu places the Slot itself.
        int offsetX = BlockUtils.slotGridOriginX(columns) - 1;
        int offsetY = BlockUtils.slotGridOriginY(rows) - 1;
        slots.ensureCapacity(columns * rows);

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                slots.add(new Vec2(x * 18 + offsetX, y * 18 + offsetY));
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTicks, int mouseX, int mouseY) {
        gfx.blit(Ref.UiTextures.PORT_GUI, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        for (Vec2 slot : slots) {
            gfx.blit(Ref.UiTextures.SLOT_PARTS, this.leftPos + (int) slot.x, this.topPos + (int) slot.y, 0, 26, 18, 18);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        // One line on purpose. The slot grid starts at a fixed y that the menu also uses to place
        // its Slot objects, so a title allowed to wrap would draw on top of the first row.
        var lines = this.font.split(header, 150);
        if (!lines.isEmpty()) {
            gfx.drawString(this.font, lines.get(0), 8, 8, 0x404040, false);
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        renderBackground(gfx, mouseX, mouseY, partialTicks);
        super.render(gfx, mouseX, mouseY, partialTicks);
        renderTooltip(gfx, mouseX, mouseY);
    }
}

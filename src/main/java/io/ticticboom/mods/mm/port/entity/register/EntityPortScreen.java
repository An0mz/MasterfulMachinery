package io.ticticboom.mods.mm.port.entity.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.entity.EntityPortMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.player.Inventory;

import java.util.LinkedHashMap;

public class EntityPortScreen extends AbstractContainerScreen<EntityPortMenu> {

    private static final int LIST_X = 10;
    private static final int LIST_Y = 55;
    private static final int LINE_HEIGHT = 11;
    private static final int MAX_LINES = 6;

    private final FormattedText header;

    public EntityPortScreen(EntityPortMenu menu, Inventory inv, Component displayName) {
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

        EntityPortBlockEntity be = menu.getBlockEntity();
        var storage = be.getPortStorage();
        var model = be.getStorageModel();
        var entities = storage.entities();

        var summary = Component.translatable(model.mode() == EntityPortMode.STORED
                ? "gui.mm.port.entity.stored"
                : "gui.mm.port.entity.standing", entities.size(), model.capacity());
        gfx.drawString(this.font, summary, this.leftPos + LIST_X, this.topPos + 40, 0x404040, false);

        var counts = new LinkedHashMap<Component, Integer>();
        for (var entry : entities) {
            counts.merge(entry.displayName(), 1, Integer::sum);
        }

        int line = 0;
        for (var entry : counts.entrySet()) {
            if (line >= MAX_LINES) {
                break;
            }
            gfx.drawString(this.font,
                    Component.translatable("gui.mm.port.entity.entry", entry.getValue(), entry.getKey()),
                    this.leftPos + LIST_X, this.topPos + LIST_Y + line * LINE_HEIGHT, 0xFFFFFF, true);
            line++;
        }
        if (line == 0) {
            gfx.drawString(this.font, Component.translatable("gui.mm.port.entity.empty"),
                    this.leftPos + LIST_X, this.topPos + LIST_Y, 0xFFFFFF, true);
        }
    }
}

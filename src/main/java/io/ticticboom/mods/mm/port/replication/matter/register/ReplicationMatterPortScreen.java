package io.ticticboom.mods.mm.port.replication.matter.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.replication.matter.MatterTypes;
import io.ticticboom.mods.mm.port.replication.matter.ReplicationMatterPortStorage;
import io.ticticboom.mods.mm.port.replication.matter.ReplicationMatterPortTank;
import io.ticticboom.mods.mm.util.WidgetUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;

public class ReplicationMatterPortScreen extends AbstractContainerScreen<ReplicationMatterPortMenu> {

    private static final int AREA_X = 8;
    private static final int AREA_Y = 51;
    private static final int AREA_WIDTH = 160;
    private static final int AREA_HEIGHT = 78;

    private final FormattedText header;

    public ReplicationMatterPortScreen(ReplicationMatterPortMenu menu, Inventory inv, Component displayName) {
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

        ReplicationMatterPortBlockEntity be = menu.getBlockEntity();
        var storage = (ReplicationMatterPortStorage) be.getStorage();
        var tanks = storage.getHandler().tanks();
        if (tanks.isEmpty()) {
            return;
        }
        int count = tanks.size();
        int top = this.topPos + AREA_Y;
        int bottom = top + AREA_HEIGHT;
        ReplicationMatterPortTank hovered = null;
        for (int i = 0; i < count; i++) {
            var tank = tanks.get(i);
            int left = this.leftPos + AREA_X + (AREA_WIDTH * i / count);
            int right = this.leftPos + AREA_X + (AREA_WIDTH * (i + 1) / count);
            var filled = (int) (Math.min(tank.getMatterAmount() / tank.getCapacity(), 1) * AREA_HEIGHT);
            if (filled > 0) {
                gfx.fill(left, bottom - filled, right, bottom, MatterTypes.colorOf(tank.getMatter().getMatterType()));
            }
            if (WidgetUtils.isPointerWithinSized(mouseX, mouseY, left, top, right - left, AREA_HEIGHT)) {
                hovered = tank;
            }
        }
        if (hovered != null) {
            gfx.renderComponentTooltip(this.font, tooltip(hovered), mouseX, mouseY);
        }
    }

    private ArrayList<Component> tooltip(ReplicationMatterPortTank tank) {
        var tooltip = new ArrayList<Component>();
        if (tank.getMatter().isEmpty()) {
            var filter = tank.getFilter();
            tooltip.add(filter == null
                    ? Component.translatable("gui.mm.port.replication_matter.empty")
                    : Component.translatable("gui.mm.port.replication_matter.reserved", filter.toString()));
        } else {
            tooltip.add(tank.getMatter().getDisplayName());
        }
        tooltip.add(Component.translatable("gui.mm.port.replication_matter.storage",
                (int) Math.floor(tank.getMatterAmount()), (int) tank.getCapacity()));
        return tooltip;
    }
}

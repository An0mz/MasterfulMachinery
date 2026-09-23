package io.ticticboom.mods.mm.port.common;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.IPortMenu;
import io.ticticboom.mods.mm.util.BlockUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.phys.Vec2;

import java.util.ArrayList;

public class SlottedContainerScreen<T extends AbstractContainerMenu & IPortMenu & IPagedPortMenu> extends AbstractContainerScreen<T> {

    private static final int PAGE_BAR_TOP = 112;
    private static final int PAGE_BUTTON_SIZE = 20;

    protected final T menu;
    protected final FormattedText header;
    protected final PortPager pager;
    protected final PortGrid grid;
    protected ArrayList<Vec2> slots = new ArrayList<>();
    private Button previousPage;
    private Button nextPage;

    public SlottedContainerScreen(T menu, Inventory inv, Component displayName) {
        super(menu, inv, displayName);
        this.menu = menu;
        this.imageHeight = 222;
        this.imageWidth = 174;
        String name = menu.getModel().displayName().getString();
        int subStrLength = Math.min(55, name.length());
        header = FormattedText.of(name.substring(0, subStrLength) + (subStrLength < 55 ? "" : "..."));
        pager = menu.getPager();
        grid = pager.grid();
        setupSlots();
    }

    protected int page() {
        return pager.page();
    }

    protected int firstSlotOnPage() {
        return grid.firstSlot(page());
    }

    private void setPage(int page) {
        pager.setPage(page);
        setupSlots();
        updatePageButtons();
    }

    private void setupSlots() {
        int count = grid.slotsOnPage(page());
        slots.clear();
        slots.ensureCapacity(count);

        // The 18x18 slot background has a 1px border around the 16x16 content area, so it is drawn
        // one pixel up and left of where the menu places the Slot itself.
        for (int i = 0; i < count; i++) {
            slots.add(new Vec2(grid.slotX(i) - 1, grid.slotY(i) - 1));
        }
    }

    @Override
    protected void init() {
        super.init();
        if (grid.pages() <= 1) {
            return;
        }
        int top = this.topPos + PAGE_BAR_TOP;
        previousPage = addRenderableWidget(Button.builder(Component.literal("<"), b -> setPage(page() - 1))
                .bounds(this.leftPos + 8, top, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE).build());
        nextPage = addRenderableWidget(Button.builder(Component.literal(">"), b -> setPage(page() + 1))
                .bounds(this.leftPos + this.imageWidth - 8 - PAGE_BUTTON_SIZE, top, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE).build());
        updatePageButtons();
    }

    private void updatePageButtons() {
        if (previousPage == null || nextPage == null) {
            return;
        }
        previousPage.active = page() > 0;
        nextPage.active = page() < grid.pages() - 1;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (grid.pages() > 1 && scrollY != 0 && overPortArea(mouseX, mouseY)) {
            setPage(page() + (scrollY < 0 ? 1 : -1));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean overPortArea(double mouseX, double mouseY) {
        double x = mouseX - this.leftPos;
        double y = mouseY - this.topPos;
        return x >= 0 && x < this.imageWidth && y >= 0 && y < BlockUtils.PLAYER_INVENTORY_TOP;
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
        if (grid.pages() > 1) {
            var label = Component.literal((page() + 1) + " / " + grid.pages());
            gfx.drawString(this.font, label, (this.imageWidth - this.font.width(label)) / 2,
                    PAGE_BAR_TOP + (PAGE_BUTTON_SIZE - 8) / 2, 0x404040, false);
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        renderBackground(gfx, mouseX, mouseY, partialTicks);
        super.render(gfx, mouseX, mouseY, partialTicks);
        renderTooltip(gfx, mouseX, mouseY);
    }
}

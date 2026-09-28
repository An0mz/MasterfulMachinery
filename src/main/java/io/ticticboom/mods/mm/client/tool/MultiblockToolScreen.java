package io.ticticboom.mods.mm.client.tool;

import io.ticticboom.mods.mm.client.util.NineSliceUtil;
import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.builder.TierPrefs;
import io.ticticboom.mods.mm.builder.structure.BuildableStructure;
import io.ticticboom.mods.mm.builder.structure.BuildableStructureRegistry;
import io.ticticboom.mods.mm.client.builder.StructureTierRows;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import io.ticticboom.mods.mm.client.structure.GuiStructureRenderer;
import io.ticticboom.mods.mm.client.util.CountFormat;
import io.ticticboom.mods.mm.client.util.TextRenderUtil;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import net.neoforged.neoforge.network.PacketDistributor;
import io.ticticboom.mods.mm.net.packet.ToolSettingsPkt;
import io.ticticboom.mods.mm.networklink.NetworkLink;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.tool.MultiblockToolMenu;
import io.ticticboom.mods.mm.tool.ToolData;
import io.ticticboom.mods.mm.tool.ToolEnergy;
import io.ticticboom.mods.mm.tool.ToolSlot;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class MultiblockToolScreen extends AbstractContainerScreen<MultiblockToolMenu> {
    private static final int MIN_WIDTH = 378;
    private static final int MIN_HEIGHT = 240;
    private static final int MAX_WIDTH = 520;
    private static final int MAX_HEIGHT = 400;
    private static final int WINDOW_MARGIN = 12;
    private static final int WINDOW_MARGIN_Y = 4;
    private static final int FRAME = 6;
    private static final int FRAME_BOTTOM = 7;
    private static final int INSET = FRAME + 2;
    private static final int TOP = 17;
    private static final int GAP = 4;
    private static final int GREY = 0xFFC6C6C6;
    private static final int PANEL = 0xFF1C1C1C;
    private static final int TITLE = 0x404040;
    private static final int TEXT = 0xE0E0E0;
    private static final int LABEL = 0x9A9A9A;
    private static final int ADJUSTED = 0xFFD84D;

    private static final int SLOT = 18;
    private static final int COLUMNS = 9;
    private static final int STORE_ROWS = MultiblockToolMenu.STORE_SLOTS / COLUMNS;
    private static final int GRID_WIDTH = COLUMNS * SLOT;
    private static final int STORE_HEIGHT = STORE_ROWS * SLOT;
    private static final int INVENTORY_HEIGHT = 76;
    private static final int FE_WIDTH = 10;
    private static final int GALLERY_MIN = 100;
    private static final int GALLERY_MAX = 140;
    private static final int SEARCH_HEIGHT = 12;
    private static final int INFO_HEIGHT = 13;
    private static final int ME_HEIGHT = 11;
    private static final int ME_GAP = 2;
    private static final int LAYER_HEIGHT = 11;
    private static final int LAYER_ARROW = 9;
    private static final int TAB = 22;
    private static final int TAB_STEP = 24;

    private enum Tab { STRUCTURES, SETTINGS }

    private static Tab tab = Tab.STRUCTURES;

    private final TierPrefs prefs;
    private final GalleryList gallery;
    private final ToolSettingsTab settings;
    @Nullable
    private StructureModel selected;
    @Nullable
    private BuildableStructure selectedBuilder;
    @Nullable
    private GuiStructureRenderer builderRenderer;
    private StructureTierRows tierRows;
    private int layer = -1;
    private EditBox search;
    private String query = "";

    private int storeTop;
    private int bandBottom;
    private int inventoryX;
    private int inventoryY;
    private int feX;
    private int galleryWidth;
    private int previewX;
    private int previewWidth;
    private int previewHeight;
    private int infoY;

    public MultiblockToolScreen(MultiblockToolMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        ItemStack tool = tool();
        this.prefs = ToolData.tiers(tool);
        var id = ToolData.structure(tool);
        this.selected = id == null ? null : StructureManager.STRUCTURES.get(id);
        var builderId = ToolData.builderStructure(tool);
        this.selectedBuilder = builderId == null ? null : BuildableStructureRegistry.CLIENT.get(builderId);
        this.builderRenderer = selectedBuilder == null ? null : GuiStructureRenderer.ofBlocks(selectedBuilder.blocks());
        this.tierRows = new StructureTierRows(selected);
        var font = Minecraft.getInstance().font;
        var entries = new ArrayList<GalleryList.Entry>();
        for (StructureModel model : StructureManager.STRUCTURES.values()) {
            entries.add(GalleryList.Entry.of(model));
        }
        for (BuildableStructure structure : BuildableStructureRegistry.CLIENT.all()) {
            entries.add(GalleryList.Entry.of(structure));
        }
        ResourceLocation selectedId = selected != null ? selected.id() : selectedBuilder != null ? selectedBuilder.id() : null;
        this.gallery = new GalleryList(font, entries, selectedId, selectedBuilder != null, this::select);
        this.settings = new ToolSettingsTab(font, prefs, this::setTier, ToolData.useMe(tool), ToolData.autoCraft(tool),
                () -> ToolData.network(tool()) != null, this::toggleUseMe, this::toggleAutoCraft, this::forgetNetwork);
        GuiStructureRenderer renderer = renderer();
        if (renderer != null) {
            renderer.resetTransforms();
        }
    }

    @Nullable
    private GuiStructureRenderer renderer() {
        return selected != null ? selected.getGuiRenderer() : builderRenderer;
    }

    private boolean hasSelection() {
        return selected != null || selectedBuilder != null;
    }

    private ItemStack tool() {
        return Minecraft.getInstance().player.getItemInHand(menu.getHand());
    }

    @Override
    protected void init() {
        this.imageWidth = Mth.clamp(this.width - 2 * WINDOW_MARGIN - TAB, MIN_WIDTH, MAX_WIDTH);
        this.imageHeight = Mth.clamp(this.height - 2 * WINDOW_MARGIN_Y, MIN_HEIGHT, MAX_HEIGHT);
        super.init();
        this.leftPos = (this.width - this.imageWidth - TAB) / 2;

        storeTop = imageHeight - FRAME_BOTTOM - STORE_HEIGHT;
        inventoryX = imageWidth - INSET - GRID_WIDTH;
        inventoryY = imageHeight - FRAME_BOTTOM - INVENTORY_HEIGHT;
        feX = INSET + GRID_WIDTH + GAP;
        bandBottom = storeTop - 3;
        galleryWidth = Mth.clamp((imageWidth - 2 * INSET) * 3 / 10, GALLERY_MIN, GALLERY_MAX);
        previewX = INSET + galleryWidth + GAP;
        previewWidth = imageWidth - INSET - previewX;
        infoY = bandBottom - INFO_HEIGHT;
        previewHeight = infoY - 3 - TOP;
        placeSlots();

        search = new EditBox(this.font, this.leftPos + INSET + 3, this.topPos + TOP + 3, galleryWidth - 6, SEARCH_HEIGHT,
                Component.translatable("gui.mm.tool.search"));
        search.setHint(Component.translatable("gui.mm.tool.search").withStyle(ChatFormatting.DARK_GRAY));
        search.setMaxLength(64);
        search.setValue(query);
        search.setResponder(text -> {
            query = text;
            gallery.setQuery(text);
        });
        search.visible = tab == Tab.STRUCTURES;
        addRenderableWidget(search);

        int listTop = TOP + 3 + SEARCH_HEIGHT + 3;
        gallery.setBounds(this.leftPos + INSET + 2, this.topPos + listTop, galleryWidth - 4, bandBottom - 2 - listTop);
        gallery.showSelected();
        settings.setBounds(this.leftPos + INSET + 2, this.topPos + TOP + 2, imageWidth - 2 * INSET - 4, bandBottom - TOP - 4);
    }

    private void placeSlots() {
        int store = MultiblockToolMenu.STORE_SLOTS;
        for (int i = 0; i < store; i++) {
            menu.moveSlot(i, INSET + 1 + (i % COLUMNS) * SLOT, storeTop + 1 + (i / COLUMNS) * SLOT);
        }
        for (int i = 0; i < 36; i++) {
            int x = inventoryX + 1 + (i % COLUMNS) * SLOT;
            int y = i < 27 ? inventoryY + 1 + (i / COLUMNS) * SLOT : inventoryY + 59;
            menu.moveSlot(store + i, x, y);
        }
        if (menu.slots.size() > store + 36) {
            menu.moveSlot(store + 36, offhandX() + 1, inventoryY + 59);
        }
    }

    private int offhandX() {
        return inventoryX - GAP - SLOT;
    }

    private void select(GalleryList.Entry entry) {
        if (entry.structure() instanceof BuildableStructure builder) {
            if (selectedBuilder != null && selectedBuilder.id().equals(builder.id())) {
                return;
            }
            selected = null;
            selectedBuilder = builder;
            builderRenderer = GuiStructureRenderer.ofBlocks(builder.blocks());
            tierRows = new StructureTierRows(null);
        } else {
            StructureModel structure = (StructureModel) entry.structure();
            if (selected != null && selected.id().equals(structure.id())) {
                return;
            }
            selected = structure;
            selectedBuilder = null;
            builderRenderer = null;
            tierRows = new StructureTierRows(structure);
        }
        layer = -1;
        renderer().resetTransforms();
        playClick();
        PacketDistributor.sendToServer(new ToolSettingsPkt(ToolSettingsPkt.Action.SELECT_STRUCTURE, entry.id().toString(),
                entry.builder() ? ToolSettingsPkt.BUILDER_STRUCTURE : 0));
    }

    private void setTier(String key, int tier) {
        playClick();
        PacketDistributor.sendToServer(new ToolSettingsPkt(ToolSettingsPkt.Action.SET_TIER, key, tier));
    }

    private void toggleUseMe(boolean value) {
        playClick();
        PacketDistributor.sendToServer(new ToolSettingsPkt(ToolSettingsPkt.Action.SET_USE_ME, "", value ? 1 : 0));
    }

    private void toggleAutoCraft(boolean value) {
        playClick();
        PacketDistributor.sendToServer(new ToolSettingsPkt(ToolSettingsPkt.Action.SET_AUTOCRAFT, "", value ? 1 : 0));
    }

    private void forgetNetwork() {
        playClick();
        PacketDistributor.sendToServer(new ToolSettingsPkt(ToolSettingsPkt.Action.FORGET_NETWORK, "", 0));
    }

    private void playClick() {
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    private void switchTab(Tab next) {
        if (tab == next) {
            return;
        }
        tab = next;
        search.visible = tab == Tab.STRUCTURES;
        if (tab != Tab.STRUCTURES) {
            unfocusSearch();
        }
        playClick();
    }

    private void unfocusSearch() {
        search.setFocused(false);
        if (getFocused() == search) {
            setFocused(null);
        }
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        var texture = Ref.UiTextures.GUI_LARGE;
        NineSliceUtil.blitNineSliced(gfx, texture, x, y, imageWidth, imageHeight, FRAME, FRAME, FRAME, FRAME_BOTTOM, 174, 222, 0, 0, 256, 256);
        gfx.fill(x + FRAME, y + FRAME, x + imageWidth - FRAME, y + imageHeight - FRAME_BOTTOM, GREY);
        drawTabs(gfx, mouseX, mouseY);

        if (tab == Tab.STRUCTURES) {
            drawPanel(gfx, x + INSET, y + TOP, galleryWidth, bandBottom - TOP);
            gallery.render(gfx, mouseX, mouseY);
            drawPanel(gfx, x + previewX, y + TOP, previewWidth, previewHeight);
            drawPreview(gfx, mouseX, mouseY);
            drawPanel(gfx, x + previewX, y + infoY, previewWidth, INFO_HEIGHT);
            drawInfo(gfx);
        } else {
            drawPanel(gfx, x + INSET, y + TOP, imageWidth - 2 * INSET, bandBottom - TOP);
            settings.render(gfx, mouseX, mouseY);
        }
        drawMeStatus(gfx);

        for (int i = 0; i < MultiblockToolMenu.STORE_SLOTS; i++) {
            gfx.blit(Ref.UiTextures.SLOT_PARTS, x + INSET + (i % COLUMNS) * SLOT, y + storeTop + (i / COLUMNS) * SLOT, 0, 26, SLOT, SLOT);
        }
        drawEnergy(gfx);
        gfx.blit(texture, x + inventoryX, y + inventoryY, 6, 139, GRID_WIDTH, INVENTORY_HEIGHT);
        if (menu.slots.size() > MultiblockToolMenu.STORE_SLOTS + 36) {
            gfx.blit(Ref.UiTextures.SLOT_PARTS, x + offhandX(), y + inventoryY + 58, 0, 26, SLOT, SLOT);
        }
    }

    private static void drawPanel(GuiGraphics gfx, int x, int y, int width, int height) {
        NineSliceUtil.blitNineSliced(gfx, Ref.UiTextures.GUI_LARGE, x, y, width, height, 2, 2, 2, 2, 162, 121, 6, 6, 256, 256);
        gfx.fill(x + 2, y + 2, x + width - 2, y + height - 2, PANEL);
    }

    private void drawTabs(GuiGraphics gfx, int mouseX, int mouseY) {
        for (Tab each : Tab.values()) {
            Rect2i area = tabArea(each);
            boolean active = each == tab;
            var texture = active || area.contains(mouseX, mouseY) ? Ref.UiTextures.BUTTON_PRESSED : Ref.UiTextures.BUTTON_ACTIVE;
            NineSliceUtil.blitNineSliced(gfx, texture, area.getX(), area.getY(), area.getWidth(), area.getHeight(), 2, 2, 2, 2, 16, 16, 0, 0, 16, 16);
            ItemStack icon = each == Tab.STRUCTURES ? new ItemStack(MMRegisters.BLUEPRINT.get()) : new ItemStack(Items.COMPARATOR);
            gfx.renderItem(icon, area.getX() + area.getWidth() - TAB + 3, area.getY() + 3);
        }
    }

    private Rect2i tabArea(Tab each) {
        int x = this.leftPos + imageWidth - (each == tab ? 3 : 1);
        int width = TAB + (each == tab ? 2 : 0);
        return new Rect2i(x, this.topPos + TOP + each.ordinal() * TAB_STEP, width, TAB);
    }

    public List<Rect2i> getTabAreas() {
        var areas = new ArrayList<Rect2i>();
        for (Tab each : Tab.values()) {
            areas.add(tabArea(each));
        }
        return areas;
    }

    @Nullable
    private Tab tabAt(double mouseX, double mouseY) {
        for (Tab each : Tab.values()) {
            if (tabArea(each).contains((int) mouseX, (int) mouseY)) {
                return each;
            }
        }
        return null;
    }

    private GuiPos viewport() {
        return GuiPos.of(this.leftPos + previewX + 2, this.topPos + TOP + 2, previewWidth - 4, previewHeight - 4);
    }

    private int layerCount() {
        GuiStructureRenderer renderer = renderer();
        if (renderer == null) {
            return 0;
        }
        renderer.init();
        return renderer.getStructureSize().y + 1;
    }

    private boolean isOnLayerBar(double mouseX, double mouseY) {
        if (layerCount() < 2) {
            return false;
        }
        GuiPos view = viewport();
        return mouseX >= view.x() && mouseX < view.x() + view.w() && mouseY >= view.y() + view.h() - LAYER_HEIGHT && mouseY < view.y() + view.h();
    }

    private boolean isOnView(double mouseX, double mouseY) {
        GuiPos view = viewport();
        return mouseX >= view.x() && mouseX < view.x() + view.w() && mouseY >= view.y() && mouseY < view.y() + view.h()
                && !isOnLayerBar(mouseX, mouseY);
    }

    private void drawPreview(GuiGraphics gfx, int mouseX, int mouseY) {
        GuiPos view = viewport();
        GuiStructureRenderer renderer = renderer();
        if (renderer == null) {
            Component hint = Component.translatable("gui.mm.tool.preview.none");
            int width = this.font.width(hint);
            gfx.drawString(this.font, hint, view.x() + (view.w() - width) / 2, view.y() + view.h() / 2 - 4, LABEL, false);
            return;
        }
        gfx.pose().pushPose();
        gfx.pose().setIdentity();
        renderer.setViewport(view);
        renderer.init();
        renderer.setYSlice(layer >= 0, renderer.getMinBound().y() + layer);
        renderer.render(gfx, mouseX, mouseY, isOnView(mouseX, mouseY));
        gfx.pose().popPose();
        drawLayerBar(gfx, mouseX, mouseY);
    }

    private void drawLayerBar(GuiGraphics gfx, int mouseX, int mouseY) {
        int count = layerCount();
        if (count < 2) {
            return;
        }
        GuiPos view = viewport();
        int x = view.x();
        int y = view.y() + view.h() - LAYER_HEIGHT;
        int width = view.w();
        gfx.fill(x, y, x + width, y + LAYER_HEIGHT, 0x80000000);
        boolean hovered = isOnLayerBar(mouseX, mouseY);
        gfx.drawString(this.font, "<", x + 2, y + 2, hovered && mouseX < x + LAYER_ARROW ? 0xFFFFFF55 : 0xFFA0A0A0, false);
        gfx.drawString(this.font, ">", x + width - LAYER_ARROW + 2, y + 2, hovered && mouseX >= x + width - LAYER_ARROW ? 0xFFFFFF55 : 0xFFA0A0A0, false);
        Component label = layer < 0 ? Component.translatable("jei.mm.structure.layer.all") : Component.translatable("jei.mm.structure.layer", layer + 1, count);
        String text = this.font.plainSubstrByWidth(label.getString(), width - 2 * LAYER_ARROW);
        gfx.drawString(this.font, text, x + (width - this.font.width(text)) / 2, y + 2, 0xFFFFFFFF, false);
    }

    private void stepLayer(int direction) {
        int states = layerCount() + 1;
        layer = Math.floorMod(layer + 1 + direction, states) - 1;
        playClick();
    }

    private void drawInfo(GuiGraphics gfx) {
        int x = this.leftPos + previewX + 4;
        int y = this.topPos + infoY + 3;
        int width = previewWidth - 8;
        if (!hasSelection()) {
            drawClipped(gfx, Component.translatable("tooltip.mm.multiblock_tool.no_structure"), x, y, width, LABEL);
            return;
        }
        if (selectedBuilder != null) {
            drawClipped(gfx, builderInfoLine(), x, y, width, TEXT);
            return;
        }
        MutableComponent line = infoLine().copy().withStyle(Style.EMPTY.withColor(TEXT))
                .append(Component.literal(" · ").withStyle(Style.EMPTY.withColor(LABEL)))
                .append(portsLine());
        drawClipped(gfx, line, x, y, width, TEXT);
    }

    private Component builderInfoLine() {
        Vec3i size = selectedBuilder.size();
        MutableComponent line = Component.translatable("gui.mm.tool.info", selectedBuilder.displayName(), size.getX(), size.getY(), size.getZ(),
                selectedBuilder.blockCount()).withStyle(Style.EMPTY.withColor(TEXT));
        if (!selectedBuilder.buildable()) {
            line.append(Component.literal(" · ").withStyle(Style.EMPTY.withColor(LABEL)))
                    .append(Component.translatable("gui.mm.tool.unbuildable", selectedBuilder.unbuildableBlock().getName())
                            .withStyle(Style.EMPTY.withColor(ADJUSTED)));
        }
        return line;
    }

    private Component infoLine() {
        GuiStructureRenderer renderer = selected.getGuiRenderer();
        renderer.init();
        Vector3i size = renderer.getStructureSize();
        int blocks = selected.layout().getPositionedPieces().size() + 1;
        return Component.translatable("gui.mm.tool.info", selected.name(), size.x + 1, size.y + 1, size.z + 1, blocks);
    }

    private Component portsLine() {
        if (tierRows.isEmpty()) {
            return Component.translatable("gui.mm.tool.ports.fixed").withStyle(Style.EMPTY.withColor(LABEL));
        }
        MutableComponent line = Component.translatable("gui.mm.tool.ports").withStyle(Style.EMPTY.withColor(LABEL));
        boolean first = true;
        for (String key : tierRows.rows().keySet()) {
            if (!first) {
                line.append(Component.literal(" · ").withStyle(Style.EMPTY.withColor(LABEL)));
            }
            first = false;
            line.append(portPart(key));
        }
        return line;
    }

    private Component portPart(String key) {
        int preferred = prefs.get(key);
        Block block = tierRows.chosenBlock(key, preferred);
        MutableComponent part = (block == null ? Component.literal("?") : block.getName().copy());
        if (tierRows.adjusted(key, preferred)) {
            return part.append("*").withStyle(Style.EMPTY.withColor(ADJUSTED));
        }
        return part.withStyle(Style.EMPTY.withColor(TEXT));
    }

    private boolean isOnPortsLine(double mouseX, double mouseY) {
        int x = this.leftPos + previewX;
        int y = this.topPos + infoY;
        return tab == Tab.STRUCTURES && selected != null && mouseX >= x && mouseX < x + previewWidth && mouseY >= y && mouseY < y + INFO_HEIGHT;
    }

    private List<Component> portsTooltip() {
        var lines = new ArrayList<Component>();
        lines.add(infoLine());
        boolean anyAdjusted = false;
        for (String key : tierRows.rows().keySet()) {
            lines.add(portPart(key));
            anyAdjusted |= tierRows.adjusted(key, prefs.get(key));
        }
        if (anyAdjusted) {
            lines.add(Component.translatable("gui.mm.assemble.adjusted").withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    private int energyStored() {
        return new ToolEnergy(tool(), MMConfig.TOOL_ENERGY_CAPACITY).getEnergyStored();
    }

    private void drawEnergy(GuiGraphics gfx) {
        int x = this.leftPos + feX;
        int y = this.topPos + storeTop;
        gfx.fill(x, y, x + FE_WIDTH, y + STORE_HEIGHT, 0xFF373737);
        gfx.fill(x + 1, y + 1, x + FE_WIDTH - 1, y + STORE_HEIGHT - 1, PANEL);
        int capacity = MMConfig.TOOL_ENERGY_CAPACITY;
        int inner = STORE_HEIGHT - 2;
        int filled = capacity <= 0 ? 0 : (int) Math.round((double) inner * energyStored() / capacity);
        if (filled > 0) {
            gfx.fillGradient(x + 1, y + 1 + inner - filled, x + FE_WIDTH - 1, y + STORE_HEIGHT - 1, 0xFFFF5555, 0xFFAA0000);
        }
    }

    private boolean isOnEnergy(double mouseX, double mouseY) {
        int x = this.leftPos + feX;
        int y = this.topPos + storeTop;
        return mouseX >= x && mouseX < x + FE_WIDTH && mouseY >= y && mouseY < y + STORE_HEIGHT;
    }

    private Component meStatus() {
        var network = ToolData.network(tool());
        return network == null ? Component.translatable("gui.mm.tool.me.not_bound")
                : Component.translatable("gui.mm.tool.me.bound", network.pos().toShortString(), network.dimension().location().getPath());
    }

    private int meStatusTop() {
        return inventoryY - ME_GAP - ME_HEIGHT;
    }

    private void drawMeStatus(GuiGraphics gfx) {
        if (!NetworkLink.AVAILABLE) {
            return;
        }
        int x = this.leftPos + inventoryX;
        int y = this.topPos + meStatusTop() + 1;
        drawClipped(gfx, meStatus(), x, y, GRID_WIDTH, LABEL);
    }

    private boolean isOnMeStatus(double mouseX, double mouseY) {
        if (!NetworkLink.AVAILABLE) {
            return false;
        }
        int x = this.leftPos + inventoryX;
        int y = this.topPos + meStatusTop();
        return mouseX >= x && mouseX < x + GRID_WIDTH && mouseY >= y && mouseY < y + ME_HEIGHT;
    }

    private void drawClipped(GuiGraphics gfx, Component text, int x, int y, int maxWidth, int color) {
        TextRenderUtil.drawClipped(gfx, this.font, text, x, y, maxWidth, color);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics gfx, int mouseX, int mouseY) {
        drawClipped(gfx, this.title, INSET, 6, imageWidth - 2 * INSET, TITLE);
    }

    @Override
    protected void renderSlot(@NotNull GuiGraphics gfx, @NotNull Slot slot) {
        ItemStack stack = slot.getItem();
        if (!(slot instanceof ToolSlot) || stack.getCount() < 100) {
            super.renderSlot(gfx, slot);
            return;
        }
        gfx.renderItem(stack, slot.x, slot.y, slot.x + slot.y * this.imageWidth);
        gfx.renderItemDecorations(this.font, stack, slot.x, slot.y, "");
        CountFormat.drawSlotCount(gfx, slot.x - 1, slot.y - 1, stack.getCount());
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx, mouseX, mouseY, partialTick);
        super.render(gfx, mouseX, mouseY, partialTick);
        renderTooltip(gfx, mouseX, mouseY);
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            return;
        }
        List<Component> tooltip = null;
        Tab hoveredTab = tabAt(mouseX, mouseY);
        if (this.hoveredSlot instanceof ToolSlot && this.menu.getCarried().isEmpty()) {
            tooltip = List.of(Component.translatable("gui.mm.tool.store"));
        } else if (hoveredTab != null) {
            tooltip = List.of(Component.translatable(hoveredTab == Tab.STRUCTURES ? "gui.mm.tool.tab.structures" : "gui.mm.tool.tab.settings"));
        } else if (isOnEnergy(mouseX, mouseY)) {
            tooltip = List.of(Component.translatable("tooltip.mm.multiblock_tool.energy",
                    CountFormat.grouped(energyStored()), CountFormat.grouped(MMConfig.TOOL_ENERGY_CAPACITY)));
        } else if (isOnMeStatus(mouseX, mouseY)) {
            tooltip = List.of(meStatus());
        } else if (tab == Tab.SETTINGS) {
            tooltip = settings.tooltip(mouseX, mouseY);
        } else if (isOnLayerBar(mouseX, mouseY)) {
            tooltip = List.of(Component.translatable("jei.mm.structure.layer.hint"));
        } else if (isOnPortsLine(mouseX, mouseY)) {
            tooltip = portsTooltip();
        } else if (!search.isMouseOver(mouseX, mouseY)) {
            tooltip = gallery.tooltip(mouseX, mouseY);
        }
        if (tooltip != null) {
            gfx.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!search.isMouseOver(mouseX, mouseY)) {
            unfocusSearch();
        }
        Tab clickedTab = tabAt(mouseX, mouseY);
        if (clickedTab != null) {
            switchTab(clickedTab);
            return true;
        }
        if (tab == Tab.SETTINGS) {
            if (settings.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        } else {
            if (button == 0 && isOnLayerBar(mouseX, mouseY)) {
                GuiPos view = viewport();
                if (mouseX < view.x() + LAYER_ARROW) {
                    stepLayer(-1);
                } else if (mouseX >= view.x() + view.w() - LAYER_ARROW) {
                    stepLayer(1);
                }
                return true;
            }
            if (gallery.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton) && tabAt(mouseX, mouseY) == null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double delta) {
        if (tab == Tab.SETTINGS) {
            if (settings.mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
        } else {
            if (isOnLayerBar(mouseX, mouseY)) {
                stepLayer(delta > 0 ? 1 : -1);
                return true;
            }
            if (renderer() != null && isOnView(mouseX, mouseY)) {
                renderer().zoom(delta);
                return true;
            }
            if (gallery.mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (search.visible && search.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                unfocusSearch();
                return true;
            }
            search.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}

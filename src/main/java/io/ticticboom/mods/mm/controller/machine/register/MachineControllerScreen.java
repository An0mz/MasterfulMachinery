package io.ticticboom.mods.mm.controller.machine.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.util.TextRenderUtil;
import io.ticticboom.mods.mm.net.packet.SelectRecipePkt;
import io.ticticboom.mods.mm.port.item.SingleItemPortIngredient;
import io.ticticboom.mods.mm.recipe.MachineRecipeManager;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.output.simple.SimpleRecipeOutputEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public class MachineControllerScreen extends AbstractContainerScreen<MachineControllerMenu> {

    private final MachineControllerMenu menu;
    private final MachineControllerBlockEntity be;
    private final FormattedText header;
    private final int redstoneBtnX = 10;
    private final int redstoneBtnY = 80;
    private static final int RECIPE_ROW_Y = 93;
    private static final int PREV_X = 10;
    private static final int NEXT_X = 158;
    private static final int ICON_X = 22;
    private static final int NAME_X = 42;
    private static final float MIN_TEXT_SCALE = 0.6f;

    public MachineControllerScreen(MachineControllerMenu menu, Inventory inv, Component p_96550_) {
        super(menu, inv, p_96550_);
        this.menu = menu;
        this.be = (MachineControllerBlockEntity) menu.getBe();
        this.imageHeight = 222;
        this.imageWidth = 174;
        String name = menu.getModel().displayName().getString();
        int subStrLength = Math.min(55, name.length());
        header = FormattedText.of(name.substring(0, subStrLength) + (subStrLength < 55 ? "" : "..."));
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        gfx.blit(Ref.UiTextures.GUI_LARGE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        // controller name
        gfx.drawWordWrap(this.font, header, 10, 10, 150, 0xacacac);

        // structure formation details
        var isFormed = be.getStructure() != null;
        gfx.drawWordWrap(this.font, Component.translatable(isFormed ? "gui.mm.controller.formed_as" : "gui.mm.controller.not_formed"), 10, 40, 150, 0xacacac);
        if (isFormed) {
            gfx.drawWordWrap(this.font, be.getStructure().displayName(), 10, 53, 150, 0xacacac);
        }

        // show max parallel recipes under the multiblock name when formed
        if (isFormed) {
            int structVal = be.getStructure().maxParallelRecipes();
            int displayInt = 1;
            if (structVal > 0) {
                displayInt = structVal;
            } else {
                var controllerModel = io.ticticboom.mods.mm.setup.loader.ControllerLoader.CONTROLLER_MODELS.get(menu.getModel().id());
                if (controllerModel != null && controllerModel.maxParallelRecipes() > 0) {
                    displayInt = controllerModel.maxParallelRecipes();
                }
            }
            String line = Component.translatable("gui.mm.controller.max_parallel", displayInt).getString();
            int nameY = 53;
            int lineHeight = this.font.lineHeight;
            int subY = nameY + lineHeight + 10;
            // smaller text by scaling
            gfx.pose().pushPose();
            gfx.pose().translate(10f, (float) subY, 0f);
            float scale = 0.65f;
            gfx.pose().scale(scale, scale, 1f);
            int wrapWidth = (int) (150 / scale);
            TextRenderUtil.renderWordWrapLimit(gfx, line, 0, 0, wrapWidth, 1, 0xacacac);
            gfx.pose().popPose();
        }

        // recipe processing details
        var isProcessing = be.getRecipeState() != null;
        if (isProcessing) {
            gfx.drawWordWrap(this.font,
                    Component.translatable("gui.mm.controller.progress",
                            String.format("%.2f", be.getRecipeState().getTickPercentage())),
                    10, 110, 150, 0xacacac);
        }

        // redstone mode toggle label (clickable)
        Component rs = Component.translatable("gui.mm.controller.redstone", be.getRedstoneModeName());
        gfx.drawString(this.font, rs, redstoneBtnX, redstoneBtnY, 0xacacac, false);

        if (be.isManualSelection()) {
            renderRecipePicker(gfx);
        }
    }

    private List<RecipeModel> selectableRecipes() {
        if (be.getStructure() == null) {
            return List.of();
        }
        return MachineRecipeManager.getRecipesByStrucutreId(be.getStructure().id()).stream()
                .sorted(Comparator.comparing(recipe -> recipe.id().toString()))
                .toList();
    }

    private RecipeModel selectedRecipe() {
        for (RecipeModel recipe : selectableRecipes()) {
            if (recipe.id().equals(be.getSelectedRecipeId())) {
                return recipe;
            }
        }
        return null;
    }

    private static Component recipeName(RecipeModel recipe) {
        if (recipe == null) {
            return Component.translatable("gui.mm.controller.recipe_none");
        }
        var icon = displayStack(recipe);
        return icon.isEmpty() ? outputName(recipe) : icon.getHoverName();
    }

    private void renderRecipePicker(GuiGraphics gfx) {
        var selected = selectedRecipe();
        gfx.drawString(this.font, "\u25C0", PREV_X, RECIPE_ROW_Y + 4, 0xacacac, false);
        gfx.drawString(this.font, "\u25B6", NEXT_X, RECIPE_ROW_Y + 4, 0xacacac, false);
        int nameX = ICON_X;
        if (selected != null) {
            var icon = displayStack(selected);
            if (!icon.isEmpty()) {
                gfx.renderItem(icon, ICON_X, RECIPE_ROW_Y);
                gfx.renderItemDecorations(this.font, icon, ICON_X, RECIPE_ROW_Y);
                nameX = NAME_X;
            }
        }
        drawFitted(gfx, recipeName(selected).getString(), nameX, RECIPE_ROW_Y + 4, NEXT_X - nameX - 4);
    }

    private void drawFitted(GuiGraphics gfx, String text, int x, int y, int maxWidth) {
        int width = this.font.width(text);
        if (width <= maxWidth) {
            gfx.drawString(this.font, text, x, y, 0xacacac, false);
            return;
        }
        float scale = Math.max(MIN_TEXT_SCALE, (float) maxWidth / width);
        String shown = this.font.plainSubstrByWidth(text, (int) (maxWidth / scale));
        gfx.pose().pushPose();
        gfx.pose().translate(x, y + (1 - scale) * this.font.lineHeight / 2f, 0);
        gfx.pose().scale(scale, scale, 1f);
        gfx.drawString(this.font, shown, 0, 0, 0xacacac, false);
        gfx.pose().popPose();
    }

    private static Component outputName(RecipeModel recipe) {
        for (var output : recipe.outputs().outputs()) {
            if (output instanceof SimpleRecipeOutputEntry simple) {
                var name = simple.getIngredient().displayName();
                if (name != null) {
                    return name;
                }
            }
        }
        return Component.literal(recipe.id().getPath());
    }

    private static ItemStack displayStack(RecipeModel recipe) {
        for (var output : recipe.outputs().outputs()) {
            if (output instanceof SimpleRecipeOutputEntry simple && simple.getIngredient() instanceof SingleItemPortIngredient item) {
                return new ItemStack(BuiltInRegistries.ITEM.get(item.getItemId()), Math.max(1, item.getCount()));
            }
        }
        return ItemStack.EMPTY;
    }

    private void cycleRecipe(int step) {
        var recipes = selectableRecipes();
        int index = -1;
        for (int i = 0; i < recipes.size(); i++) {
            if (recipes.get(i).id().equals(be.getSelectedRecipeId())) {
                index = i;
            }
        }
        int next = Math.floorMod(index + 1 + step, recipes.size() + 1) - 1;
        String id = next < 0 ? "" : recipes.get(next).id().toString();
        PacketDistributor.sendToServer(new SelectRecipePkt(be.getBlockPos(), id));
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        renderBackground(gfx, mouseX, mouseY, partial);
        super.render(gfx, mouseX, mouseY, partial);
        renderTooltip(gfx, mouseX, mouseY);
        if (be.isManualSelection()) {
            double mx = mouseX - this.leftPos;
            double my = mouseY - this.topPos;
            if (mx >= ICON_X && mx <= NEXT_X - 2 && my >= RECIPE_ROW_Y && my <= RECIPE_ROW_Y + 16) {
                gfx.renderTooltip(this.font, Component.translatable("gui.mm.controller.recipe", recipeName(selectedRecipe())), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double mx = mouseX - this.leftPos;
        double my = mouseY - this.topPos;
        int redstoneBtnW = 150;
        int redstoneBtnH = 12;
        if (mx >= redstoneBtnX && mx <= redstoneBtnX + redstoneBtnW && my >= redstoneBtnY && my <= redstoneBtnY + redstoneBtnH) {
            try {
                BlockEntity beEntity = menu.getBe().getBlockEntity();
                var pos = beEntity.getBlockPos();
                int next = (be.getRedstoneModeOrdinal() + 1) % 3;
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new io.ticticboom.mods.mm.net.packet.ToggleRedstoneModePkt(pos, next));
            } catch (Throwable ignored) { }
            return true;
        }
        if (be.isManualSelection() && my >= RECIPE_ROW_Y && my <= RECIPE_ROW_Y + 16) {
            if (mx >= PREV_X - 2 && mx <= PREV_X + 8) {
                cycleRecipe(-1);
                return true;
            }
            if (mx >= NEXT_X - 2 && mx <= NEXT_X + 8) {
                cycleRecipe(1);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}

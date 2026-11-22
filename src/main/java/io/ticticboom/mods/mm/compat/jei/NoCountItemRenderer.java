package io.ticticboom.mods.mm.compat.jei;

import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class NoCountItemRenderer implements IIngredientRenderer<ItemStack> {

    private final IIngredientRenderer<ItemStack> delegate;

    public NoCountItemRenderer(IIngredientRenderer<ItemStack> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void render(GuiGraphics guiGraphics, ItemStack ingredient) {
        guiGraphics.renderItem(ingredient, 0, 0);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, ItemStack ingredient, TooltipFlag tooltipFlag) {
        delegate.getTooltip(tooltip, ingredient, tooltipFlag);
    }

    @SuppressWarnings({"deprecation", "removal"})
    @Override
    public List<Component> getTooltip(ItemStack ingredient, TooltipFlag tooltipFlag) {
        return delegate.getTooltip(ingredient, tooltipFlag);
    }

    @Override
    public Font getFontRenderer(Minecraft minecraft, ItemStack ingredient) {
        return delegate.getFontRenderer(minecraft, ingredient);
    }
}

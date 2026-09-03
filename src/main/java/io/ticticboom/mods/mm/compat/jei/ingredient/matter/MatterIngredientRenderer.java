package io.ticticboom.mods.mm.compat.jei.ingredient.matter;

import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MatterIngredientRenderer implements IIngredientRenderer<MatterIngredient> {

    @Override
    public void render(GuiGraphics gfx, @NotNull MatterIngredient ingredient) {
        gfx.fill(0, 0, 16, 16, 0xFF1B1B1B);
        gfx.fill(1, 1, 15, 15, ingredient.color() | 0xFF000000);
    }

    @SuppressWarnings("removal")
    @Override
    public @NotNull List<Component> getTooltip(MatterIngredient ingredient, @NotNull TooltipFlag tooltipFlag) {
        var result = new ArrayList<Component>();
        result.add(Component.translatable("jei.mm.ingredient.replication_matter.title", MatterIngredientNames.displayName(ingredient.matter())));
        result.add(Component.translatable("jei.mm.ingredient.replication_matter.amount", ingredient.amount()));
        return result;
    }
}

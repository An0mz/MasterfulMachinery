package io.ticticboom.mods.mm.compat.jei.ingredient.starlight;

import io.ticticboom.mods.mm.compat.astralsorcery.AstralSorceryNames;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class StarlightIngredientRenderer implements IIngredientRenderer<StarlightIngredient> {

    public static Component title(StarlightIngredient ingredient) {
        if (ingredient.constellation() == null) {
            return Component.translatable("port.mm.astral_starlight.name");
        }
        return Component.translatable("port.mm.astral_starlight.of", AstralSorceryNames.constellation(ingredient.constellation()));
    }

    @Override
    public void render(GuiGraphics gfx, @NotNull StarlightIngredient ingredient) {
        gfx.fill(0, 0, 16, 16, 0xFF0B1033);
        gfx.fill(1, 1, 15, 15, 0xFF1C2A6B);
        gfx.fill(7, 3, 9, 13, 0xFFE8F0FF);
        gfx.fill(3, 7, 13, 9, 0xFFE8F0FF);
        gfx.fill(6, 6, 10, 10, 0xFFFFFFFF);
        gfx.fill(3, 3, 4, 4, 0xFF9DB4FF);
        gfx.fill(12, 12, 13, 13, 0xFF9DB4FF);
        gfx.fill(12, 3, 13, 4, 0xFF9DB4FF);
    }

    @SuppressWarnings("removal")
    @Override
    public @NotNull List<Component> getTooltip(StarlightIngredient ingredient, @NotNull TooltipFlag tooltipFlag) {
        var amount = ingredient.min() == ingredient.max()
                ? Component.translatable("jei.mm.ingredient.astral_starlight.amount", ingredient.max())
                : Component.translatable("jei.mm.ingredient.astral_starlight.range", ingredient.min(), ingredient.max());
        return List.of(title(ingredient).copy().withStyle(ChatFormatting.AQUA), amount);
    }
}

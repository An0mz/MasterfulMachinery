package io.ticticboom.mods.mm.compat.jei.ingredient.ether;

import io.ticticboom.mods.mm.util.NumberText;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EtherIngredientRenderer implements IIngredientRenderer<EtherStack> {

    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath("etheria", "ether_orb");

    private ItemStack icon;

    @Override
    public void render(GuiGraphics guiGraphics, @NotNull EtherStack stack) {
        if (icon == null) {
            icon = BuiltInRegistries.ITEM.get(ICON).getDefaultInstance();
        }
        guiGraphics.renderFakeItem(icon, 0, 0);
    }

    @SuppressWarnings("removal")
    @Override
    public @NotNull List<Component> getTooltip(EtherStack stack, @NotNull TooltipFlag tooltipFlag) {
        var amount = stack.min() == stack.max()
                ? Component.translatable("jei.mm.ingredient.etheria_ether.amount", NumberText.grouped(stack.max()))
                : Component.translatable("jei.mm.ingredient.etheria_ether.range", NumberText.grouped(stack.min()), NumberText.grouped(stack.max()));
        return List.of(Component.translatable("jei.mm.ingredient.etheria_ether.title"), amount);
    }
}

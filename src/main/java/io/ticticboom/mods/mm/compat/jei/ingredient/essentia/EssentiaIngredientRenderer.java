package io.ticticboom.mods.mm.compat.jei.ingredient.essentia;

import com.leclowndu93150.thaumaturge.api.client.AspectRendering;
import io.ticticboom.mods.mm.port.thaumaturge.essentia.EssentiaAspects;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EssentiaIngredientRenderer implements IIngredientRenderer<EssentiaStack> {

    @Override
    public void render(GuiGraphics guiGraphics, @NotNull EssentiaStack stack) {
        var holder = EssentiaAspects.holder(EssentiaAspects.key(stack.aspect()));
        if (holder == null) {
            AspectRendering.renderMissingGui(guiGraphics, 0, 0);
            return;
        }
        AspectRendering.renderGui(guiGraphics, Minecraft.getInstance().font, 0, 0, holder, 0);
    }

    @SuppressWarnings("removal")
    @Override
    public @NotNull List<Component> getTooltip(EssentiaStack stack, @NotNull TooltipFlag tooltipFlag) {
        var amount = stack.min() == stack.max()
                ? Component.translatable("jei.mm.ingredient.thaumaturge_essentia.amount", stack.max())
                : Component.translatable("jei.mm.ingredient.thaumaturge_essentia.range", stack.min(), stack.max());
        return List.of(EssentiaAspects.name(EssentiaAspects.key(stack.aspect())), amount);
    }
}

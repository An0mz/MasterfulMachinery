package io.ticticboom.mods.mm.compat.jei.ingredient.essentia;

import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import io.ticticboom.mods.mm.port.thaumaturge.essentia.EssentiaAspects;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class EssentiaIngredientHelper implements IIngredientHelper<EssentiaStack> {
    @Override
    public IIngredientType<EssentiaStack> getIngredientType() {
        return MMJeiIngredients.THAUMATURGE_ESSENTIA;
    }

    @Override
    public String getDisplayName(EssentiaStack stack) {
        return EssentiaAspects.name(EssentiaAspects.key(stack.aspect())).getString();
    }

    @Override
    public String getUniqueId(EssentiaStack stack, UidContext uidContext) {
        return "thaumaturge/essentia/" + stack.aspect();
    }

    @Override
    public ResourceLocation getResourceLocation(EssentiaStack stack) {
        return stack.aspect();
    }

    @Override
    public EssentiaStack copyIngredient(EssentiaStack stack) {
        return new EssentiaStack(stack.aspect(), stack.min(), stack.max());
    }

    @Override
    public String getErrorInfo(@Nullable EssentiaStack stack) {
        return stack == null ? "null essentia" : stack.toString();
    }
}

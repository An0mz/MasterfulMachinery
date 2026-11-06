package io.ticticboom.mods.mm.compat.jei.ingredient.essentia;

import mezz.jei.api.ingredients.IIngredientType;

public class EssentiaIngredientType implements IIngredientType<EssentiaStack> {
    @Override
    public Class<? extends EssentiaStack> getIngredientClass() {
        return EssentiaStack.class;
    }
}

package io.ticticboom.mods.mm.compat.jei.ingredient.ether;

import mezz.jei.api.ingredients.IIngredientType;

public class EtherIngredientType implements IIngredientType<EtherStack> {
    @Override
    public Class<? extends EtherStack> getIngredientClass() {
        return EtherStack.class;
    }
}

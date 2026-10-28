package io.ticticboom.mods.mm.compat.jei.ingredient.ether;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class EtherIngredientHelper implements IIngredientHelper<EtherStack> {
    @Override
    public IIngredientType<EtherStack> getIngredientType() {
        return MMJeiIngredients.ETHERIA_ETHER;
    }

    @Override
    public String getDisplayName(EtherStack stack) {
        return "Ether";
    }

    @Override
    public String getUniqueId(EtherStack stack, UidContext uidContext) {
        return "etheria/ether";
    }

    @Override
    public ResourceLocation getResourceLocation(EtherStack stack) {
        return Ref.id("etheria/ether");
    }

    @Override
    public EtherStack copyIngredient(EtherStack stack) {
        return new EtherStack(stack.min(), stack.max());
    }

    @Override
    public String getErrorInfo(@Nullable EtherStack stack) {
        return "ERR";
    }
}

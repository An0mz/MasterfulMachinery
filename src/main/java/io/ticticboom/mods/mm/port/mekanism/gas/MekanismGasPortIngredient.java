package io.ticticboom.mods.mm.port.mekanism.gas;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortIngredient;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorage;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import mekanism.api.chemical.ChemicalStack;
import mekanism.client.recipe_viewer.jei.MekanismJEI;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class MekanismGasPortIngredient extends MekanismChemicalPortIngredient {

    public MekanismGasPortIngredient(ResourceLocation chemical, long amount) {
        super(chemical, amount);
    }

    @Override
    public Class<? extends MekanismChemicalPortStorage> getStorageClass() {
        return MekanismGasPortStorage.class;
    }

    @Override
    public ResourceLocation getTypeId() {
        return Ref.Ports.MEK_GAS;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid, IRecipeSlotBuilder recipeSlot) {
        ChemicalStack s = createStack(chemical, amount);
        s.setAmount(1000);
        recipeSlot.addIngredient(MekanismJEI.TYPE_CHEMICAL, s);
        super.setRecipe(builder, model, focus, helpers, grid, recipeSlot);
    }
}

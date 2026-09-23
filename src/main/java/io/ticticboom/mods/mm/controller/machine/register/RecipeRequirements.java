package io.ticticboom.mods.mm.controller.machine.register;

import io.ticticboom.mods.mm.port.energy.EnergyPortIngredient;
import io.ticticboom.mods.mm.port.fluid.FluidPortIngredient;
import io.ticticboom.mods.mm.port.item.SingleItemPortIngredient;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.input.consume.ConsumeRecipeIngredientEntry;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

record RecipeRequirements(Set<ResourceLocation> itemIds, Set<ResourceLocation> fluidIds, boolean energy,
                          @Nullable ResourceLocation primaryItem) {

    static RecipeRequirements of(RecipeModel recipe) {
        var items = new HashSet<ResourceLocation>();
        var fluids = new HashSet<ResourceLocation>();
        boolean energy = false;
        ResourceLocation primary = null;
        for (var input : recipe.inputs().inputs()) {
            if (!(input instanceof ConsumeRecipeIngredientEntry entry)) {
                continue;
            }
            var ingredient = entry.getIngredient();
            if (ingredient instanceof SingleItemPortIngredient item && item.getItemId() != null) {
                items.add(item.getItemId());
                if (primary == null) {
                    primary = item.getItemId();
                }
            } else if (ingredient instanceof FluidPortIngredient fluid && fluid.getFluidId() != null) {
                fluids.add(fluid.getFluidId());
            } else if (ingredient instanceof EnergyPortIngredient) {
                energy = true;
            }
        }
        return new RecipeRequirements(Set.copyOf(items), Set.copyOf(fluids), energy, primary);
    }
}

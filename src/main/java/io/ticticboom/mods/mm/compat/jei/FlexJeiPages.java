package io.ticticboom.mods.mm.compat.jei;

import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.input.flex.FlexRecipeIngredientEntry;

import java.util.ArrayList;
import java.util.List;

public final class FlexJeiPages {

    private FlexJeiPages() {
    }

    public static List<RecipeModel> expand(RecipeModel recipe) {
        FlexRecipeIngredientEntry flex = null;
        for (var input : recipe.inputs().inputs()) {
            if (input instanceof FlexRecipeIngredientEntry entry) {
                flex = entry;
                break;
            }
        }
        if (flex == null || recipe.config() == null) {
            return List.of(recipe);
        }
        var pages = flex.jeiPages();
        boolean tiered = false;
        for (var page : pages) {
            if (page.tier() >= 0) {
                tiered = true;
                break;
            }
        }
        if (pages.size() <= 1 && !tiered) {
            return List.of(recipe);
        }
        var copies = new ArrayList<RecipeModel>();
        for (int i = 0; i < pages.size(); i++) {
            var config = recipe.config().deepCopy();
            config.addProperty(FlexRecipeIngredientEntry.JEI_PAGE, i);
            config.addProperty(FlexRecipeIngredientEntry.JEI_PAGE_TIER, pages.get(i).tier());
            config.addProperty(FlexRecipeIngredientEntry.JEI_PAGE_PART, pages.get(i).part());
            copies.add(new RecipeModel(recipe.id(), recipe.structureId(), recipe.ticks(), recipe.inputs(), recipe.outputs(),
                    new ArrayList<>(), new ArrayList<>(), recipe.conditions(), recipe.parallelProcessing(), recipe.requestOnly(), config));
        }
        return copies;
    }
}

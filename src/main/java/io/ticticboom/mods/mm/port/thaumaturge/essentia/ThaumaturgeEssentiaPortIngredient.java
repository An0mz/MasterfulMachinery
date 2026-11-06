package io.ticticboom.mods.mm.port.thaumaturge.essentia;

import io.ticticboom.mods.mm.port.PortContent;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import io.ticticboom.mods.mm.compat.jei.ingredient.essentia.EssentiaStack;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.util.AmountRange;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class ThaumaturgeEssentiaPortIngredient implements IPortIngredient {

    private final ResourceKey<IAspect> aspect;
    private final AmountRange essentia;

    public ThaumaturgeEssentiaPortIngredient(ResourceLocation aspect, AmountRange essentia) {
        this.aspect = EssentiaAspects.key(aspect);
        this.essentia = essentia;
    }

    public ResourceKey<IAspect> getAspect() {
        return aspect;
    }

    private int amount(RecipeStateModel state) {
        return state == null ? essentia.max() : essentia.resolve(state);
    }

    @Override
    public AmountRange getAmountRange() {
        return essentia;
    }

    @Override
    public boolean canProcess(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (storages == null) {
            return false;
        }
        int remaining = amount(state);
        for (var input : storages.getInputStorages(ThaumaturgeEssentiaPortStorage.class)) {
            remaining -= input.extract(aspect, remaining, true);
            if (remaining <= 0) {
                return true;
            }
        }
        return remaining <= 0;
    }

    @Override
    public void process(Level level, RecipeStorages storages, RecipeStateModel state) {
        int remaining = amount(state);
        for (var input : storages.getInputStorages(ThaumaturgeEssentiaPortStorage.class)) {
            remaining -= input.extract(aspect, remaining, false);
            if (remaining <= 0) {
                return;
            }
        }
    }

    @Override
    public void processTick(Level level, RecipeStorages storages, RecipeStateModel state) {
        process(level, storages, state);
    }

    @Override
    public boolean canOutput(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (storages == null) {
            return false;
        }
        int remaining = amount(state);
        for (var output : storages.getOutputStoragesByPriority(ThaumaturgeEssentiaPortStorage.class)) {
            remaining -= output.insert(aspect, remaining, true);
            if (remaining <= 0) {
                return true;
            }
        }
        return remaining <= 0;
    }

    @Override
    public void output(Level level, RecipeStorages storages, RecipeStateModel state) {
        int remaining = amount(state);
        for (var output : storages.getOutputStoragesByPriority(ThaumaturgeEssentiaPortStorage.class)) {
            remaining -= output.insert(aspect, remaining, false);
            if (remaining <= 0) {
                return;
            }
        }
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid, IRecipeSlotBuilder recipeSlot) {
        recipeSlot.addIngredient(MMJeiIngredients.THAUMATURGE_ESSENTIA, new EssentiaStack(aspect.location(), essentia.min(), essentia.max()));
    }

    @Override
    public Component displayName() {
        return EssentiaAspects.name(aspect);
    }

    @Override
    public JsonObject debugInput(Level level, RecipeStorages storages, JsonObject json) {
        json.addProperty("ingredientType", Ref.Ports.THAUMATURGE_ESSENTIA.toString());
        json.addProperty("aspect", aspect.location().toString());
        essentia.addToDebug(json, "amountToExtract", null);
        var searched = new JsonArray();
        for (var input : storages.getInputStorages(ThaumaturgeEssentiaPortStorage.class)) {
            searched.add(input.getStorageUid().toString());
        }
        json.add("searchedStorages", searched);
        json.addProperty("canRun", canProcess(level, storages, null));
        return json;
    }

    @Override
    public JsonObject debugOutput(Level level, RecipeStorages storages, JsonObject json) {
        json.addProperty("ingredientType", Ref.Ports.THAUMATURGE_ESSENTIA.toString());
        json.addProperty("aspect", aspect.location().toString());
        essentia.addToDebug(json, "amountToInsert", null);
        var searched = new JsonArray();
        for (var output : storages.getOutputStoragesByPriority(ThaumaturgeEssentiaPortStorage.class)) {
            searched.add(output.getStorageUid().toString());
        }
        json.add("searchedStorages", searched);
        json.addProperty("canRun", canOutput(level, storages, null));
        return json;
    }

    @Override
    public PortContent display() {
        return PortContent.gauge(EssentiaAspects.name(aspect), EssentiaAspects.color(null, aspect), essentia.max(), 0, "");
    }
}

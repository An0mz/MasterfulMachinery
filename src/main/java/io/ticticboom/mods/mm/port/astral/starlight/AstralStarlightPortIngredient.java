package io.ticticboom.mods.mm.port.astral.starlight;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.astralsorcery.AstralSorceryNames;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import io.ticticboom.mods.mm.compat.jei.ingredient.starlight.StarlightIngredient;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.PortContent;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.util.AmountRange;
import lombok.Getter;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class AstralStarlightPortIngredient implements IPortIngredient {

    @Getter
    @Nullable
    private final ResourceLocation constellation;
    private final AmountRange starlight;

    public AstralStarlightPortIngredient(@Nullable ResourceLocation constellation, AmountRange starlight) {
        this.constellation = constellation;
        this.starlight = starlight;
    }

    private int amount(RecipeStateModel state) {
        return state == null ? starlight.max() : starlight.resolve(state);
    }

    @Override
    public AmountRange getAmountRange() {
        return starlight;
    }

    private double drain(RecipeStorages storages, RecipeStateModel state, boolean simulate) {
        double remaining = amount(state);
        for (AstralStarlightPortStorage input : storages.getInputStorages(AstralStarlightPortStorage.class)) {
            if (remaining <= 0) {
                break;
            }
            remaining -= input.extract(constellation, remaining, simulate);
        }
        return remaining;
    }

    @Override
    public boolean canProcess(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (storages == null) {
            return false;
        }
        return drain(storages, state, true) <= 1e-6;
    }

    @Override
    public void process(Level level, RecipeStorages storages, RecipeStateModel state) {
        drain(storages, state, false);
    }

    @Override
    public void processTick(Level level, RecipeStorages storages, RecipeStateModel state) {
        process(level, storages, state);
    }

    @Override
    public boolean canOutput(Level level, RecipeStorages storages, RecipeStateModel state) {
        return false;
    }

    @Override
    public void output(Level level, RecipeStorages storages, RecipeStateModel state) {
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid, IRecipeSlotBuilder recipeSlot) {
        recipeSlot.addIngredient(MMJeiIngredients.ASTRAL_STARLIGHT,
                new StarlightIngredient(constellation, starlight.min(), starlight.max()));
    }

    @Override
    public Component displayName() {
        if (constellation != null) {
            return Component.translatable("port.mm.astral_starlight.of", AstralSorceryNames.constellation(constellation));
        }
        return Component.translatable("port.mm.astral_starlight.name");
    }

    @Override
    public JsonObject debugInput(Level level, RecipeStorages storages, JsonObject json) {
        json.addProperty("ingredientType", Ref.Ports.ASTRAL_STARLIGHT.toString());
        json.addProperty("constellation", constellation == null ? "any" : constellation.toString());
        starlight.addToDebug(json, "amountToExtract", null);
        json.addProperty("canRun", canProcess(level, storages, null));
        return json;
    }

    @Override
    public JsonObject debugOutput(Level level, RecipeStorages storages, JsonObject json) {
        json.addProperty("ingredientType", Ref.Ports.ASTRAL_STARLIGHT.toString());
        json.addProperty("canRun", false);
        return json;
    }

    @Override
    public PortContent display() {
        return PortContent.gauge(displayName(), AstralStarlightPortStorage.FILL_COLOR, starlight.max(), 0, "Starlight");
    }
}

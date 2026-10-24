package io.ticticboom.mods.mm.port.astral.lumen;

import com.google.gson.JsonObject;
import hellfirepvp.astralsorcery.common.integration.jei.ingredient.LumenIngredientType;
import hellfirepvp.astralsorcery.common.lib.RegistriesAS;
import hellfirepvp.astralsorcery.common.lumen.LumenStack;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.astralsorcery.AstralSorceryNames;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
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

public class AstralLumenPortIngredient implements IPortIngredient {

    @Getter
    private final ResourceLocation lumen;
    private final AmountRange amount;

    public AstralLumenPortIngredient(ResourceLocation lumen, AmountRange amount) {
        this.lumen = lumen;
        this.amount = amount;
    }

    private int amount(RecipeStateModel state) {
        return state == null ? amount.max() : amount.resolve(state);
    }

    @Override
    public AmountRange getAmountRange() {
        return amount;
    }

    @Override
    public boolean canProcess(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (storages == null) {
            return false;
        }
        var remaining = amount(state);
        for (AstralLumenPortStorage input : storages.getInputStorages(AstralLumenPortStorage.class)) {
            remaining -= input.extract(lumen, remaining, true);
        }
        return remaining <= 0;
    }

    @Override
    public void process(Level level, RecipeStorages storages, RecipeStateModel state) {
        var remaining = amount(state);
        for (AstralLumenPortStorage input : storages.getInputStorages(AstralLumenPortStorage.class)) {
            remaining -= input.extract(lumen, remaining, false);
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
        var remaining = amount(state);
        for (AstralLumenPortStorage output : storages.getOutputStorages(AstralLumenPortStorage.class)) {
            remaining -= output.receive(lumen, remaining, true);
        }
        return remaining <= 0;
    }

    @Override
    public void output(Level level, RecipeStorages storages, RecipeStateModel state) {
        var remaining = amount(state);
        for (AstralLumenPortStorage output : storages.getOutputStorages(AstralLumenPortStorage.class)) {
            remaining -= output.receive(lumen, remaining, false);
        }
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid, IRecipeSlotBuilder recipeSlot) {
        RegistriesAS.REGISTRY_LUMEN.getOptional(lumen).ifPresent(type ->
                recipeSlot.addIngredient(LumenIngredientType.INSTANCE, LumenStack.of(type, amount.max())));
    }

    @Override
    public Component displayName() {
        return AstralSorceryNames.lumen(lumen);
    }

    @Override
    public JsonObject debugInput(Level level, RecipeStorages storages, JsonObject json) {
        json.addProperty("ingredientType", Ref.Ports.ASTRAL_LUMEN.toString());
        json.addProperty("lumen", lumen.toString());
        amount.addToDebug(json, "amountToExtract", null);
        json.addProperty("canRun", canProcess(level, storages, null));
        return json;
    }

    @Override
    public JsonObject debugOutput(Level level, RecipeStorages storages, JsonObject json) {
        json.addProperty("ingredientType", Ref.Ports.ASTRAL_LUMEN.toString());
        json.addProperty("lumen", lumen.toString());
        amount.addToDebug(json, "amountToInsert", null);
        json.addProperty("canRun", canOutput(level, storages, null));
        return json;
    }

    @Override
    public PortContent display() {
        return PortContent.gauge(displayName(), AstralLumenPortStorage.FILL_COLOR, amount.max(), 0, "Lumen");
    }
}

package io.ticticboom.mods.mm.port.etheria.ether;

import io.ticticboom.mods.mm.port.PortContent;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import io.ticticboom.mods.mm.compat.jei.ingredient.ether.EtherStack;
import io.ticticboom.mods.mm.port.ITickSpreadIngredient;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.util.LongAmountRange;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public class EtheriaEtherPortIngredient implements ITickSpreadIngredient {

    private final LongAmountRange ether;

    public EtheriaEtherPortIngredient(LongAmountRange ether) {
        this.ether = ether;
    }

    @Override
    public long resolveAmount(RecipeStateModel state) {
        return ether.resolve(state);
    }

    @Override
    public long extractFromInputs(RecipeStorages storages, long amount, boolean simulate) {
        long extracted = 0;
        for (EtheriaEtherPortStorage input : storages.getInputStorages(EtheriaEtherPortStorage.class)) {
            extracted += input.extract(amount - extracted, simulate);
            if (extracted >= amount) {
                break;
            }
        }
        return extracted;
    }

    private long insertIntoOutputs(RecipeStorages storages, long amount, boolean simulate) {
        long inserted = 0;
        for (EtheriaEtherPortStorage output : storages.getOutputStorages(EtheriaEtherPortStorage.class)) {
            inserted += output.receive(amount - inserted, simulate);
            if (inserted >= amount) {
                break;
            }
        }
        return inserted;
    }

    @Override
    public boolean canProcess(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (storages == null) {
            return false;
        }
        long amount = resolveAmount(state);
        return extractFromInputs(storages, amount, true) >= amount;
    }

    @Override
    public void process(Level level, RecipeStorages storages, RecipeStateModel state) {
        extractFromInputs(storages, resolveAmount(state), false);
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
        long amount = resolveAmount(state);
        return insertIntoOutputs(storages, amount, true) >= amount;
    }

    @Override
    public void output(Level level, RecipeStorages storages, RecipeStateModel state) {
        insertIntoOutputs(storages, resolveAmount(state), false);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid, IRecipeSlotBuilder recipeSlot) {
        recipeSlot.addIngredient(MMJeiIngredients.ETHERIA_ETHER, new EtherStack(ether.min(), ether.max()));
    }

    @Override
    public Component displayName() {
        return Component.translatable("port.mm.etheria_ether.name");
    }

    @Override
    public JsonObject debugInput(Level level, RecipeStorages storages, JsonObject json) {
        json.addProperty("ingredientType", Ref.Ports.ETHERIA_ETHER.toString());
        json.addProperty("amountToExtract", ether.max());
        json.addProperty("canRun", canProcess(level, storages, null));
        return json;
    }

    @Override
    public JsonObject debugOutput(Level level, RecipeStorages storages, JsonObject json) {
        json.addProperty("ingredientType", Ref.Ports.ETHERIA_ETHER.toString());
        json.addProperty("amountToInsert", ether.max());
        json.addProperty("canRun", canOutput(level, storages, null));
        return json;
    }

    @Override
    public PortContent display() {
        return PortContent.gauge(Component.translatable("port.mm.etheria_ether.name"), EtheriaEtherPortStorage.FILL_COLOR, ether.max(), 0, "Ether");
    }
}

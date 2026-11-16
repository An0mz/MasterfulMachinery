package io.ticticboom.mods.mm.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.recipe.input.IRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.output.IRecipeOutputEntry;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public record RecipeOutputs(
        List<IRecipeOutputEntry> outputs
) {
    public static RecipeOutputs parse(JsonArray json) {
        var outputs = new ArrayList<IRecipeOutputEntry>();
        for (JsonElement elem : json) {
            JsonObject entryJson = elem.getAsJsonObject();
            outputs.add(MachineRecipeManager.parseOutputEntry(entryJson));
        }
        return new RecipeOutputs(outputs);
    }

    public boolean canProcess(Level level, RecipeStorages storages, RecipeStateModel model) {
        for (int i = 0; i < outputs.size(); i++) {
            IRecipeOutputEntry output = outputs.get(i);
            boolean ok = output.canOutput(level, storages, model);
            if (!ok) {
                model.markBlocked(RecipeBlockReason.Kind.OUTPUT, i);
                return false;
            }
        }
        if (storages != null && sharesPorts(level, model, false) && !fitTogether(level, storages, model, false)) {
            model.markBlocked(RecipeBlockReason.Kind.OUTPUT, -1);
            return false;
        }
        return true;
    }

    public boolean canProcessTick(Level level, RecipeStorages storages, RecipeStateModel model) {
        int simulated = 0;
        for (int e = 0; e < outputs.size(); e++) {
            var planned = outputs.get(e).plannedTickOutputs(level, model);
            for (int i = 0; i < planned.size(); i++) {
                IPortIngredient ingredient = planned.get(i);
                if (!ingredient.canOutput(level, storages, model)) {
                    model.markBlocked(RecipeBlockReason.Kind.OUTPUT, e);
                    return false;
                }
                if (ingredient.canSimulateOutput()) {
                    simulated++;
                }
            }
        }
        if (simulated >= 2 && sharesPorts(level, model, true) && !fitTogether(level, storages, model, true)) {
            model.markBlocked(RecipeBlockReason.Kind.OUTPUT, -1);
            return false;
        }
        return true;
    }

    private boolean sharesPorts(Level level, RecipeStateModel model, boolean perTick) {
        for (int a = 0; a < outputs.size(); a++) {
            var first = perTick ? outputs.get(a).plannedTickOutputs(level, model) : outputs.get(a).plannedOutputs(level, model);
            for (int i = 0; i < first.size(); i++) {
                IPortIngredient one = first.get(i);
                if (!one.canSimulateOutput()) {
                    continue;
                }
                if (i + 1 < first.size()) {
                    for (int j = i + 1; j < first.size(); j++) {
                        if (first.get(j).canSimulateOutput() && samePorts(one, first.get(j))) {
                            return true;
                        }
                    }
                }
                for (int b = a + 1; b < outputs.size(); b++) {
                    var second = perTick ? outputs.get(b).plannedTickOutputs(level, model) : outputs.get(b).plannedOutputs(level, model);
                    for (int j = 0; j < second.size(); j++) {
                        if (second.get(j).canSimulateOutput() && samePorts(one, second.get(j))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean samePorts(IPortIngredient a, IPortIngredient b) {
        return a.getClass() == b.getClass()
                || (a instanceof io.ticticboom.mods.mm.port.item.BaseItemPortIngredient && b instanceof io.ticticboom.mods.mm.port.item.BaseItemPortIngredient);
    }

    private boolean fitTogether(Level level, RecipeStorages storages, RecipeStateModel model, boolean perTick) {
        int count = 0;
        for (IRecipeOutputEntry output : outputs) {
            var planned = perTick ? output.plannedTickOutputs(level, model) : output.plannedOutputs(level, model);
            for (int i = 0; i < planned.size(); i++) {
                if (planned.get(i).canSimulateOutput()) {
                    count++;
                }
            }
        }
        if (count < 2) {
            return true;
        }
        var simulated = new ArrayList<IPortIngredient>(count);
        for (IRecipeOutputEntry output : outputs) {
            var planned = perTick ? output.plannedTickOutputs(level, model) : output.plannedOutputs(level, model);
            for (int i = 0; i < planned.size(); i++) {
                if (planned.get(i).canSimulateOutput()) {
                    simulated.add(planned.get(i));
                }
            }
        }
        return OutputSimulation.fitTogether(level, storages, model, simulated);
    }

    public boolean usesConsumedItems() {
        for (IRecipeOutputEntry output : outputs) {
            if (output.usesConsumedItems()) {
                return true;
            }
        }
        return false;
    }

    public void process(Level level, RecipeStorages storages, RecipeStateModel model) {
        for (IRecipeOutputEntry output : outputs) {
            output.output(level, storages, model);
        }
    }

    public void processTick(Level level, RecipeStorages storages, RecipeStateModel model) {
        for (IRecipeOutputEntry output : outputs) {
            output.processTick(level, storages, model);
        }
    }


    public void ditch(Level level, RecipeStorages storages, RecipeStateModel model) {
        for (IRecipeOutputEntry output : outputs) {
            output.ditchRecipe(level, storages, model);
        }
    }


    public JsonArray debugRun(Level level, RecipeStorages storages, RecipeStateModel model) {
        var jsonArray = new JsonArray();
        for (IRecipeOutputEntry output : outputs) {
            var expected = output.debugExpected(level, storages, model, new JsonObject());
            var inputRunJson = new JsonObject();
            inputRunJson.add("expected", expected);
            jsonArray.add(inputRunJson);
        }
        return jsonArray;
    }
}

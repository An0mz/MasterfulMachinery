package io.ticticboom.mods.mm.recipe;

import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.IPortStorage;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public final class OutputSimulation {

    private final RecipeStorages real;
    private final HolderLookup.Provider registries;
    private RecipeStorages current;

    public OutputSimulation(Level level, RecipeStorages real) {
        this.real = real;
        this.registries = level.registryAccess();
        this.current = real;
    }

    public static boolean fitTogether(Level level, RecipeStorages storages, RecipeStateModel state, List<? extends IPortIngredient> outputs) {
        return new OutputSimulation(level, storages).tryAdd(level, state, outputs);
    }

    public boolean tryAdd(Level level, RecipeStateModel state, List<? extends IPortIngredient> outputs) {
        var trial = copyOf(current.outputStorages());
        for (IPortIngredient output : outputs) {
            if (!output.canOutput(level, trial, state)) {
                return false;
            }
            output.output(level, trial, state);
        }
        current = trial;
        return true;
    }

    private RecipeStorages copyOf(List<IPortStorage> outputs) {
        var copies = new ArrayList<IPortStorage>(outputs.size());
        for (IPortStorage storage : outputs) {
            IPortStorage copy = storage.simulationCopy(registries);
            if (copy != null) {
                copies.add(copy);
            }
        }
        return new RecipeStorages(real.inputStorages(), copies);
    }
}

package io.ticticboom.mods.mm.controller.machine.register;

import io.ticticboom.mods.mm.recipe.RecipeModel;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.resources.ResourceLocation;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class RecipeCandidateIndex {
    private final List<RecipeModel> recipes;
    private final RecipeRequirements[] requirements;
    private final Map<ResourceLocation, int[]> positionsByItem = new HashMap<>();
    private final int[] unindexed;
    private final BitSet tested = new BitSet();
    private final BitSet found = new BitSet();

    RecipeCandidateIndex(List<RecipeModel> recipes) {
        this.recipes = List.copyOf(recipes);
        this.requirements = new RecipeRequirements[this.recipes.size()];
        Map<ResourceLocation, IntArrayList> byItem = new HashMap<>();
        var none = new IntArrayList();
        for (int i = 0; i < this.recipes.size(); i++) {
            var req = RecipeRequirements.of(this.recipes.get(i));
            requirements[i] = req;
            if (req.itemIds().isEmpty()) {
                none.add(i);
            } else {
                for (ResourceLocation item : req.itemIds()) {
                    byItem.computeIfAbsent(item, k -> new IntArrayList()).add(i);
                }
            }
        }
        byItem.forEach((item, positions) -> positionsByItem.put(item, positions.toIntArray()));
        this.unindexed = none.toIntArray();
    }

    int size() {
        return recipes.size();
    }

    RecipeModel recipe(int position) {
        return recipes.get(position);
    }

    RecipeRequirements requirements(int position) {
        return requirements[position];
    }

    void collect(Set<ResourceLocation> itemIds, Set<ResourceLocation> fluidIds, boolean hasEnergy, IntArrayList out) {
        out.clear();
        collect(unindexed, itemIds, fluidIds, hasEnergy);
        for (ResourceLocation item : itemIds) {
            int[] positions = positionsByItem.get(item);
            if (positions != null) {
                collect(positions, itemIds, fluidIds, hasEnergy);
            }
        }
        for (int position = found.nextSetBit(0); position >= 0; position = found.nextSetBit(position + 1)) {
            out.add(position);
        }
        tested.clear();
        found.clear();
    }

    void nearMisses(Set<ResourceLocation> itemIds, Set<ResourceLocation> fluidIds, boolean hasEnergy, int limit, IntArrayList out) {
        out.clear();
        for (ResourceLocation item : itemIds) {
            int[] positions = positionsByItem.get(item);
            if (positions == null) {
                continue;
            }
            for (int position : positions) {
                if (!tested.get(position)) {
                    tested.set(position);
                    if (!matches(requirements[position], itemIds, fluidIds, hasEnergy)) {
                        found.set(position);
                    }
                }
            }
        }
        for (int position = found.nextSetBit(0); position >= 0 && out.size() < limit; position = found.nextSetBit(position + 1)) {
            out.add(position);
        }
        tested.clear();
        found.clear();
    }

    private void collect(int[] positions, Set<ResourceLocation> itemIds, Set<ResourceLocation> fluidIds, boolean hasEnergy) {
        for (int position : positions) {
            if (tested.get(position)) {
                continue;
            }
            tested.set(position);
            if (matches(requirements[position], itemIds, fluidIds, hasEnergy)) {
                found.set(position);
            }
        }
    }

    static boolean matches(RecipeRequirements needs, Set<ResourceLocation> itemIds, Set<ResourceLocation> fluidIds, boolean hasEnergy) {
        return itemIds.containsAll(needs.itemIds())
                && fluidIds.containsAll(needs.fluidIds())
                && (!needs.energy() || hasEnergy);
    }
}

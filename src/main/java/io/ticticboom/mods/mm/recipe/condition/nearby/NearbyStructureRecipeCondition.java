package io.ticticboom.mods.mm.recipe.condition.nearby;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.recipe.condition.IRecipeCondition;
import io.ticticboom.mods.mm.recipe.condition.RecipeConditionContext;
import io.ticticboom.mods.mm.structure.StructureBoost;
import io.ticticboom.mods.mm.structure.StructureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class NearbyStructureRecipeCondition implements IRecipeCondition {
    private final ResourceLocation structureId;
    private final int radius;
    private final int min;

    public NearbyStructureRecipeCondition(ResourceLocation structureId, int radius, int min) {
        this.structureId = structureId;
        this.radius = radius;
        this.min = min;
    }

    public static IRecipeCondition parse(JsonObject json) {
        var id = ResourceLocation.tryParse(json.get("structure").getAsString());
        if (id == null) {
            throw new IllegalArgumentException("Invalid structure id in mm:nearby_structure: " + json.get("structure"));
        }
        int radius = json.has("radius") ? json.get("radius").getAsInt() : StructureBoost.DEFAULT_RADIUS;
        int min = json.has("min") ? json.get("min").getAsInt() : 1;
        return new NearbyStructureRecipeCondition(id, radius, min);
    }

    @Override
    public boolean canRun(RecipeConditionContext ctx) {
        if (ctx.nearby() == null) return true;
        return ctx.nearby().count(structureId, radius) >= min;
    }

    @Override
    public Component describe() {
        var structure = StructureManager.STRUCTURES.get(structureId);
        Component name = structure != null ? structure.displayName() : Component.literal(structureId.toString());
        return Component.translatable("jei.mm.condition.nearby_structure", min, name, radius);
    }
}

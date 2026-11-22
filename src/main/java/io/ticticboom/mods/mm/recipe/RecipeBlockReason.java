package io.ticticboom.mods.mm.recipe;

import io.ticticboom.mods.mm.port.PortContent;
import io.ticticboom.mods.mm.recipe.input.consume.ConsumeRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.output.DisplayedOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public record RecipeBlockReason(ResourceLocation recipeId, Kind kind, int index) {

    public enum Kind {
        CONDITION,
        INPUT,
        OUTPUT
    }

    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putString("Recipe", recipeId.toString());
        tag.putString("Kind", kind.name().toLowerCase(Locale.ROOT));
        tag.putInt("Index", index);
        return tag;
    }

    @Nullable
    public static RecipeBlockReason load(CompoundTag tag) {
        var id = ResourceLocation.tryParse(tag.getString("Recipe"));
        if (id == null) {
            return null;
        }
        try {
            return new RecipeBlockReason(id, Kind.valueOf(tag.getString("Kind").toUpperCase(Locale.ROOT)), tag.getInt("Index"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    public RecipeModel recipe() {
        return MachineRecipeManager.RECIPES.get(recipeId);
    }

    @Nullable
    public PortContent content() {
        RecipeModel recipe = recipe();
        if (recipe == null || index < 0) {
            return null;
        }
        if (kind == Kind.INPUT && index < recipe.inputs().inputs().size()
                && recipe.inputs().inputs().get(index) instanceof ConsumeRecipeIngredientEntry consume) {
            return consume.getIngredient().display();
        }
        if (kind == Kind.OUTPUT && index < recipe.outputs().outputs().size()) {
            var outputs = recipe.outputs().outputs().get(index).displayedOutputs();
            if (!outputs.isEmpty()) {
                return outputs.get(0).ingredient().display();
            }
        }
        return null;
    }

    @Nullable
    public PortContent product() {
        RecipeModel recipe = recipe();
        if (recipe == null) {
            return null;
        }
        for (var entry : recipe.outputs().outputs()) {
            for (DisplayedOutput output : entry.displayedOutputs()) {
                PortContent content = output.ingredient().display();
                if (content != null) {
                    return content;
                }
            }
        }
        return null;
    }

    public int progress() {
        return switch (kind) {
            case OUTPUT -> 2_000_000;
            case INPUT -> 1_000_000 + index;
            case CONDITION -> 0;
        };
    }
}

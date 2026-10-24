package io.ticticboom.mods.mm.compat.jei.ingredient.starlight;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StarlightIngredientHelper implements IIngredientHelper<StarlightIngredient> {
    @Override
    public @NotNull IIngredientType<StarlightIngredient> getIngredientType() {
        return MMJeiIngredients.ASTRAL_STARLIGHT;
    }

    @Override
    public @NotNull String getDisplayName(@NotNull StarlightIngredient ingredient) {
        return StarlightIngredientRenderer.title(ingredient).getString();
    }

    @Override
    public @NotNull String getUniqueId(@NotNull StarlightIngredient ingredient, @NotNull UidContext context) {
        return "astralsorcery/starlight/" + (ingredient.constellation() == null ? "any" : ingredient.constellation());
    }

    @Override
    public @NotNull ResourceLocation getResourceLocation(@NotNull StarlightIngredient ingredient) {
        return ingredient.constellation() != null ? ingredient.constellation() : Ref.id("astralsorcery/starlight");
    }

    @Override
    public @NotNull StarlightIngredient copyIngredient(@NotNull StarlightIngredient ingredient) {
        return new StarlightIngredient(ingredient.constellation(), ingredient.min(), ingredient.max());
    }

    @Override
    public @NotNull String getErrorInfo(@Nullable StarlightIngredient ingredient) {
        return ingredient == null ? "Error" : String.valueOf(ingredient.constellation());
    }
}

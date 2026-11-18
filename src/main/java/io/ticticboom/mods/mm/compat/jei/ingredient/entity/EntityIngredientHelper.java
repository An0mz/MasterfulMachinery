package io.ticticboom.mods.mm.compat.jei.ingredient.entity;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EntityIngredientHelper implements IIngredientHelper<EntityIngredient> {
    @Override
    public @NotNull IIngredientType<EntityIngredient> getIngredientType() {
        return MMJeiIngredients.ENTITY;
    }

    @Override
    public @NotNull String getDisplayName(@NotNull EntityIngredient ingredient) {
        return EntityIngredientNames.displayName(ingredient).getString();
    }

    @Override
    public @NotNull String getUniqueId(@NotNull EntityIngredient ingredient, @NotNull UidContext context) {
        if (ingredient.group() != null) {
            return "entity/group/" + ingredient.group().serialize();
        }
        if (ingredient.id() == null) {
            return "entity/any";
        }
        return "entity/" + (ingredient.tag() ? "tag/" : "") + ingredient.id();
    }

    @Override
    public @NotNull ResourceLocation getResourceLocation(@NotNull EntityIngredient ingredient) {
        if (ingredient.group() != null) {
            return Ref.id("entity_group/" + ingredient.group().serialize());
        }
        return ingredient.id() == null ? Ref.id("any_entity") : ingredient.id();
    }

    @Override
    public @NotNull EntityIngredient copyIngredient(@NotNull EntityIngredient ingredient) {
        return new EntityIngredient(ingredient.id(), ingredient.tag(), ingredient.group(), ingredient.amount());
    }

    @Override
    public @NotNull String getErrorInfo(@Nullable EntityIngredient ingredient) {
        if (ingredient == null) {
            return "Error";
        }
        if (ingredient.group() != null) {
            return "group/" + ingredient.group().serialize();
        }
        return ingredient.id() == null ? "any" : ingredient.id().toString();
    }
}

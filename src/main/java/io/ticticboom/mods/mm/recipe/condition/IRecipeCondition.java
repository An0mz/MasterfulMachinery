package io.ticticboom.mods.mm.recipe.condition;

import net.minecraft.network.chat.Component;

public interface IRecipeCondition {
    boolean canRun(RecipeConditionContext ctx);

    default boolean canStart(RecipeConditionContext ctx) {
        return canRun(ctx);
    }

    default void onStart(RecipeConditionContext ctx) {
    }

    default void onFinish(RecipeConditionContext ctx) {
    }

    Component describe();
}

package io.ticticboom.mods.mm.recipe.condition.astral;

import com.google.gson.JsonObject;
import hellfirepvp.astralsorcery.common.constellation.level.LevelSkyHandler;
import io.ticticboom.mods.mm.compat.astralsorcery.AstralSorceryNames;
import io.ticticboom.mods.mm.recipe.condition.IRecipeCondition;
import io.ticticboom.mods.mm.recipe.condition.RecipeConditionContext;
import io.ticticboom.mods.mm.util.ParserUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class AstralConstellationRecipeCondition implements IRecipeCondition {

    private final ResourceLocation constellation;

    public AstralConstellationRecipeCondition(ResourceLocation constellation) {
        this.constellation = constellation;
    }

    public static IRecipeCondition parse(JsonObject json) {
        var constellation = ParserUtils.parseId(json, "constellation");
        AstralSorceryNames.requireConstellation(constellation);
        return new AstralConstellationRecipeCondition(constellation);
    }

    @Override
    public boolean canRun(RecipeConditionContext ctx) {
        var cst = AstralSorceryNames.requireConstellation(constellation);
        return LevelSkyHandler.getContext(ctx.level())
                .map(sky -> sky.getConstellationHandler().isCurrentlyActive(cst))
                .orElse(false);
    }

    @Override
    public Component describe() {
        return Component.translatable("jei.mm.condition.astral_constellation", AstralSorceryNames.constellation(constellation));
    }
}

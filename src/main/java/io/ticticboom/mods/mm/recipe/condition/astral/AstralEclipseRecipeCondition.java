package io.ticticboom.mods.mm.recipe.condition.astral;

import com.google.gson.JsonObject;
import hellfirepvp.astralsorcery.common.constellation.level.LevelSkyHandler;
import io.ticticboom.mods.mm.recipe.condition.IRecipeCondition;
import io.ticticboom.mods.mm.recipe.condition.RecipeConditionContext;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public class AstralEclipseRecipeCondition implements IRecipeCondition {

    private final boolean solar;

    public AstralEclipseRecipeCondition(boolean solar) {
        this.solar = solar;
    }

    public static IRecipeCondition parse(JsonObject json) {
        var eclipse = json.get("eclipse").getAsString().toLowerCase(Locale.ROOT);
        if (!eclipse.equals("solar") && !eclipse.equals("lunar")) {
            throw new RuntimeException("Unknown eclipse '" + eclipse + "', expected 'solar' or 'lunar'");
        }
        return new AstralEclipseRecipeCondition(eclipse.equals("solar"));
    }

    @Override
    public boolean canRun(RecipeConditionContext ctx) {
        return LevelSkyHandler.getContext(ctx.level())
                .map(sky -> {
                    var events = sky.getCelestialEventHandler();
                    return solar ? events.getSolarEclipse().isActiveNow() : events.getLunarEclipse().isActiveNow();
                })
                .orElse(false);
    }

    @Override
    public Component describe() {
        return Component.translatable(solar ? "jei.mm.condition.astral_eclipse.solar" : "jei.mm.condition.astral_eclipse.lunar");
    }
}

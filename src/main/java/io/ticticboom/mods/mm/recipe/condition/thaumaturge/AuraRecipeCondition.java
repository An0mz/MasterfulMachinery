package io.ticticboom.mods.mm.recipe.condition.thaumaturge;

import com.google.gson.JsonObject;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import io.ticticboom.mods.mm.recipe.condition.IRecipeCondition;
import io.ticticboom.mods.mm.recipe.condition.RecipeConditionContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;

public class AuraRecipeCondition implements IRecipeCondition {
    private final float minVis;
    private final float maxFlux;
    private final float vis;
    private final float flux;

    public AuraRecipeCondition(float minVis, float maxFlux, float vis, float flux) {
        this.minVis = minVis;
        this.maxFlux = maxFlux;
        this.vis = vis;
        this.flux = flux;
    }

    public static IRecipeCondition parse(JsonObject json) {
        float minVis = json.has("minVis") ? Math.max(0, json.get("minVis").getAsFloat()) : 0;
        float maxFlux = json.has("maxFlux") ? Math.max(0, json.get("maxFlux").getAsFloat()) : Float.POSITIVE_INFINITY;
        float vis = json.has("vis") ? Math.max(0, json.get("vis").getAsFloat()) : 0;
        float flux = json.has("flux") ? Math.max(0, json.get("flux").getAsFloat()) : 0;
        return new AuraRecipeCondition(minVis, maxFlux, vis, flux);
    }

    @Override
    public boolean canRun(RecipeConditionContext ctx) {
        if (ctx.pos() == null || maxFlux == Float.POSITIVE_INFINITY) {
            return true;
        }
        return AuraHelper.getFlux(ctx.level(), ctx.pos()) <= maxFlux;
    }

    @Override
    public boolean canStart(RecipeConditionContext ctx) {
        if (!canRun(ctx)) {
            return false;
        }
        var pos = ctx.pos();
        if (pos == null) {
            return true;
        }
        if (minVis > 0 && AuraHelper.getVis(ctx.level(), pos) < minVis) {
            return false;
        }
        return vis <= 0 || AuraHelper.drainVis(ctx.level(), pos, vis, true) >= vis;
    }

    @Override
    public void onStart(RecipeConditionContext ctx) {
        if (vis > 0 && ctx.pos() != null && !ctx.level().isClientSide()) {
            AuraHelper.drainVis(ctx.level(), ctx.pos(), vis, false);
        }
    }

    @Override
    public void onFinish(RecipeConditionContext ctx) {
        if (flux > 0 && ctx.pos() != null && !ctx.level().isClientSide()) {
            AuraHelper.polluteAura(ctx.level(), ctx.pos(), flux, true);
        }
    }

    @Override
    public Component describe() {
        var parts = new ArrayList<Component>();
        if (vis > 0) parts.add(Component.translatable("jei.mm.condition.aura.vis", format(vis)));
        if (minVis > 0) parts.add(Component.translatable("jei.mm.condition.aura.min_vis", format(minVis)));
        if (maxFlux != Float.POSITIVE_INFINITY) parts.add(Component.translatable("jei.mm.condition.aura.max_flux", format(maxFlux)));
        if (flux > 0) parts.add(Component.translatable("jei.mm.condition.aura.flux", format(flux)));
        if (parts.isEmpty()) return Component.translatable("jei.mm.condition.aura.any");
        MutableComponent line = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) line.append(", ");
            line.append(parts.get(i));
        }
        return line;
    }

    private static String format(float value) {
        return value == Math.rint(value) ? String.valueOf((int) value) : String.valueOf(value);
    }
}

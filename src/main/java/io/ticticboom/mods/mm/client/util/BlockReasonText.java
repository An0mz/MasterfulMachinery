package io.ticticboom.mods.mm.client.util;

import io.ticticboom.mods.mm.port.PortContent;
import io.ticticboom.mods.mm.recipe.RecipeBlockReason;
import io.ticticboom.mods.mm.util.NumberText;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class BlockReasonText {

    private BlockReasonText() {
    }

    public static Component title(RecipeBlockReason reason) {
        String key = "gui.mm.controller.blocked." + reason.kind().name().toLowerCase(Locale.ROOT);
        if (reason.kind() == RecipeBlockReason.Kind.INPUT) {
            PortContent content = reason.content();
            if (content != null && content.kind() == PortContent.Kind.ENERGY) {
                key = "gui.mm.controller.blocked.energy";
            }
        }
        PortContent product = reason.kind() == RecipeBlockReason.Kind.OUTPUT ? null : reason.product();
        return product == null ? Component.translatable(key) : Component.translatable(key + ".for", amount(product));
    }

    public static Component detail(RecipeBlockReason reason) {
        var recipe = reason.recipe();
        if (reason.kind() == RecipeBlockReason.Kind.CONDITION && recipe != null) {
            var lines = recipe.conditions().describe();
            if (reason.index() >= 0 && reason.index() < lines.size()) {
                return lines.get(reason.index());
            }
        }
        PortContent content = reason.content();
        if (content != null) {
            return amount(content);
        }
        if (reason.kind() == RecipeBlockReason.Kind.OUTPUT) {
            return Component.translatable("gui.mm.controller.blocked.output.together");
        }
        return Component.literal(reason.recipeId().toString());
    }

    private static Component amount(PortContent content) {
        Component name = content.kind() == PortContent.Kind.ENERGY ? Component.translatable("gui.mm.port.energy")
                : content.name() != null ? content.name() : Component.empty();
        String unit = content.unit().isEmpty() ? "" : " " + content.unit();
        return Component.literal(NumberText.grouped(content.amount()) + unit + " ").append(name);
    }
}

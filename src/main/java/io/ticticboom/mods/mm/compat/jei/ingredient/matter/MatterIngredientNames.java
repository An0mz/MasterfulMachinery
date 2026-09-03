package io.ticticboom.mods.mm.compat.jei.ingredient.matter;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class MatterIngredientNames {

    public static Component displayName(ResourceLocation matter) {
        if (matter == null) {
            return Component.literal("?");
        }
        return Component.translatableWithFallback("replication.matter_type." + matter.getPath(), matter.getPath());
    }
}

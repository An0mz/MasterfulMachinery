package io.ticticboom.mods.mm.recipe.flex;

import io.ticticboom.mods.mm.compat.mysticalagriculture.MACrops;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.function.Function;

public final class FlexItems {

    public static final Set<String> TOKENS = Set.of("{ma:essence}", "{ma:crux}");

    private FlexItems() {
    }

    public static void validate(String item) {
        String text = item.trim();
        if (text.startsWith("{") && text.endsWith("}") && text.indexOf('}') == text.length() - 1
                && !text.equals("{ns}") && !text.equals("{path}") && !TOKENS.contains(text)) {
            throw new RuntimeException("Unknown flex token " + text + ", known: " + TOKENS);
        }
    }

    public static ItemStack resolve(FlexTemplate template, ItemStack input) {
        ResourceLocation inputId = BuiltInRegistries.ITEM.getKey(input.getItem());
        String text = template.apply(inputId.getNamespace(), inputId.getPath());
        if (text == null) {
            return ItemStack.EMPTY;
        }
        text = text.trim();
        if (text.equals("{ma:essence}")) {
            return fromMA(input, MACrops.Info::essence);
        }
        if (text.equals("{ma:crux}")) {
            return fromMA(input, MACrops.Info::crux);
        }
        ResourceLocation outputId = ResourceLocation.tryParse(text);
        if (outputId == null || !BuiltInRegistries.ITEM.containsKey(outputId)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(outputId));
    }

    private static ItemStack fromMA(ItemStack input, Function<MACrops.Info, Item> part) {
        MACrops.Info info = MACrops.forSeeds(input.getItem());
        Item item = info == null ? null : part.apply(info);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
}

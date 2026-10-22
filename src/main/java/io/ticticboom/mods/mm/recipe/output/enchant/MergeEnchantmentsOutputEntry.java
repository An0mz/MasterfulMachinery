package io.ticticboom.mods.mm.recipe.output.enchant;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.SlotGridEntry;
import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.recipe.output.IRecipeOutputEntry;
import io.ticticboom.mods.mm.util.ParserUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

import java.util.List;

public class MergeEnchantmentsOutputEntry implements IRecipeOutputEntry {

    private static final int LEVEL_LIMIT = 255;

    private final Item item;
    private final int maxLevel;

    public MergeEnchantmentsOutputEntry(Item item, int maxLevel) {
        this.item = item;
        this.maxLevel = Math.max(1, Math.min(maxLevel, LEVEL_LIMIT));
    }

    public static IRecipeOutputEntry parse(JsonObject json) {
        ResourceLocation itemId = json.has("item") ? ParserUtils.parseId(json, "item") : BuiltInRegistries.ITEM.getKey(Items.ENCHANTED_BOOK);
        if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
            throw new RuntimeException(String.format("Could not find item [%s] which is required by an MM recipe", itemId));
        }
        int maxLevel = json.has("maxLevel") ? json.get("maxLevel").getAsInt() : LEVEL_LIMIT;
        return new MergeEnchantmentsOutputEntry(BuiltInRegistries.ITEM.get(itemId), maxLevel);
    }

    @Override
    public boolean usesConsumedItems() {
        return true;
    }

    public ItemStack result(RecipeStateModel state) {
        List<ItemStack> consumed = state == null || state.getConsumedItems() == null ? List.of() : state.getConsumedItems();
        ItemStack result = ItemStack.EMPTY;
        for (ItemStack stack : consumed) {
            if (stack.is(item)) {
                result = stack.copyWithCount(1);
                break;
            }
        }
        if (result.isEmpty()) {
            result = new ItemStack(item);
        }
        var merged = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (ItemStack stack : consumed) {
            for (int i = 0; i < stack.getCount(); i++) {
                merge(merged, stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
                merge(merged, stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY));
            }
        }
        EnchantmentHelper.setEnchantments(result, merged.toImmutable());
        return result;
    }

    private void merge(ItemEnchantments.Mutable merged, ItemEnchantments from) {
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : from.entrySet()) {
            int current = merged.getLevel(entry.getKey());
            int level = entry.getIntValue();
            int next = current == level ? level + 1 : Math.max(current, level);
            merged.set(entry.getKey(), Math.min(next, maxLevel));
        }
    }

    @Override
    public boolean canOutput(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (storages == null) return false;
        ItemStack stack = result(state);
        int remaining = 1;
        for (ItemPortStorage storage : storages.getOutputItemStorages(item)) {
            remaining = storage.canInsert(stack, remaining);
            if (remaining <= 0) return true;
        }
        return false;
    }

    @Override
    public void output(Level level, RecipeStorages storages, RecipeStateModel state) {
        ItemStack stack = result(state);
        int remaining = 1;
        for (ItemPortStorage storage : storages.getOutputItemStorages(item)) {
            if (remaining <= 0) break;
            remaining = storage.insert(stack, remaining);
        }
    }

    @Override
    public void ditchRecipe(Level level, RecipeStorages storages, RecipeStateModel state) {
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid) {
        SlotGridEntry slot = grid.next();
        slot.setUsed();
        builder.addSlot(RecipeIngredientRole.OUTPUT, slot.getInnerX(), slot.getInnerY())
                .addIngredient(MMJeiIngredients.ITEM, new ItemStack(item))
                .addRichTooltipCallback((v, list) -> {
                    list.add(Component.translatable("jei.mm.recipe.merge_enchantments").withStyle(ChatFormatting.DARK_AQUA));
                    if (maxLevel < LEVEL_LIMIT) {
                        list.add(Component.translatable("jei.mm.recipe.merge_enchantments.max_level", maxLevel).withStyle(ChatFormatting.DARK_AQUA));
                    }
                });
    }

    @Override
    public JsonObject debugExpected(Level level, RecipeStorages storages, RecipeStateModel model, JsonObject json) {
        json.addProperty("item", BuiltInRegistries.ITEM.getKey(item).toString());
        json.addProperty("maxLevel", maxLevel);
        json.addProperty("result", result(model).toString());
        return json;
    }
}

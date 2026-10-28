package io.ticticboom.mods.mm.recipe.output.enchant;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.SlotGridEntry;
import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.recipe.output.IRecipeOutputEntry;
import io.ticticboom.mods.mm.util.ParserUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MergeEnchantmentsOutputEntry implements IRecipeOutputEntry {

    private static final int LEVEL_LIMIT = 255;

    private final Item item;
    private final TagKey<Item> tag;
    private final Integer maxLevel;
    private final boolean allowConflicting;
    private final boolean allowUnsupported;

    public MergeEnchantmentsOutputEntry(Item item, TagKey<Item> tag, Integer maxLevel, boolean allowConflicting, boolean allowUnsupported) {
        this.item = item;
        this.tag = tag;
        this.maxLevel = maxLevel == null ? null : Math.max(1, Math.min(maxLevel, LEVEL_LIMIT));
        this.allowConflicting = allowConflicting;
        this.allowUnsupported = allowUnsupported;
    }

    public static IRecipeOutputEntry parse(JsonObject json) {
        Item item = null;
        TagKey<Item> tag = null;
        if (json.has("item")) {
            String raw = json.get("item").getAsString();
            if (raw.startsWith("#")) {
                tag = TagKey.create(Registries.ITEM, ParserUtils.parseId(new JsonPrimitive(raw.substring(1))));
            } else {
                ResourceLocation itemId = ParserUtils.parseId(json, "item");
                if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
                    throw new RuntimeException(String.format("Could not find item [%s] which is required by an MM recipe", itemId));
                }
                item = BuiltInRegistries.ITEM.get(itemId);
            }
        }
        Integer maxLevel = json.has("maxLevel") ? json.get("maxLevel").getAsInt() : null;
        boolean allowConflicting = json.has("allowConflicting") && json.get("allowConflicting").getAsBoolean();
        boolean allowUnsupported = json.has("allowUnsupported") && json.get("allowUnsupported").getAsBoolean();
        return new MergeEnchantmentsOutputEntry(item, tag, maxLevel, allowConflicting, allowUnsupported);
    }

    @Override
    public boolean usesConsumedItems() {
        return true;
    }

    private static boolean isBook(ItemStack stack) {
        return stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK);
    }

    private boolean isBase(ItemStack stack) {
        if (item != null) return stack.is(item);
        if (tag != null) return stack.is(tag);
        return !isBook(stack) && (stack.isEnchantable() || stack.isEnchanted());
    }

    private ItemStack base(List<ItemStack> consumed) {
        for (ItemStack stack : consumed) {
            if (!stack.isEmpty() && isBase(stack)) {
                return stack.copyWithCount(1);
            }
        }
        if (tag == null && (item == null || item == Items.ENCHANTED_BOOK)) {
            return new ItemStack(Items.ENCHANTED_BOOK);
        }
        return ItemStack.EMPTY;
    }

    public ItemStack result(RecipeStateModel state) {
        List<ItemStack> consumed = state == null || state.getConsumedItems() == null ? List.of() : state.getConsumedItems();
        ItemStack result = base(consumed);
        if (result.isEmpty()) {
            return result;
        }
        Set<Holder<Enchantment>> own = EnchantmentHelper.getEnchantmentsForCrafting(result).keySet();
        Map<Holder<Enchantment>, IntList> levels = new LinkedHashMap<>();
        for (ItemStack stack : consumed) {
            for (int i = 0; i < stack.getCount(); i++) {
                collect(levels, stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
                collect(levels, stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY));
            }
        }
        Object2IntMap<Holder<Enchantment>> combined = new Object2IntOpenHashMap<>();
        levels.forEach((holder, list) -> combined.put(holder, combine(holder, list)));
        List<Holder<Enchantment>> order = new ArrayList<>(combined.keySet());
        order.sort(Comparator.<Holder<Enchantment>, Boolean>comparing(h -> !own.contains(h))
                .thenComparing(h -> -combined.getInt(h))
                .thenComparing(MergeEnchantmentsOutputEntry::key));
        boolean book = result.is(Items.ENCHANTED_BOOK);
        var merged = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (Holder<Enchantment> holder : order) {
            if (!accepts(result, book, merged, holder)) continue;
            merged.set(holder, combined.getInt(holder));
        }
        EnchantmentHelper.setEnchantments(result, merged.toImmutable());
        return result;
    }

    private static void collect(Map<Holder<Enchantment>, IntList> levels, ItemEnchantments from) {
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : from.entrySet()) {
            levels.computeIfAbsent(entry.getKey(), k -> new IntArrayList()).add(entry.getIntValue());
        }
    }

    private int combine(Holder<Enchantment> holder, IntList list) {
        list.sort(null);
        int level = list.getInt(0);
        for (int i = 1; i < list.size(); i++) {
            int next = list.getInt(i);
            level = level == next ? level + 1 : Math.max(level, next);
        }
        int cap = maxLevel != null ? maxLevel : Math.max(holder.value().getMaxLevel(), list.getInt(list.size() - 1));
        return Math.min(level, cap);
    }

    private boolean accepts(ItemStack result, boolean book, ItemEnchantments.Mutable merged, Holder<Enchantment> holder) {
        if (!allowUnsupported && !book && !result.supportsEnchantment(holder)) return false;
        if (allowConflicting) return true;
        for (Holder<Enchantment> other : merged.keySet()) {
            if (!Enchantment.areCompatible(other, holder)) return false;
        }
        return true;
    }

    private static String key(Holder<Enchantment> holder) {
        return holder.unwrapKey().map(k -> k.location().toString()).orElse("");
    }

    @Override
    public boolean canOutput(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (storages == null) return false;
        ItemStack stack = result(state);
        if (stack.isEmpty()) return true;
        int remaining = 1;
        for (ItemPortStorage storage : storages.getOutputItemStorages(stack.getItem())) {
            remaining = storage.canInsert(stack, remaining);
            if (remaining <= 0) return true;
        }
        return false;
    }

    @Override
    public void output(Level level, RecipeStorages storages, RecipeStateModel state) {
        ItemStack stack = result(state);
        if (stack.isEmpty()) return;
        int remaining = 1;
        for (ItemPortStorage storage : storages.getOutputItemStorages(stack.getItem())) {
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
                .addIngredients(MMJeiIngredients.ITEM, displayStacks())
                .addRichTooltipCallback((v, list) -> {
                    list.add(Component.translatable("jei.mm.recipe.merge_enchantments").withStyle(ChatFormatting.DARK_AQUA));
                    if (maxLevel != null) {
                        list.add(Component.translatable("jei.mm.recipe.merge_enchantments.max_level", maxLevel).withStyle(ChatFormatting.DARK_AQUA));
                    }
                });
    }

    private List<ItemStack> displayStacks() {
        if (item != null) return List.of(new ItemStack(item));
        if (tag != null) {
            List<ItemStack> stacks = new ArrayList<>();
            BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(h -> stacks.add(new ItemStack(h)));
            if (!stacks.isEmpty()) return stacks;
        }
        return List.of(new ItemStack(Items.ENCHANTED_BOOK));
    }

    @Override
    public JsonObject debugExpected(Level level, RecipeStorages storages, RecipeStateModel model, JsonObject json) {
        json.addProperty("item", item != null ? BuiltInRegistries.ITEM.getKey(item).toString() : tag != null ? "#" + tag.location() : "");
        if (maxLevel != null) json.addProperty("maxLevel", maxLevel);
        json.addProperty("allowConflicting", allowConflicting);
        json.addProperty("allowUnsupported", allowUnsupported);
        json.addProperty("result", result(model).toString());
        return json;
    }
}

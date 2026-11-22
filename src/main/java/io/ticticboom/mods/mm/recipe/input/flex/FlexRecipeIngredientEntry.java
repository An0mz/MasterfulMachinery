package io.ticticboom.mods.mm.recipe.input.flex;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.compat.jei.NoCountItemRenderer;
import io.ticticboom.mods.mm.compat.jei.SlotBadgeDrawable;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.SlotGridEntry;
import io.ticticboom.mods.mm.compat.mysticalagriculture.MACrops;
import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.item.BaseItemPortIngredient;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.recipe.OutputSimulation;
import io.ticticboom.mods.mm.recipe.RecipeBlockReason;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.recipe.flex.FlexCost;
import io.ticticboom.mods.mm.recipe.input.IRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.output.flex.FlexRecipeOutputEntry;
import io.ticticboom.mods.mm.util.ChanceUtils;
import lombok.Getter;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

public class FlexRecipeIngredientEntry implements IRecipeIngredientEntry {

    public static final int JEI_MAX_COPIES = 9;
    public static final int JEI_SLOT_LIMIT = 100;
    public static final String JEI_PAGE = "_jeiPage";
    public static final String JEI_PAGE_TIER = "_jeiPageTier";
    public static final String JEI_PAGE_PART = "_jeiPagePart";

    public record JeiPage(int tier, String part, List<ItemStack> items) {
    }

    @Getter
    private final String name;
    @Getter
    private final BaseItemPortIngredient ingredient;
    @Getter
    private final int maxTypes;
    private final double chance;
    private final List<Predicate<ItemStack>> exclude;
    @Nullable
    private final Integer minTier;
    @Nullable
    private final Integer maxTier;
    @Nullable
    private FlexCost cost;
    private final List<FlexRecipeOutputEntry> outputs = new ArrayList<>();
    private final List<FlexMatchIngredientEntry> matches = new ArrayList<>();
    private volatile List<ItemStack> accepted;
    private volatile List<JeiPage> pages;
    private final List<IRecipeSlotBuilder> jeiGroup = new ArrayList<>();

    public FlexRecipeIngredientEntry(String name, BaseItemPortIngredient ingredient, int maxTypes, double chance,
                                     List<Predicate<ItemStack>> exclude, @Nullable Integer minTier, @Nullable Integer maxTier) {
        this.name = name;
        this.ingredient = ingredient;
        this.maxTypes = maxTypes;
        this.chance = chance;
        this.exclude = exclude;
        this.minTier = minTier;
        this.maxTier = maxTier;
    }

    public void bindMatch(FlexMatchIngredientEntry match) {
        matches.add(match);
    }

    public void bindOutput(FlexRecipeOutputEntry output) {
        outputs.add(output);
    }

    public void bindCost(@Nullable FlexCost cost) {
        this.cost = cost;
    }

    public int jeiCopies(RecipeModel model) {
        if (maxTypes == Integer.MAX_VALUE || !MMConfig.JEI_FLEX_INPUT_COPIES) {
            return 1;
        }
        return Math.max(1, Math.min(Math.min(maxTypes, JEI_MAX_COPIES), examplesFor(model).size()));
    }

    public void clearJeiGroup() {
        jeiGroup.clear();
    }

    public void addToJeiGroup(IRecipeSlotBuilder slot) {
        jeiGroup.add(slot);
    }

    public List<IRecipeSlotBuilder> getJeiGroup() {
        return jeiGroup;
    }

    @Override
    public int jeiSlotCount(RecipeModel model) {
        return jeiCopies(model);
    }

    private void setPicked(RecipeStateModel state, List<ItemStack> picked) {
        state.setCaptured(name, picked);
        state.setCount(name, picked.size());
        state.setCostMultiplier((cost == null ? FlexCost.global() : cost).multiplier(picked.size()));
    }

    public boolean dropLastKind(RecipeStateModel state) {
        var picked = state.getCaptured(name);
        if (picked.size() <= 1) {
            return false;
        }
        setPicked(state, picked.subList(0, picked.size() - 1));
        return true;
    }

    public boolean accepts(@Nullable Level level, ItemStack stack) {
        if (!ingredient.matches(stack)) {
            return false;
        }
        for (Predicate<ItemStack> excluded : exclude) {
            if (excluded.test(stack)) {
                return false;
            }
        }
        if (minTier != null || maxTier != null) {
            MACrops.Info crop = MACrops.forSeeds(stack.getItem());
            if (crop == null || (minTier != null && crop.tier() < minTier) || (maxTier != null && crop.tier() > maxTier)) {
                return false;
            }
        }
        for (FlexMatchIngredientEntry match : matches) {
            if (match.requiredFor(stack).isEmpty()) {
                return false;
            }
        }
        for (FlexRecipeOutputEntry output : outputs) {
            if (output.resolve(level, stack).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private List<ItemStack> pick(Level level, @Nullable RecipeStorages storages, RecipeStateModel state) {
        var picked = new ArrayList<ItemStack>();
        if (storages == null) {
            return picked;
        }
        var itemStorages = storages.getInputStorages(ItemPortStorage.class);
        var candidates = new ArrayList<ItemStack>();
        for (ItemPortStorage storage : itemStorages) {
            var handler = storage.getHandler();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty() && ingredient.matches(stack) && !containsKind(candidates, stack)) {
                    candidates.add(stack);
                }
            }
        }
        if (state.getFlexInputOrder() != null) {
            candidates.sort(state.getFlexInputOrder());
        }
        var reserved = new HashMap<Item, Integer>();
        OutputSimulation simulation = null;
        boolean outputsBlocked = false;
        for (int i = 0; i < candidates.size() && picked.size() < maxTypes; i++) {
            ItemStack stack = candidates.get(i);
            if (!accepts(level, stack) || !enoughOf(itemStorages, stack) || !matchesAvailable(itemStorages, stack, reserved)) {
                continue;
            }
            var kindOutputs = new ArrayList<IPortIngredient>();
            for (FlexRecipeOutputEntry output : outputs) {
                IPortIngredient made = output.outputFor(level, stack, state);
                if (made != null) {
                    kindOutputs.add(made);
                }
            }
            if (!kindOutputs.isEmpty() && simulation == null) {
                simulation = new OutputSimulation(level, storages);
            }
            if (kindOutputs.isEmpty() || simulation.tryAdd(level, state, kindOutputs)) {
                picked.add(stack.copyWithCount(1));
                for (FlexMatchIngredientEntry match : matches) {
                    reserved.merge(match.requiredFor(stack).getItem(), match.getCount(), Integer::sum);
                }
            } else {
                outputsBlocked = true;
            }
        }
        if (picked.isEmpty() && outputsBlocked) {
            state.markBlocked(RecipeBlockReason.Kind.OUTPUT, -1);
        }
        return picked;
    }

    private static boolean containsKind(List<ItemStack> stacks, ItemStack stack) {
        for (ItemStack each : stacks) {
            if (ItemStack.isSameItemSameComponents(each, stack)) {
                return true;
            }
        }
        return false;
    }

    private boolean enoughOf(List<ItemPortStorage> itemStorages, ItemStack stack) {
        int remaining = ingredient.getCount();
        Predicate<ItemStack> same = exactly(stack);
        for (ItemPortStorage s : itemStorages) {
            remaining = s.canExtract(same, remaining);
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesAvailable(List<ItemPortStorage> itemStorages, ItemStack kind, Map<Item, Integer> reserved) {
        var needed = new HashMap<Item, Integer>();
        for (FlexMatchIngredientEntry match : matches) {
            needed.merge(match.requiredFor(kind).getItem(), match.getCount(), Integer::sum);
        }
        for (var entry : needed.entrySet()) {
            Item item = entry.getKey();
            int remaining = entry.getValue() + reserved.getOrDefault(item, 0);
            for (ItemPortStorage s : itemStorages) {
                remaining = s.canExtract(st -> st.is(item), remaining);
                if (remaining <= 0) {
                    break;
                }
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    private static Predicate<ItemStack> exactly(ItemStack stack) {
        ItemStack copy = stack.copyWithCount(1);
        return s -> ItemStack.isSameItemSameComponents(s, copy);
    }

    @Override
    public boolean canProcess(Level level, RecipeStorages storages, RecipeStateModel state) {
        var picked = pick(level, storages, state);
        setPicked(state, picked);
        return !picked.isEmpty();
    }

    @Override
    public void process(Level level, RecipeStorages storages, RecipeStateModel state) {
        var picked = state.getCaptured(name);
        if (picked.isEmpty()) {
            picked = pick(level, storages, state);
            setPicked(state, picked);
        }
        var itemStorages = storages.getInputStorages(ItemPortStorage.class);
        for (ItemStack kind : picked) {
            if (chance <= 0 || !ChanceUtils.shouldProceed(chance)) {
                continue;
            }
            int remaining = ingredient.getCount();
            Predicate<ItemStack> same = exactly(kind);
            for (ItemPortStorage storage : itemStorages) {
                remaining = storage.extract(same, remaining);
                if (remaining <= 0) {
                    break;
                }
            }
        }
    }

    @Override
    public void ditchRecipe(Level level, RecipeStorages storages, RecipeStateModel state) {
    }

    public List<JeiPage> jeiPages() {
        var cachedPages = pages;
        if (cachedPages != null) {
            return cachedPages;
        }
        var groups = new TreeMap<Integer, List<ItemStack>>();
        for (ItemStack item : acceptedExamples()) {
            MACrops.Info crop = MACrops.forSeeds(item.getItem());
            groups.computeIfAbsent(crop == null ? Integer.MAX_VALUE : crop.tier(), k -> new ArrayList<>()).add(item);
        }
        var result = new ArrayList<JeiPage>();
        for (var group : groups.entrySet()) {
            List<ItemStack> items = group.getValue();
            int tier = group.getKey() == Integer.MAX_VALUE ? -1 : group.getKey();
            int parts = (items.size() + JEI_SLOT_LIMIT - 1) / JEI_SLOT_LIMIT;
            for (int i = 0; i < parts; i++) {
                var chunk = items.subList(i * JEI_SLOT_LIMIT, Math.min(items.size(), (i + 1) * JEI_SLOT_LIMIT));
                result.add(new JeiPage(tier, parts > 1 ? (i + 1) + "/" + parts : "", List.copyOf(chunk)));
            }
        }
        if (accepted != null) {
            pages = List.copyOf(result);
        }
        return result;
    }

    public List<ItemStack> examplesFor(RecipeModel model) {
        if (model.config() == null || !model.config().has(JEI_PAGE)) {
            return acceptedExamples();
        }
        int page = model.config().get(JEI_PAGE).getAsInt();
        var pages = jeiPages();
        return page >= 0 && page < pages.size() ? pages.get(page).items() : List.of();
    }

    public List<ItemStack> acceptedExamples() {
        var cached = accepted;
        if (cached != null) {
            return cached;
        }
        var list = new ArrayList<ItemStack>();
        for (ItemStack example : ingredient.exampleStacks()) {
            if (accepts(null, example)) {
                list.add(example.copyWithCount(1));
            }
        }
        if (!list.isEmpty()) {
            accepted = List.copyOf(list);
        }
        return list;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid) {
        List<ItemStack> examples = examplesFor(model);
        int copies = jeiCopies(model);
        List<Component> kindLines = maxTypesTooltip();
        for (int i = 0; i < copies; i++) {
            addJeiSlot(builder, helpers, grid, examples, kindLines);
        }
    }

    private List<Component> maxTypesTooltip() {
        if (maxTypes <= 1 || maxTypes == Integer.MAX_VALUE) {
            return List.of();
        }
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        for (ItemStack item : acceptedExamples()) {
            MACrops.Info crop = MACrops.forSeeds(item.getItem());
            if (crop != null) {
                lowest = Math.min(lowest, crop.tier());
                highest = Math.max(highest, crop.tier());
            }
        }
        Component kinds;
        if (lowest == Integer.MAX_VALUE) {
            kinds = Component.translatable("jei.mm.recipe.flex_max_types", maxTypes);
        } else if (lowest == highest) {
            kinds = Component.translatable("jei.mm.recipe.flex_max_types_tier", maxTypes, lowest);
        } else {
            kinds = Component.translatable("jei.mm.recipe.flex_max_types_tiers", maxTypes, lowest, highest);
        }
        return List.of(kinds.copy().withStyle(ChatFormatting.GOLD),
                Component.translatable("jei.mm.recipe.flex_mixed").withStyle(ChatFormatting.GOLD));
    }

    private void addJeiSlot(IRecipeLayoutBuilder builder, IJeiHelpers helpers, SlotGrid grid, List<ItemStack> items,
                            List<Component> kindLines) {
        SlotGridEntry slot = grid.next();
        slot.setUsed();
        var rSlot = builder.addSlot(RecipeIngredientRole.INPUT, slot.getInnerX(), slot.getInnerY());
        int count = ingredient.getCount();
        slot.setBadgeCount(count);
        if (chance <= 0) {
            slot.setBadgeNotUsed();
        }
        if (count > 1 || chance <= 0) {
            rSlot.setOverlay(new SlotBadgeDrawable(count, chance <= 0), 0, 0);
        }
        rSlot.addItemStacks(items);
        addToJeiGroup(rSlot);
        var defaultRenderer = helpers.getIngredientManager().getIngredientRenderer(VanillaTypes.ITEM_STACK);
        rSlot.setCustomRenderer(VanillaTypes.ITEM_STACK, new NoCountItemRenderer(defaultRenderer));
        rSlot.addRichTooltipCallback((v, list) -> {
            list.add(Component.translatable("jei.mm.recipe.flex_input").withStyle(ChatFormatting.DARK_AQUA));
            list.addAll(kindLines);
            if (chance <= 0) {
                list.add(Component.translatable("jei.mm.not_used").withStyle(ChatFormatting.DARK_AQUA));
            } else if (chance < 1) {
                list.add(Component.translatable("jei.mm.recipe.chance_of_consumption", String.format("%.2f", chance * 100)).withStyle(ChatFormatting.DARK_AQUA));
            }
        });
    }

    @Override
    public JsonObject debugExpected(Level level, RecipeStorages storages, RecipeStateModel state, JsonObject json) {
        json.addProperty("as", name);
        json.addProperty("maxTypes", maxTypes);
        json.addProperty("chance", chance);
        if (minTier != null) json.addProperty("minTier", minTier);
        if (maxTier != null) json.addProperty("maxTier", maxTier);
        json.addProperty("captured", String.valueOf(state.getCaptured(name)));
        json.add("ingredient", ingredient.debugInput(level, storages, new JsonObject()));
        return json;
    }
}

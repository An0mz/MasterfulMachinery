package io.ticticboom.mods.mm.recipe.output.flex;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.interop.MMInteropManager;
import io.ticticboom.mods.mm.compat.jei.NoCountItemRenderer;
import io.ticticboom.mods.mm.compat.jei.SlotBadgeDrawable;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.SlotGridEntry;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.item.SingleItemPortIngredient;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.recipe.flex.FlexItems;
import io.ticticboom.mods.mm.recipe.flex.FlexTemplate;
import io.ticticboom.mods.mm.recipe.input.flex.FlexRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.output.DisplayedOutput;
import io.ticticboom.mods.mm.recipe.output.IRecipeOutputEntry;
import io.ticticboom.mods.mm.util.AmountRange;
import lombok.Getter;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FlexRecipeOutputEntry implements IRecipeOutputEntry {

    private record Key(Item item, DataComponentPatch components) {
    }

    private Key keyOf(ItemStack stack) {
        return new Key(stack.getItem(), copyComponents ? stack.getComponentsPatch() : DataComponentPatch.EMPTY);
    }

    private record OutputKey(Key item, int amount) {
    }

    @Getter
    private final String from;
    private final FlexTemplate template;
    private final int count;
    private final boolean copyComponents;
    private final double chance;
    private final boolean perTick;
    @Getter
    @Nullable
    private final String scaleBy;
    private final String chanceRollKey;

    private ResourceLocation recipeId;
    private FlexRecipeIngredientEntry input;

    private final Map<Key, ItemStack> templateResults = new ConcurrentHashMap<>();
    private final Map<Key, ItemStack> serverResults = new ConcurrentHashMap<>();
    private final Map<OutputKey, SingleItemPortIngredient> outputIngredients = new ConcurrentHashMap<>();
    private volatile List<DisplayedOutput> displayed;

    public FlexRecipeOutputEntry(String from, FlexTemplate template, int count, boolean copyComponents, double chance, boolean perTick, @Nullable String scaleBy) {
        this.from = from;
        this.template = template;
        this.count = count;
        this.copyComponents = copyComponents;
        this.chance = chance;
        this.perTick = perTick;
        this.scaleBy = scaleBy;
        this.chanceRollKey = "f" + AmountRange.nextRollKey();
    }

    public void bind(ResourceLocation recipeId, FlexRecipeIngredientEntry input) {
        this.recipeId = recipeId;
        this.input = input;
    }

    public ItemStack resolve(@Nullable Level level, ItemStack inputStack) {
        if (inputStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Key key = keyOf(inputStack);
        ItemStack templated = templateResults.computeIfAbsent(key, k -> fromTemplate(inputStack));
        if (level == null || level.isClientSide() || MMInteropManager.KUBEJS.isEmpty()) {
            return templated;
        }
        return serverResults.computeIfAbsent(key, k -> {
            ItemStack picked = MMInteropManager.KUBEJS.get().resolveFlexOutput(recipeId, from, inputStack.copyWithCount(1), templated.copy());
            return picked.isEmpty() ? ItemStack.EMPTY : picked.copyWithCount(1);
        });
    }

    private ItemStack fromTemplate(ItemStack inputStack) {
        ItemStack result = FlexItems.resolve(template, inputStack);
        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (copyComponents && !inputStack.getComponentsPatch().isEmpty()) {
            result.applyComponents(inputStack.getComponentsPatch());
        }
        return result;
    }

    private int amount(RecipeStateModel state) {
        return scaleBy == null ? count : IPortIngredient.scaleAmount(count, state.getCount(scaleBy));
    }

    private boolean shouldRun(@Nullable RecipeStateModel state) {
        return chance >= 1 || (state != null && chance >= state.getRollToken(chanceRollKey));
    }

    @Nullable
    public SingleItemPortIngredient outputFor(Level level, ItemStack captured, RecipeStateModel state) {
        ItemStack result = resolve(level, captured);
        int amount = amount(state);
        if (result.isEmpty() || amount <= 0) {
            return null;
        }
        return outputIngredients.computeIfAbsent(new OutputKey(new Key(result.getItem(), result.getComponentsPatch()), amount), k ->
                new SingleItemPortIngredient(BuiltInRegistries.ITEM.getKey(result.getItem()), AmountRange.of(amount),
                        null, false, result.getComponentsPatch()));
    }

    private List<IPortIngredient> outputsFor(Level level, RecipeStateModel state) {
        var outputs = new ArrayList<IPortIngredient>();
        for (ItemStack captured : state.getCaptured(from)) {
            SingleItemPortIngredient output = outputFor(level, captured, state);
            if (output == null) {
                Ref.LOG.debug("Flex recipe {} has no output for {}", recipeId, captured);
                continue;
            }
            outputs.add(output);
        }
        return outputs;
    }

    @Override
    public boolean canOutput(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (!shouldRun(state)) {
            return true;
        }
        var outputs = outputsFor(level, state);
        if (outputs.size() == 1) {
            return outputs.get(0).canOutput(level, storages, state);
        }
        return outputs.isEmpty() || io.ticticboom.mods.mm.recipe.OutputSimulation.fitTogether(level, storages, state, outputs);
    }

    @Override
    public void output(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (!perTick && shouldRun(state)) {
            for (IPortIngredient output : outputsFor(level, state)) {
                output.output(level, storages, state);
            }
        }
    }

    @Override
    public void processTick(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (perTick && shouldRun(state)) {
            for (IPortIngredient output : outputsFor(level, state)) {
                output.output(level, storages, state);
            }
        }
    }

    @Override
    public List<IPortIngredient> plannedOutputs(Level level, RecipeStateModel state) {
        return !perTick && shouldRun(state) ? outputsFor(level, state) : List.of();
    }

    @Override
    public List<IPortIngredient> plannedTickOutputs(Level level, RecipeStateModel state) {
        return perTick && chance >= 1 ? outputsFor(level, state) : List.of();
    }

    @Override
    public void ditchRecipe(Level level, RecipeStorages storages, RecipeStateModel state) {
    }

    private List<ItemStack> exampleOutputs(List<ItemStack> inputs) {
        var outputs = new ArrayList<ItemStack>();
        for (ItemStack example : inputs) {
            outputs.add(resolve(null, example).copyWithCount(count));
        }
        return outputs;
    }

    @Override
    public List<DisplayedOutput> displayedOutputs() {
        var cached = displayed;
        if (cached != null) {
            return cached;
        }
        var outputs = input == null ? List.<ItemStack>of() : exampleOutputs(input.acceptedExamples());
        var result = new ArrayList<DisplayedOutput>();
        for (ItemStack output : outputs) {
            if (!output.isEmpty()) {
                var ingredient = new SingleItemPortIngredient(BuiltInRegistries.ITEM.getKey(output.getItem()), AmountRange.of(count),
                        null, false, output.getComponentsPatch());
                result.add(new DisplayedOutput(ingredient, chance));
                break;
            }
        }
        if (result.isEmpty()) {
            return List.of();
        }
        displayed = List.copyOf(result);
        return displayed;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid) {
        var outputs = input == null ? List.<ItemStack>of() : exampleOutputs(input.examplesFor(model));
        if (outputs.isEmpty()) {
            return;
        }
        SlotGridEntry slot = grid.next();
        slot.setUsed();
        var rSlot = builder.addSlot(RecipeIngredientRole.OUTPUT, slot.getInnerX(), slot.getInnerY());
        slot.setBadgeCount(count);
        if (count > 1) {
            rSlot.setOverlay(new SlotBadgeDrawable(count, false), 0, 0);
        }
        rSlot.addItemStacks(outputs);
        var defaultRenderer = helpers.getIngredientManager().getIngredientRenderer(VanillaTypes.ITEM_STACK);
        rSlot.setCustomRenderer(VanillaTypes.ITEM_STACK, new NoCountItemRenderer(defaultRenderer));
        if (input != null) {
            input.addToJeiGroup(rSlot);
        }
        double percent = chance * 100.0;
        String percentStr = new java.math.BigDecimal(Double.toString(percent)).setScale(4, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        rSlot.addRichTooltipCallback((v, list) -> {
            list.add(Component.translatable("jei.mm.recipe.flex_output").withStyle(ChatFormatting.DARK_AQUA));
            if (chance < 1) {
                list.add(Component.translatable("jei.mm.recipe.chance_of_output", percentStr).withStyle(ChatFormatting.DARK_AQUA));
            }
            if (perTick) {
                list.add(Component.translatable("jei.mm.recipe.output_per_tick").withStyle(ChatFormatting.DARK_AQUA));
            }
            if (scaleBy != null) {
                list.add(Component.translatable("jei.mm.recipe.scaled_by", scaleBy).withStyle(ChatFormatting.DARK_AQUA));
            }
        });
    }

    @Override
    public int jeiSlotCount(RecipeModel model) {
        return 1;
    }

    @Override
    public JsonObject debugExpected(Level level, RecipeStorages storages, RecipeStateModel model, JsonObject json) {
        json.addProperty("from", from);
        json.addProperty("chance", chance);
        json.addProperty("perTick", perTick);
        json.addProperty("amount", amount(model));
        var resolved = new JsonArray();
        for (ItemStack captured : model.getCaptured(from)) {
            resolved.add(String.valueOf(resolve(level, captured)));
        }
        json.add("resolved", resolved);
        return json;
    }
}

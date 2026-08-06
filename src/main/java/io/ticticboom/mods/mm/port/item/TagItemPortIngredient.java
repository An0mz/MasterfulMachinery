package io.ticticboom.mods.mm.port.item;

import io.ticticboom.mods.mm.util.ItemNbtUtil;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.util.ConditionalLazy;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.nbt.CompoundTag;

public class TagItemPortIngredient extends BaseItemPortIngredient {

    private final TagKey<Item> tag;
    private final ConditionalLazy<List<ItemStack>> stacks;

    public TagItemPortIngredient(ResourceLocation tagId, int count, CompoundTag requiredNbt, boolean nbtStrong) {
        super(count, createPredicate(tagId), requiredNbt, nbtStrong);
        this.tag = ItemTags.create(tagId);
        // Registry.getTag returns an Optional<HolderSet.Named<Item>> of holders rather than
        // Forge's ITag of values, so an absent tag yields an empty list instead of a null.
        stacks = ConditionalLazy.create(() -> BuiltInRegistries.ITEM.getTag(tag)
                .map(holders -> holders.stream().map(holder -> {
                    // use display stacks with the real required count so external transfer/encode handlers
                    // that read the ItemStack count (rather than the JEI badge) get the correct amount.
                    var s = new ItemStack(holder.value(), count);
                    if (requiredNbt != null) ItemNbtUtil.setTag(s, requiredNbt.copy());
                    return s;
                }).toList())
                .orElseGet(List::of),
                () -> BuiltInRegistries.ITEM.getTag(tag).map(holders -> holders.size() > 0).orElse(false), List.of());
    }

    private static Predicate<ItemStack> createPredicate(ResourceLocation id) {
        var key = ItemTags.create(id);
        return  i -> i.is(key);
    }

    @Override
    public boolean canOutput(Level level, RecipeStorages storages, RecipeStateModel state) {
        return false;
    }

    @Override
    public void output(Level level, RecipeStorages storages, RecipeStateModel state) {
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid, IRecipeSlotBuilder recipeSlot) {
        recipeSlot.addItemStacks(stacks.get());
    }

    @Override
    public JsonObject debugOutput(Level level, RecipeStorages storages, JsonObject json) {
        json.addProperty("isTag", true);
        json.addProperty("WILL_NEVER_WORK", true);
        if (requiredNbt != null) {
            json.addProperty("nbt_match", nbtStrong ? "strong" : "weak");
            json.add("nbt", io.ticticboom.mods.mm.util.NbtMatchUtils.toJson(requiredNbt));
        }
        return json;
    }
}

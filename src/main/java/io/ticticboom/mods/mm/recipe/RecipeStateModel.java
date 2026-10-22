package io.ticticboom.mods.mm.recipe;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Getter
@Setter
@NoArgsConstructor
public class RecipeStateModel {
    private static final Random ROLL_RANDOM = new Random();
    private static final String ROLL_TOKENS_KEY = "rollTokens";
    private static final String CONSUMED_ITEMS_KEY = "consumedItems";

    private boolean canProcess = false;
    private int tickProgress = 0;
    private double tickPercentage = 0;
    private boolean canFinish = false;
    private double progressCarry = 0;

    private final Map<String, Double> rollTokens = new HashMap<>();

    @Nullable
    private List<ItemStack> consumedItems = null;

    public void trackConsumedItems() {
        if (consumedItems == null) {
            consumedItems = new ArrayList<>();
        }
    }

    public void proceedTick() {
        proceedTick(1.0);
    }

    public void proceedTick(double speed) {
        if (speed <= 1.0) {
            tickProgress++;
            return;
        }
        progressCarry += speed;
        int steps = (int) progressCarry;
        progressCarry -= steps;
        tickProgress += Math.max(1, steps);
    }

    public double getRollToken(String key) {
        var existing = rollTokens.get(key);
        if (existing != null) {
            return existing;
        }
        double token = ROLL_RANDOM.nextDouble();
        rollTokens.put(key, token);
        return token;
    }

    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("canProcess", canProcess);
        tag.putInt("tickProgress", tickProgress);
        tag.putDouble("tickPercentage", tickPercentage);
        tag.putBoolean("canFinish", canFinish);
        tag.putDouble("progressCarry", progressCarry);
        if (!rollTokens.isEmpty()) {
            var rolls = new CompoundTag();
            rollTokens.forEach(rolls::putDouble);
            tag.put(ROLL_TOKENS_KEY, rolls);
        }
        if (consumedItems != null) {
            var items = new ListTag();
            for (ItemStack stack : consumedItems) {
                if (!stack.isEmpty()) items.add(stack.save(registries));
            }
            tag.put(CONSUMED_ITEMS_KEY, items);
        }
        return tag;
    }

    public static RecipeStateModel load(CompoundTag tag, HolderLookup.Provider registries) {
        RecipeStateModel model = new RecipeStateModel();
        model.setCanProcess(tag.getBoolean("canProcess"));
        model.setTickProgress(tag.getInt("tickProgress"));
        model.setTickPercentage(tag.getDouble("tickPercentage"));
        model.setCanFinish(tag.getBoolean("canFinish"));
        model.setProgressCarry(tag.getDouble("progressCarry"));
        if (tag.contains(ROLL_TOKENS_KEY)) {
            var rolls = tag.getCompound(ROLL_TOKENS_KEY);
            for (String key : rolls.getAllKeys()) {
                model.rollTokens.put(key, rolls.getDouble(key));
            }
        }
        if (tag.contains(CONSUMED_ITEMS_KEY)) {
            model.trackConsumedItems();
            for (Tag item : tag.getList(CONSUMED_ITEMS_KEY, Tag.TAG_COMPOUND)) {
                ItemStack.parse(registries, item).ifPresent(model.consumedItems::add);
            }
        }
        return model;
    }

}

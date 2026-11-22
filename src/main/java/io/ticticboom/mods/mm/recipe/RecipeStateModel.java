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
import java.util.Comparator;
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
    private RecipeBlockReason.Kind blockedKind;
    private int blockedIndex = -1;

    private final Map<String, Double> rollTokens = new HashMap<>();
    private final Map<String, List<ItemStack>> captures = new HashMap<>();
    private final Map<String, Integer> counts = new HashMap<>();
    private double costMultiplier = 1;
    @Nullable
    private Comparator<ItemStack> flexInputOrder;

    @Nullable
    private List<ItemStack> consumedItems = null;

    public void markBlocked(@Nullable RecipeBlockReason.Kind kind, int index) {
        blockedKind = kind;
        blockedIndex = index;
    }

    @Nullable
    public RecipeBlockReason.Kind getBlockedKind() {
        return blockedKind;
    }

    public int getBlockedIndex() {
        return blockedIndex;
    }

    public List<ItemStack> getCaptured(String name) {
        return captures.getOrDefault(name, List.of());
    }

    public void setCaptured(String name, List<ItemStack> stacks) {
        if (stacks.isEmpty()) {
            captures.remove(name);
            return;
        }
        var copies = new ArrayList<ItemStack>(stacks.size());
        for (ItemStack stack : stacks) {
            copies.add(stack.copyWithCount(1));
        }
        captures.put(name, List.copyOf(copies));
    }

    public int getCount(String name) {
        return counts.getOrDefault(name, 1);
    }

    public void setCount(String name, int count) {
        counts.put(name, count);
    }

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
        if (!captures.isEmpty()) {
            var capturesTag = new CompoundTag();
            captures.forEach((name, stacks) -> {
                var list = new ListTag();
                for (ItemStack stack : stacks) {
                    list.add(stack.save(registries));
                }
                capturesTag.put(name, list);
            });
            tag.put("captures", capturesTag);
        }
        if (!counts.isEmpty()) {
            var countsTag = new CompoundTag();
            counts.forEach(countsTag::putInt);
            tag.put("counts", countsTag);
        }
        if (costMultiplier != 1) {
            tag.putDouble("costMultiplier", costMultiplier);
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
        var capturesTag = tag.getCompound("captures");
        for (String name : capturesTag.getAllKeys()) {
            var stacks = new ArrayList<ItemStack>();
            for (Tag item : capturesTag.getList(name, Tag.TAG_COMPOUND)) {
                ItemStack.parse(registries, item).ifPresent(stacks::add);
            }
            model.setCaptured(name, stacks);
        }
        var countsTag = tag.getCompound("counts");
        for (String name : countsTag.getAllKeys()) {
            model.setCount(name, countsTag.getInt(name));
        }
        if (tag.contains("costMultiplier")) {
            model.setCostMultiplier(tag.getDouble("costMultiplier"));
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

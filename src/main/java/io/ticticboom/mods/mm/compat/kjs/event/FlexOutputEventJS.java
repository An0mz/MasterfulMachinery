package io.ticticboom.mods.mm.compat.kjs.event;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.world.item.ItemStack;

public class FlexOutputEventJS implements KubeEvent {
    private final String recipeId;
    private final String from;
    private final ItemStack input;
    private final ItemStack templateOutput;
    private ItemStack result;

    public FlexOutputEventJS(String recipeId, String from, ItemStack input, ItemStack templateOutput) {
        this.recipeId = recipeId;
        this.from = from;
        this.input = input;
        this.templateOutput = templateOutput;
        this.result = templateOutput;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public String getFrom() {
        return from;
    }

    public ItemStack getInput() {
        return input.copy();
    }

    public ItemStack getOutput() {
        return templateOutput.copy();
    }

    public void setOutput(ItemStack output) {
        result = output == null ? ItemStack.EMPTY : output.copy();
    }

    public void reject() {
        result = ItemStack.EMPTY;
    }

    public ItemStack getResult() {
        return result;
    }
}

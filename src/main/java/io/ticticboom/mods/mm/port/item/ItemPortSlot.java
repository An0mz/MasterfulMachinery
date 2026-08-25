package io.ticticboom.mods.mm.port.item;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ItemPortSlot extends Slot {

    private final ItemPortHandler handler;
    private final int handlerIndex;

    public ItemPortSlot(ItemPortContainer container, int index, int x, int y) {
        super(container, index, x, y);
        this.handler = container.getHandler();
        this.handlerIndex = index;
    }

    @Override
    public int getMaxStackSize() {
        return handler.getSlotLimit(handlerIndex);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        if (!stack.isStackable()) {
            return 1;
        }
        return handler.getSlotLimit(handlerIndex);
    }
}

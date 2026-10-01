package io.ticticboom.mods.mm.gateway;

import io.ticticboom.mods.mm.cap.MMCapabilities;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.item.ItemPortHandler;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class GatewayItemHandler implements IItemHandler {
    private final Supplier<List<IPortStorage>> inputs;

    public GatewayItemHandler(Supplier<List<IPortStorage>> inputs) {
        this.inputs = inputs;
    }

    private List<IItemHandler> handlers() {
        var result = new ArrayList<IItemHandler>();
        for (IPortStorage storage : inputs.get()) {
            var handler = storage.getCapability(MMCapabilities.ITEM);
            if (handler != null) {
                result.add(handler);
            }
        }
        return result;
    }

    @Override
    public int getSlots() {
        int slots = 1;
        for (IItemHandler handler : handlers()) {
            slots += handler.getSlots();
        }
        return slots;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        for (IItemHandler handler : handlers()) {
            if (slot < handler.getSlots()) {
                return handler.getStackInSlot(slot);
            }
            slot -= handler.getSlots();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        ItemStack remaining = stack;
        for (IItemHandler handler : handlers()) {
            if (remaining.isEmpty()) {
                break;
            }
            remaining = handler instanceof ItemPortHandler port
                    ? port.insertStackFast(remaining, simulate)
                    : ItemHandlerHelper.insertItemStacked(handler, remaining, simulate);
        }
        return remaining;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        for (IItemHandler handler : handlers()) {
            if (slot < handler.getSlots()) {
                return handler.getSlotLimit(slot);
            }
            slot -= handler.getSlots();
        }
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return true;
    }
}

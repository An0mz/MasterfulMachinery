package io.ticticboom.mods.mm.port.item.feature;

import io.ticticboom.mods.mm.port.common.IHandlerCoupling;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import org.jetbrains.annotations.Nullable;

public class ItemHandlerCoupling implements IHandlerCoupling {

    /** The port's own handler. It is owned by this block entity, so it cannot go stale. */
    @Getter
    private final @Nullable IItemHandler fromHandler;

    /**
     * LazyOptional invalidated itself when the neighbour it came from went away, so a cached
     * handler could never outlive its block entity. A plain reference carries no such signal, and
     * a stale one would let auto-push keep moving contents into a detached inventory. The
     * neighbour side is therefore held as a BlockCapabilityCache, which re-resolves when the
     * target block entity is replaced or removed.
     */
    @Getter
    @Setter
    private @Nullable BlockCapabilityCache<IItemHandler, Direction> toHandler;

    public ItemHandlerCoupling(@Nullable IItemHandler fromHandler,
                               @Nullable BlockCapabilityCache<IItemHandler, Direction> toHandler) {
        this.fromHandler = fromHandler;
        this.toHandler = toHandler;
    }

    @Override
    public void attemptTransfer() {
        if (fromHandler == null || toHandler == null) {
            return;
        }
        var to = toHandler.getCapability();
        if (to == null) {
            return;
        }
        attemptTransfer(fromHandler, to);
    }
    private void attemptTransfer(IItemHandler from, IItemHandler to) {
        for (int fromSlot = 0; fromSlot < from.getSlots(); fromSlot++) {
            ItemStack extracted = from.extractItem(fromSlot, from.getSlotLimit(fromSlot), true);
            if (extracted.isEmpty()) {
                continue;
            }

            var remaining = attemptInsert(from, to, extracted, fromSlot);
            var shouldExtractCount = extracted.getCount() - remaining.getCount();
            if (shouldExtractCount > 0) {
                from.extractItem(fromSlot, shouldExtractCount, false);
            }
        }
    }

    private ItemStack attemptInsert(IItemHandler from, IItemHandler to, ItemStack toInsert, int fromSlot) {
        // First: simulate through all target slots to see how much would remain
        ItemStack simulated = toInsert.copy();
        for (int toSlot = 0; toSlot < to.getSlots(); toSlot++) {
            simulated = to.insertItem(toSlot, simulated, true);
            if (simulated.isEmpty()) {
                break;
            }
        }

        // If nothing would be accepted, return original
        if (simulated.getCount() == toInsert.getCount()) {
            return toInsert;
        }

        // Otherwise perform the real insertion pass
        ItemStack remaining = toInsert.copy();
        for (int toSlot = 0; toSlot < to.getSlots(); toSlot++) {
            remaining = to.insertItem(toSlot, remaining, false);
            if (remaining.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return remaining;
    }
}

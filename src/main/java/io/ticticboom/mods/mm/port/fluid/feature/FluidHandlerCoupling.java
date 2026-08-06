package io.ticticboom.mods.mm.port.fluid.feature;

import io.ticticboom.mods.mm.port.common.IHandlerCoupling;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import org.jetbrains.annotations.Nullable;

public class FluidHandlerCoupling implements IHandlerCoupling {

    /** The port's own handler. It is owned by this block entity, so it cannot go stale. */
    @Getter
    private final @Nullable IFluidHandler fromHandler;

    /**
     * LazyOptional invalidated itself when the neighbour it came from went away, so a cached
     * handler could never outlive its block entity. A plain reference carries no such signal, and
     * a stale one would let auto-push keep moving contents into a detached inventory. The
     * neighbour side is therefore held as a BlockCapabilityCache, which re-resolves when the
     * target block entity is replaced or removed.
     */
    @Getter
    @Setter
    private @Nullable BlockCapabilityCache<IFluidHandler, Direction> toHandler;

    public FluidHandlerCoupling(@Nullable IFluidHandler fromHandler,
                                @Nullable BlockCapabilityCache<IFluidHandler, Direction> toHandler) {
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
    private void attemptTransfer(IFluidHandler from, IFluidHandler to) {
        FluidStack extracted = from.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        if (extracted.isEmpty()) {
            return;
        }

        var remaining = to.fill(extracted, IFluidHandler.FluidAction.EXECUTE);
        if (remaining > 0) {
            from.drain(remaining, IFluidHandler.FluidAction.EXECUTE);
        }
    }
}

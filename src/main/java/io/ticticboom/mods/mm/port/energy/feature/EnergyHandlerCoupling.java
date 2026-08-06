package io.ticticboom.mods.mm.port.energy.feature;

import io.ticticboom.mods.mm.port.common.IHandlerCoupling;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import org.jetbrains.annotations.Nullable;

public class EnergyHandlerCoupling implements IHandlerCoupling {

    /** The port's own handler. It is owned by this block entity, so it cannot go stale. */
    @Getter
    private final @Nullable IEnergyStorage fromHandler;

    /**
     * LazyOptional invalidated itself when the neighbour it came from went away, so a cached
     * handler could never outlive its block entity. A plain reference carries no such signal, and
     * a stale one would let auto-push keep moving contents into a detached inventory. The
     * neighbour side is therefore held as a BlockCapabilityCache, which re-resolves when the
     * target block entity is replaced or removed.
     */
    @Getter
    @Setter
    private @Nullable BlockCapabilityCache<IEnergyStorage, Direction> toHandler;

    public EnergyHandlerCoupling(@Nullable IEnergyStorage fromHandler,
                                 @Nullable BlockCapabilityCache<IEnergyStorage, Direction> toHandler) {
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
    private void attemptTransfer(IEnergyStorage from, IEnergyStorage to) {
        int extracted = from.extractEnergy(from.getMaxEnergyStored(), true);
        if (extracted > 0) {
            int inserted = to.receiveEnergy(extracted, false);
            if (inserted > 0) {
                from.extractEnergy(inserted, false);
            }
        }
    }
}

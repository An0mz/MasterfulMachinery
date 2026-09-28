package io.ticticboom.mods.mm.gateway;

import io.ticticboom.mods.mm.cap.MMCapabilities;
import io.ticticboom.mods.mm.port.IPortStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class GatewayFluidHandler implements IFluidHandler {
    private final Supplier<List<IPortStorage>> inputs;

    public GatewayFluidHandler(Supplier<List<IPortStorage>> inputs) {
        this.inputs = inputs;
    }

    private List<IFluidHandler> handlers() {
        var result = new ArrayList<IFluidHandler>();
        for (IPortStorage storage : inputs.get()) {
            var handler = storage.getCapability(MMCapabilities.FLUID);
            if (handler != null) {
                result.add(handler);
            }
        }
        return result;
    }

    @Override
    public int getTanks() {
        int tanks = 1;
        for (IFluidHandler handler : handlers()) {
            tanks += handler.getTanks();
        }
        return tanks;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        for (IFluidHandler handler : handlers()) {
            if (tank < handler.getTanks()) {
                return handler.getFluidInTank(tank);
            }
            tank -= handler.getTanks();
        }
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        for (IFluidHandler handler : handlers()) {
            if (tank < handler.getTanks()) {
                return handler.getTankCapacity(tank);
            }
            tank -= handler.getTanks();
        }
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return true;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return 0;
        }
        int filled = 0;
        for (IFluidHandler handler : handlers()) {
            if (filled >= resource.getAmount()) {
                break;
            }
            filled += handler.fill(resource.copyWithAmount(resource.getAmount() - filled), action);
        }
        return filled;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        return FluidStack.EMPTY;
    }
}

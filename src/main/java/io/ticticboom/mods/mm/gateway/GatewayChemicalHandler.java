package io.ticticboom.mods.mm.gateway;

import io.ticticboom.mods.mm.cap.MekCapabilities;
import io.ticticboom.mods.mm.port.IPortStorage;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class GatewayChemicalHandler implements IChemicalHandler {
    private final Supplier<List<IPortStorage>> inputs;

    public GatewayChemicalHandler(Supplier<List<IPortStorage>> inputs) {
        this.inputs = inputs;
    }

    private List<IChemicalHandler> handlers() {
        var result = new ArrayList<IChemicalHandler>();
        for (IPortStorage storage : inputs.get()) {
            var handler = storage.getCapability(MekCapabilities.CHEMICAL);
            if (handler != null) {
                result.add(handler);
            }
        }
        return result;
    }

    @Override
    public int getChemicalTanks() {
        int tanks = 1;
        for (var handler : handlers()) {
            tanks += handler.getChemicalTanks();
        }
        return tanks;
    }

    @Override
    public ChemicalStack getChemicalInTank(int tank) {
        for (var handler : handlers()) {
            if (tank < handler.getChemicalTanks()) {
                return handler.getChemicalInTank(tank);
            }
            tank -= handler.getChemicalTanks();
        }
        return ChemicalStack.EMPTY;
    }

    @Override
    public void setChemicalInTank(int tank, ChemicalStack stack) {
    }

    @Override
    public long getChemicalTankCapacity(int tank) {
        for (var handler : handlers()) {
            if (tank < handler.getChemicalTanks()) {
                return handler.getChemicalTankCapacity(tank);
            }
            tank -= handler.getChemicalTanks();
        }
        return Long.MAX_VALUE;
    }

    @Override
    public boolean isValid(int tank, ChemicalStack stack) {
        return true;
    }

    @Override
    public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action) {
        return tank == getChemicalTanks() - 1 ? insertChemical(stack, action) : stack;
    }

    @Override
    public ChemicalStack insertChemical(ChemicalStack stack, Action action) {
        ChemicalStack remainder = stack;
        for (var handler : handlers()) {
            if (remainder.isEmpty()) {
                break;
            }
            remainder = handler.insertChemical(remainder, action);
        }
        return remainder;
    }

    @Override
    public ChemicalStack extractChemical(int tank, long amount, Action action) {
        return ChemicalStack.EMPTY;
    }

    @Override
    public ChemicalStack extractChemical(long amount, Action action) {
        return ChemicalStack.EMPTY;
    }

    @Override
    public ChemicalStack extractChemical(ChemicalStack stack, Action action) {
        return ChemicalStack.EMPTY;
    }
}

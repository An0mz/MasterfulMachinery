package io.ticticboom.mods.mm.port.mekanism.gas;

import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorage;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorageModel;

/**
 * Kept as a distinct class so mm:mekanism/gas stays a valid port type for existing packs and
 * keeps its own block, block entity and menu registrations. Mekanism 1.21.1 no longer separates
 * gas from any other chemical, so all behaviour comes from the shared base: the tank, the
 * capability and the serialisation are identical across all four kinds.
 */
public class MekanismGasPortStorage extends MekanismChemicalPortStorage {

    protected MekanismGasPortStorage(MekanismChemicalPortStorageModel model, INotifyChangeFunction changed) {
        super(model, changed);
    }
}

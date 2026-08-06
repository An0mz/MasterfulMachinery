package io.ticticboom.mods.mm.port.mekanism.slurry;

import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorage;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorageModel;

/**
 * Kept as a distinct class so mm:mekanism/slurry stays a valid port type for existing packs and
 * keeps its own block, block entity and menu registrations. Mekanism 1.21.1 no longer separates
 * slurry from any other chemical, so all behaviour comes from the shared base: the tank, the
 * capability and the serialisation are identical across all four kinds.
 */
public class MekanismSlurryPortStorage extends MekanismChemicalPortStorage {

    protected MekanismSlurryPortStorage(MekanismChemicalPortStorageModel model, INotifyChangeFunction changed) {
        super(model, changed);
    }
}

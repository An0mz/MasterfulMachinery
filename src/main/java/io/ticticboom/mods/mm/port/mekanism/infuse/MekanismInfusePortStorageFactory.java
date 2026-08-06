package io.ticticboom.mods.mm.port.mekanism.infuse;

import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorage;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorageFactory;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorageModel;

public class MekanismInfusePortStorageFactory extends MekanismChemicalPortStorageFactory {

    public MekanismInfusePortStorageFactory(MekanismChemicalPortStorageModel model) {
        super(model);
    }

    @Override
    public IPortStorage createPortStorage(INotifyChangeFunction changed) {
        return new MekanismInfusePortStorage(model, changed);
    }
}
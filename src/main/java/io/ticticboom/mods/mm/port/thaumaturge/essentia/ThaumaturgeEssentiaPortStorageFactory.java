package io.ticticboom.mods.mm.port.thaumaturge.essentia;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;

public class ThaumaturgeEssentiaPortStorageFactory implements IPortStorageFactory {

    private final ThaumaturgeEssentiaPortStorageModel model;

    public ThaumaturgeEssentiaPortStorageFactory(ThaumaturgeEssentiaPortStorageModel model) {
        this.model = model;
    }

    @Override
    public IPortStorage createPortStorage(INotifyChangeFunction changed) {
        return new ThaumaturgeEssentiaPortStorage(model, changed);
    }

    @Override
    public JsonObject serialize() {
        return model.serialize();
    }

    @Override
    public IPortStorageModel getModel() {
        return model;
    }
}

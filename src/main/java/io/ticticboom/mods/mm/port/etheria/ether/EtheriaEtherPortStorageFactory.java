package io.ticticboom.mods.mm.port.etheria.ether;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;

public class EtheriaEtherPortStorageFactory implements IPortStorageFactory {

    private final EtheriaEtherPortStorageModel model;

    public EtheriaEtherPortStorageFactory(EtheriaEtherPortStorageModel model) {
        this.model = model;
    }

    @Override
    public IPortStorage createPortStorage(INotifyChangeFunction changed) {
        return new EtheriaEtherPortStorage(model, changed);
    }

    @Override
    public JsonObject serialize() {
        var json = new JsonObject();
        json.addProperty("capacity", model.capacity());
        json.addProperty("vesselRate", model.vesselRate());
        return json;
    }

    @Override
    public IPortStorageModel getModel() {
        return model;
    }
}

package io.ticticboom.mods.mm.port.astral.starlight;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;

public class AstralStarlightPortStorageFactory implements IPortStorageFactory {

    private final AstralStarlightPortStorageModel model;

    public AstralStarlightPortStorageFactory(AstralStarlightPortStorageModel model) {
        this.model = model;
    }

    @Override
    public IPortStorage createPortStorage(INotifyChangeFunction changed) {
        return new AstralStarlightPortStorage(model, changed);
    }

    @Override
    public JsonObject serialize() {
        var json = new JsonObject();
        json.addProperty("capacity", model.capacity());
        return json;
    }

    @Override
    public IPortStorageModel getModel() {
        return model;
    }
}

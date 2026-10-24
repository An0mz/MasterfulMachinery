package io.ticticboom.mods.mm.port.astral.lumen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;

public class AstralLumenPortStorageFactory implements IPortStorageFactory {

    private final AstralLumenPortStorageModel model;

    public AstralLumenPortStorageFactory(AstralLumenPortStorageModel model) {
        this.model = model;
    }

    @Override
    public IPortStorage createPortStorage(INotifyChangeFunction changed) {
        return new AstralLumenPortStorage(model, changed);
    }

    @Override
    public JsonObject serialize() {
        var json = new JsonObject();
        json.addProperty("capacity", model.capacity());
        if (!model.filter().isEmpty()) {
            var filter = new JsonArray();
            model.filter().forEach(id -> filter.add(id.toString()));
            json.add("lumen", filter);
        }
        return json;
    }

    @Override
    public IPortStorageModel getModel() {
        return model;
    }
}

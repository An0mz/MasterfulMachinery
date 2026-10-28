package io.ticticboom.mods.mm.port.etheria.ether;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.util.LongAmountRange;

public class EtheriaEtherPortParser implements IPortParser {

    @Override
    public IPortStorageFactory parseStorage(JsonObject json) {
        var capacity = json.get("capacity").getAsLong();
        var vesselRate = json.has("vesselRate") ? json.get("vesselRate").getAsLong() : 0;
        return new EtheriaEtherPortStorageFactory(new EtheriaEtherPortStorageModel(capacity, vesselRate));
    }

    @Override
    public IPortIngredient parseRecipeIngredient(JsonObject json) {
        return new EtheriaEtherPortIngredient(LongAmountRange.parse(json, "ether"));
    }
}

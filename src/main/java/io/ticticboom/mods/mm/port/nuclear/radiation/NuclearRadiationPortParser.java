package io.ticticboom.mods.mm.port.nuclear.radiation;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.util.AmountRange;

public class NuclearRadiationPortParser implements IPortParser {

    @Override
    public IPortStorageFactory parseStorage(JsonObject json) {
        return new NuclearRadiationPortStorageFactory(NuclearRadiationPortStorageModel.parse(json));
    }

    @Override
    public IPortIngredient parseRecipeIngredient(JsonObject json) {
        String isotope = json.has("isotope") ? json.get("isotope").getAsString() : null;
        var element = json.get("amount");
        if (element == null || element.isJsonNull()) {
            throw new RuntimeException("Missing 'amount' on a radiation ingredient: " + json);
        }
        if (element.isJsonObject()) {
            return new NuclearRadiationPortIngredient(isotope, 0, AmountRange.parse(json, "amount"));
        }
        return new NuclearRadiationPortIngredient(isotope, element.getAsDouble(), null);
    }
}

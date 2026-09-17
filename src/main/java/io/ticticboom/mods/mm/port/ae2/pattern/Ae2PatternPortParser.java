package io.ticticboom.mods.mm.port.ae2.pattern;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;

public class Ae2PatternPortParser implements IPortParser {
    @Override
    public IPortStorageFactory parseStorage(JsonObject json) {
        var priority = json.has("patternPriority") ? json.get("patternPriority").getAsInt() : 0;
        return new Ae2PatternPortStorageFactory(new Ae2PatternPortStorageModel(priority));
    }

    @Override
    public IPortIngredient parseRecipeIngredient(JsonObject json) {
        throw new RuntimeException("ME pattern ports can't be used as recipe ingredients");
    }
}

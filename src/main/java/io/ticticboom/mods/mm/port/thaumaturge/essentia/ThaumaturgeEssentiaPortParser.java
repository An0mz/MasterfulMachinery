package io.ticticboom.mods.mm.port.thaumaturge.essentia;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.util.AmountRange;
import io.ticticboom.mods.mm.util.ParserUtils;

public class ThaumaturgeEssentiaPortParser implements IPortParser {

    @Override
    public IPortStorageFactory parseStorage(JsonObject json) {
        return new ThaumaturgeEssentiaPortStorageFactory(ThaumaturgeEssentiaPortStorageModel.parse(json));
    }

    @Override
    public IPortIngredient parseRecipeIngredient(JsonObject json) {
        var aspect = ParserUtils.parseOptionalId(json, "aspect");
        if (aspect == null) {
            throw new RuntimeException("An MM essentia ingredient needs an 'aspect' field: " + json);
        }
        return new ThaumaturgeEssentiaPortIngredient(aspect, AmountRange.parse(json, "amount"));
    }
}

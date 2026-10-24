package io.ticticboom.mods.mm.port.astral.starlight;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.compat.astralsorcery.AstralSorceryNames;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.util.AmountRange;
import io.ticticboom.mods.mm.util.ParserUtils;

public class AstralStarlightPortParser implements IPortParser {

    @Override
    public IPortStorageFactory parseStorage(JsonObject json) {
        return new AstralStarlightPortStorageFactory(new AstralStarlightPortStorageModel(json.get("capacity").getAsInt()));
    }

    @Override
    public IPortIngredient parseRecipeIngredient(JsonObject json) {
        var constellation = json.has("constellation") ? ParserUtils.parseId(json, "constellation") : null;
        if (constellation != null) {
            AstralSorceryNames.requireConstellation(constellation);
        }
        return new AstralStarlightPortIngredient(constellation, AmountRange.parse(json, "starlight"));
    }
}

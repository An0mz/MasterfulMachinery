package io.ticticboom.mods.mm.port.astral.lumen;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.compat.astralsorcery.AstralSorceryNames;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.util.AmountRange;
import io.ticticboom.mods.mm.util.ParserUtils;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;

public class AstralLumenPortParser implements IPortParser {

    @Override
    public IPortStorageFactory parseStorage(JsonObject json) {
        var capacity = json.get("capacity").getAsInt();
        var filter = new ArrayList<ResourceLocation>();
        if (json.has("lumen")) {
            var lumen = json.get("lumen");
            if (lumen.isJsonArray()) {
                lumen.getAsJsonArray().forEach(el -> filter.add(ResourceLocation.parse(el.getAsString())));
            } else {
                filter.add(ResourceLocation.parse(lumen.getAsString()));
            }
        }
        return new AstralLumenPortStorageFactory(new AstralLumenPortStorageModel(capacity, filter));
    }

    @Override
    public IPortIngredient parseRecipeIngredient(JsonObject json) {
        var lumen = ParserUtils.parseId(json, "lumen");
        AstralSorceryNames.requireLumen(lumen);
        return new AstralLumenPortIngredient(lumen, AmountRange.parse(json, "amount"));
    }
}

package io.ticticboom.mods.mm.port.projecte.emc;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.util.LongAmountRange;

public class ProjectEEmcPortParser implements IPortParser {

    @Override
    public IPortStorageFactory parseStorage(JsonObject json) {
        return new ProjectEEmcPortStorageFactory(new ProjectEEmcPortStorageModel(json.get("capacity").getAsLong()));
    }

    @Override
    public IPortIngredient parseRecipeIngredient(JsonObject json) {
        return new ProjectEEmcPortIngredient(LongAmountRange.parse(json, "emc"));
    }
}

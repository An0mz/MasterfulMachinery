package io.ticticboom.mods.mm.port.mekanism.infuse;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortParser;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorageFactory;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorageModel;
import io.ticticboom.mods.mm.util.ParserUtils;

public class MekanismInfusePortParser extends MekanismChemicalPortParser {
    @Override
    public MekanismChemicalPortStorageFactory createFactory(long amount) {
        return new MekanismInfusePortStorageFactory(new MekanismChemicalPortStorageModel(amount));
    }

    @Override
    public IPortIngredient parseRecipeIngredient(JsonObject json) {
        var infuseType = ParserUtils.parseId(json, "infuse");
        var amount = json.get("amount").getAsLong();
        return new MekanismInfusePortIngredient(infuseType, amount);
    }
}

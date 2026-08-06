package io.ticticboom.mods.mm.port.mekanism.slurry;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortParser;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorageFactory;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorageModel;
import io.ticticboom.mods.mm.util.ParserUtils;

public class MekanismSlurryPortParser extends MekanismChemicalPortParser {

    @Override
    public MekanismChemicalPortStorageFactory createFactory(long amount) {
        return new MekanismSlurryPortStorageFactory(new MekanismChemicalPortStorageModel(amount));
    }

    @Override
    public IPortIngredient parseRecipeIngredient(JsonObject json) {
        var slurry = ParserUtils.parseId(json, "slurry");
        var amount = json.get("amount").getAsLong();
        return new MekanismSlurryPortIngredient(slurry, amount);
    }
}
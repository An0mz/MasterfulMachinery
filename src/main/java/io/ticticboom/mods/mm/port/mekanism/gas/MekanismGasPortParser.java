package io.ticticboom.mods.mm.port.mekanism.gas;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortParser;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorageFactory;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorageModel;
import io.ticticboom.mods.mm.util.ParserUtils;

public class MekanismGasPortParser extends MekanismChemicalPortParser {
    @Override
    public MekanismChemicalPortStorageFactory createFactory(long amount) {
        return new MekanismGasPortStorageFactory(new MekanismChemicalPortStorageModel(amount));
    }

    @Override
    public IPortIngredient parseRecipeIngredient(JsonObject json) {
        var gas = ParserUtils.parseId(json, "gas");
        var amount = json.get("amount").getAsLong();
        return new MekanismGasPortIngredient(gas, amount);
    }
}

package io.ticticboom.mods.mm.port.mekanism.chemical;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.util.ParserUtils;
import net.minecraft.resources.ResourceLocation;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;

public abstract class MekanismChemicalPortParser implements IPortParser {

    public abstract MekanismChemicalPortStorageFactory createFactory(long amount);

    protected static ResourceLocation parseChemicalId(JsonObject json, String key) {
        var specific = ParserUtils.parseOptionalId(json, key);
        if (specific != null) {
            return specific;
        }
        var chemical = ParserUtils.parseOptionalId(json, "chemical");
        if (chemical != null) {
            return chemical;
        }
        throw new RuntimeException(String.format(
                "An MM Mekanism ingredient needs either '%s' or 'chemical': %s", key, json));
    }

    @Override
    public IPortStorageFactory parseStorage(JsonObject json) {
        var amount = json.get("capacity").getAsLong();
        return createFactory(amount);
    }
}

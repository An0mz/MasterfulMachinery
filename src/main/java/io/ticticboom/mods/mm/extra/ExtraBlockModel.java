package io.ticticboom.mods.mm.extra;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.util.ParserUtils;
import net.minecraft.resources.ResourceLocation;

public record ExtraBlockModel(
        String id,
        String name,
        ResourceLocation type
) {
    public static ExtraBlockModel parse(JsonObject json) {
        String id = json.get("id").getAsString();
        // Extra blocks have no GUI, so their name is only used to seed the generated block.mm.<id>
        // lang entry. The key form is accepted here purely to keep the datapack schema uniform.
        String name = ParserUtils.parseComponentKey(json.get("name"));
        ResourceLocation type = ParserUtils.parseId(json, "type");
        return new ExtraBlockModel(id, name, type);
    }
}

package io.ticticboom.mods.mm.extra;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.util.ParserUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public record ExtraBlockModel(
        String id,
        String name,
        Component displayName,
        ResourceLocation type
) {
    public static ExtraBlockModel parse(JsonObject json) {
        String id = json.get("id").getAsString();
        // The raw string seeds the generated block.mm.<id> lang entry; the component form is what
        // the block actually displays when the pack supplied a translation key.
        String name = ParserUtils.parseComponentKey(json.get("name"));
        Component displayName = ParserUtils.parseComponent(json.get("name"));
        ResourceLocation type = ParserUtils.parseId(json, "type");
        return new ExtraBlockModel(id, name, displayName, type);
    }
}

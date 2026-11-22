package io.ticticboom.mods.mm.port.entity;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.util.AmountRange;
import io.ticticboom.mods.mm.util.ParserUtils;

public class EntityPortParser implements IPortParser {
    @Override
    public IPortStorageFactory parseStorage(JsonObject json) {
        return new EntityPortStorageFactory(EntityPortStorageModel.parse(json));
    }

    @Override
    public IPortIngredient parseRecipeIngredient(JsonObject json) {
        var entity = ParserUtils.parseOptionalId(json, "entity");
        var tag = ParserUtils.parseOptionalId(json, "tag");
        EntityGroup group = null;
        if (json.has("group") && !json.get("group").isJsonNull()) {
            if (entity != null || tag != null) {
                throw new RuntimeException("An entity ingredient takes one of 'entity', 'tag' or 'group', not several");
            }
            var value = json.get("group");
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                throw new RuntimeException("Entity 'group' must be 'passive', 'hostile' or 'boss'");
            }
            group = EntityGroup.parse(value.getAsString());
        }
        var amount = json.has("amount") ? AmountRange.parse(json, "amount") : AmountRange.of(1);
        return new EntityPortIngredient(entity, tag, group, amount);
    }
}

package io.ticticboom.mods.mm.piece.modifier.framed;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.piece.modifier.MMStructurePieceModifierType;
import io.ticticboom.mods.mm.piece.modifier.StructurePieceModifier;
import net.minecraft.resources.ResourceLocation;

public class FramedCamoStructurePieceModifierType extends MMStructurePieceModifierType {

    @Override
    public boolean identify(JsonObject json) {
        return json.has("camo");
    }

    @Override
    public StructurePieceModifier parse(JsonObject json) {
        var camo = ResourceLocation.parse(json.get("camo").getAsString());
        var camoTwo = json.has("camo_two") ? ResourceLocation.parse(json.get("camo_two").getAsString()) : null;
        return new FramedCamoStructurePieceModifier(camo, camoTwo);
    }
}

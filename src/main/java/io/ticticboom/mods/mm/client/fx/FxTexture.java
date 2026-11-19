package io.ticticboom.mods.mm.client.fx;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public final class FxTexture {
    private FxTexture() {
    }

    public static ResourceLocation required(JsonObject json, String layer) {
        if (!json.has("texture")) {
            throw new IllegalArgumentException(layer + " layer needs 'texture', e.g. \"mypack:textures/fx/flow.png\"");
        }
        ResourceLocation id = ResourceLocation.tryParse(json.get("texture").getAsString());
        if (id == null) {
            throw new IllegalArgumentException(layer + " 'texture' is not a valid id: " + json.get("texture").getAsString());
        }
        return id;
    }
}

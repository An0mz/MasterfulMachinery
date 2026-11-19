package io.ticticboom.mods.mm.client.fx;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record FxDefinition(ResourceLocation id, double rise, double fall, double hold, double viewDistance, List<FxLayer> layers) {

    public static FxDefinition parse(ResourceLocation id, JsonObject json) {
        double rise = json.has("rise") ? json.get("rise").getAsDouble() : 2.4;
        double fall = json.has("fall") ? json.get("fall").getAsDouble() : 3.6;
        double hold = json.has("hold") ? json.get("hold").getAsDouble() : 1.25;
        double viewDistance = json.has("view_distance") ? json.get("view_distance").getAsDouble() : 96;
        var layers = new ArrayList<FxLayer>();
        if (json.has("layers")) {
            for (var element : json.getAsJsonArray("layers")) {
                JsonObject layer = element.getAsJsonObject();
                String type = layer.has("type") ? layer.get("type").getAsString() : "";
                switch (type) {
                    case "stream" -> layers.add(StreamLayer.parse(layer));
                    case "drip" -> layers.add(DripLayer.parse(layer));
                    case "drops" -> layers.add(DropsLayer.parse(layer));
                    case "pool" -> layers.add(PoolLayer.parse(layer));
                    case "ripple" -> layers.add(RippleLayer.parse(layer));
                    case "seep" -> layers.add(SeepLayer.parse(layer));
                    default -> throw new IllegalArgumentException("Unknown effect layer type: " + type);
                }
            }
        }
        return new FxDefinition(id, rise, fall, hold, viewDistance, List.copyOf(layers));
    }
}

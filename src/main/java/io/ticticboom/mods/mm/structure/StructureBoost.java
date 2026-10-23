package io.ticticboom.mods.mm.structure;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record StructureBoost(ResourceLocation structureId, int radius, double speed, int max) {

    public static final int DEFAULT_RADIUS = 16;

    public static List<StructureBoost> parseAll(JsonObject config) {
        if (config == null || !config.has("boosts") || !config.get("boosts").isJsonArray()) {
            return List.of();
        }
        var result = new ArrayList<StructureBoost>();
        for (JsonElement element : config.getAsJsonArray("boosts")) {
            result.add(parse(element.getAsJsonObject()));
        }
        return List.copyOf(result);
    }

    public static StructureBoost parse(JsonObject json) {
        var id = ResourceLocation.tryParse(json.get("structure").getAsString());
        if (id == null) {
            throw new IllegalArgumentException("Invalid structure id in boost: " + json.get("structure"));
        }
        int radius = json.has("radius") ? json.get("radius").getAsInt() : DEFAULT_RADIUS;
        double speed = json.has("speed") ? json.get("speed").getAsDouble() : 0;
        int max = json.has("max") ? json.get("max").getAsInt() : Integer.MAX_VALUE;
        return new StructureBoost(id, radius, speed, max);
    }

    public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("structure", structureId.toString());
        json.addProperty("radius", radius);
        json.addProperty("speed", speed);
        if (max != Integer.MAX_VALUE) {
            json.addProperty("max", max);
        }
        return json;
    }

    public double bonus(NearbyMachines.Lookup nearby) {
        if (speed <= 0 || max <= 0) {
            return 0;
        }
        return Math.min(nearby.count(structureId, radius), max) * speed;
    }
}

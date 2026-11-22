package io.ticticboom.mods.mm.client.fx;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public record FxValue(double idle, double working) {
    public static FxValue of(double value) {
        return new FxValue(value, value);
    }

    public static FxValue parse(JsonObject json, String key, double fallback) {
        if (!json.has(key)) {
            return of(fallback);
        }
        JsonElement el = json.get(key);
        if (el.isJsonObject()) {
            JsonObject o = el.getAsJsonObject();
            double idle = o.has("idle") ? o.get("idle").getAsDouble() : fallback;
            double working = o.has("working") ? o.get("working").getAsDouble() : fallback;
            return new FxValue(idle, working);
        }
        return of(el.getAsDouble());
    }

    public double at(double intensity) {
        return idle + (working - idle) * intensity;
    }
}

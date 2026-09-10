package io.ticticboom.mods.mm.port.nuclear.radiation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortStorageModel;

import java.util.ArrayList;
import java.util.List;

public record NuclearRadiationPortStorageModel(
        double capacity,
        List<String> isotopes,
        boolean decay,
        boolean shielded
) implements IPortStorageModel {

    public boolean accepts(String isotope) {
        return isotopes.isEmpty() || isotopes.contains(isotope);
    }

    public static NuclearRadiationPortStorageModel parse(JsonObject json) {
        double capacity = json.get("capacity").getAsDouble();
        var isotopes = new ArrayList<String>();
        if (json.has("isotopes")) {
            JsonElement element = json.get("isotopes");
            if (element.isJsonArray()) {
                for (JsonElement entry : element.getAsJsonArray()) {
                    isotopes.add(entry.getAsString());
                }
            } else {
                isotopes.add(element.getAsString());
            }
        }
        boolean decay = !json.has("decay") || json.get("decay").getAsBoolean();
        boolean shielded = !json.has("shielded") || json.get("shielded").getAsBoolean();
        return new NuclearRadiationPortStorageModel(capacity, List.copyOf(isotopes), decay, shielded);
    }

    public JsonObject serialize() {
        var json = new JsonObject();
        json.addProperty("capacity", capacity);
        var list = new JsonArray();
        isotopes.forEach(list::add);
        json.add("isotopes", list);
        json.addProperty("decay", decay);
        json.addProperty("shielded", shielded);
        return json;
    }
}

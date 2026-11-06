package io.ticticboom.mods.mm.port.thaumaturge.essentia;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortStorageModel;

public record ThaumaturgeEssentiaPortStorageModel(
        int capacity,
        int suction,
        int transferRate
) implements IPortStorageModel {

    public static final int DEFAULT_SUCTION = 128;
    public static final int DEFAULT_TRANSFER_RATE = 1;

    public static ThaumaturgeEssentiaPortStorageModel parse(JsonObject json) {
        var capacity = json.get("capacity").getAsInt();
        var suction = json.has("suction") ? json.get("suction").getAsInt() : DEFAULT_SUCTION;
        var transferRate = json.has("transferRate") ? json.get("transferRate").getAsInt() : DEFAULT_TRANSFER_RATE;
        return new ThaumaturgeEssentiaPortStorageModel(capacity, suction, transferRate);
    }

    public JsonObject serialize() {
        var json = new JsonObject();
        json.addProperty("capacity", capacity);
        json.addProperty("suction", suction);
        json.addProperty("transferRate", transferRate);
        return json;
    }
}

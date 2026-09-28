package io.ticticboom.mods.mm.compat.kjs.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecipeBuilderJS {
    private final List<JsonObject> inputs = new ArrayList<>();
    private final List<JsonObject> outputs = new ArrayList<>();
    private int ticks;
    private ResourceLocation structureId;
    private boolean parallelProcessing = false;
    private boolean requestOnly = false;
    private final List<ResourceLocation> extraStructureIds = new ArrayList<>();
    private final JsonArray conditions = new JsonArray();
    @Getter
    private final ResourceLocation id;

    @SuppressWarnings("removal")
    public RecipeBuilderJS(String id) {
        //noinspection removal
        this.id = ResourceLocation.parse(id);
    }

    public RecipeBuilderJS input(JsonObject entry) {
        inputs.add(entry);
        return this;
    }

    public RecipeBuilderJS output(JsonObject entry) {
        outputs.add(entry);
        return this;
    }

    public RecipeBuilderJS ticks(int ticks) {
        this.ticks = ticks;
        return this;
    }

    public RecipeBuilderJS structureId(ResourceLocation id) {
        this.structureId = id;
        return this;
    }

    @SuppressWarnings("unused")
    public RecipeBuilderJS parallelProcessing(boolean parallelProcessing) {
        this.parallelProcessing = parallelProcessing;
        return this;
    }

    @SuppressWarnings("unused")
    public RecipeBuilderJS requestOnly(boolean requestOnly) {
        this.requestOnly = requestOnly;
        return this;
    }

    @SuppressWarnings("unused")
    public RecipeBuilderJS structureIds(String... ids) {
        for (String id : ids) {
            var rl = ResourceLocation.tryParse(id);
            if (rl == null) throw new IllegalArgumentException("Invalid structure id: " + id);
            if (structureId == null) structureId = rl;
            else if (!rl.equals(structureId) && !extraStructureIds.contains(rl)) extraStructureIds.add(rl);
        }
        return this;
    }

    @SuppressWarnings("unused")
    public RecipeBuilderJS dimension(String dimension) {
        return condition("dimension", "dimension", dimension);
    }

    @SuppressWarnings("unused")
    public RecipeBuilderJS biome(String biome) {
        return condition("biome", "biome", biome);
    }

    @SuppressWarnings("unused")
    public RecipeBuilderJS time(String time) {
        return condition("time", "time", time);
    }

    @SuppressWarnings("unused")
    public RecipeBuilderJS weather(String weather) {
        return condition("weather", "weather", weather.toUpperCase(Locale.ROOT));
    }

    @SuppressWarnings("unused")
    public RecipeBuilderJS minY(int y) {
        return rangeCondition("height", "minY", y);
    }

    @SuppressWarnings("unused")
    public RecipeBuilderJS maxY(int y) {
        return rangeCondition("height", "maxY", y);
    }

    @SuppressWarnings("unused")
    public RecipeBuilderJS redstone(String redstone) {
        return condition("redstone", "redstone", redstone);
    }

    @SuppressWarnings("unused")
    public RecipeBuilderJS minTier(double tier) {
        return rangeCondition("tier", "minTier", tier);
    }

    @SuppressWarnings("unused")
    public RecipeBuilderJS maxTier(double tier) {
        return rangeCondition("tier", "maxTier", tier);
    }

    private RecipeBuilderJS condition(String type, String key, String value) {
        var json = new JsonObject();
        json.addProperty("type", "mm:" + type);
        json.addProperty(key, value);
        conditions.add(json);
        return this;
    }

    private RecipeBuilderJS rangeCondition(String type, String key, Number value) {
        for (var element : conditions) {
            var json = element.getAsJsonObject();
            if (("mm:" + type).equals(json.get("type").getAsString())) {
                json.addProperty(key, value);
                return this;
            }
        }
        var json = new JsonObject();
        json.addProperty("type", "mm:" + type);
        json.addProperty(key, value);
        conditions.add(json);
        return this;
    }

    public RecipeModel build() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id.toString());
        json.addProperty("ticks", ticks);
        json.addProperty("structureId", structureId.toString());
        json.addProperty("parallelProcessing", parallelProcessing);
        json.addProperty("requestOnly", requestOnly);
        var inputArr = new JsonArray();
        for (JsonObject input : inputs) {
            inputArr.add(input);
        }
        var outputArr = new JsonArray();
        for (JsonObject output : outputs) {
            outputArr.add(output);
        }
        json.add("inputs", inputArr);
        json.add("outputs", outputArr);
        if (!conditions.isEmpty()) {
            json.add("conditions", conditions);
        }
        if (!extraStructureIds.isEmpty()) {
            var ids = new JsonArray();
            extraStructureIds.forEach(structure -> ids.add(structure.toString()));
            json.add("structureIds", ids);
        }
        return RecipeModel.parse(json, id);
    }
}

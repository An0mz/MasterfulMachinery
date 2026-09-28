package io.ticticboom.mods.mm.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.IPortStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RecipeStorages {

    private final List<IPortStorage> inputStorages;
    private final List<IPortStorage> outputStorages;
    private final List<IPortBlockEntity> sources;
    private final Map<Class<?>, List<?>> inputsByType = new HashMap<>();
    private final Map<Class<?>, List<?>> outputsByType = new HashMap<>();

    public RecipeStorages(List<IPortStorage> inputStorages, List<IPortStorage> outputStorages) {
        this(inputStorages, outputStorages, List.of());
    }

    public RecipeStorages(List<IPortStorage> inputStorages, List<IPortStorage> outputStorages, List<IPortBlockEntity> sources) {
        this.inputStorages = List.copyOf(inputStorages);
        this.outputStorages = List.copyOf(outputStorages);
        this.sources = List.copyOf(sources);
    }

    public List<IPortStorage> inputStorages() {
        return inputStorages;
    }

    public List<IPortStorage> outputStorages() {
        return outputStorages;
    }

    public List<IPortBlockEntity> sources() {
        return sources;
    }

    public <T extends IPortStorage> List<T> getInputStorages(Class<T> clz) {
        return getStorages(clz, inputStorages, inputsByType);
    }

    public <T extends IPortStorage> List<T> getOutputStorages(Class<T> clz) {
        return getStorages(clz, outputStorages, outputsByType);
    }

    public long changeCount() {
        long total = 0;
        for (IPortBlockEntity source : sources) {
            total += source.changeCount();
        }
        return total;
    }

    @SuppressWarnings("unchecked")
    private static <T extends IPortStorage> List<T> getStorages(Class<T> clz, List<IPortStorage> storages, Map<Class<?>, List<?>> cache) {
        var cached = cache.get(clz);
        if (cached != null) {
            return (List<T>) cached;
        }
        var result = new ArrayList<T>();
        for (IPortStorage storage : storages) {
            if (clz.isInstance(storage)) {
                result.add((T) storage);
            }
        }
        var frozen = List.copyOf(result);
        cache.put(clz, frozen);
        return frozen;
    }

    public JsonObject debug() {
        var inputsJson = new JsonArray();
        var outputsJson = new JsonArray();
        for (IPortStorage inputStorage : inputStorages) {
            inputsJson.add(inputStorage.debugDump());
        }
        for (IPortStorage outputStorage : outputStorages) {
            outputsJson.add(outputStorage.debugDump());
        }
        var json = new JsonObject();
        json.add("inputs", inputsJson);
        json.add("outputs", outputsJson);
        return json;
    }
}

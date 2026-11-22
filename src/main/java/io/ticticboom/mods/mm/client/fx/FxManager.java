package io.ticticboom.mods.mm.client.fx;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ticticboom.mods.mm.Ref;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public final class FxManager {
    private static final String FOLDER = "mm_fx";
    private static Map<ResourceLocation, FxDefinition> definitions = Map.of();
    private static Map<ResourceLocation, JsonObject> sources = Map.of();
    private static int generation;

    private FxManager() {
    }

    @Nullable
    public static FxDefinition get(ResourceLocation id) {
        return definitions.get(id);
    }

    @Nullable
    public static JsonObject source(ResourceLocation id) {
        return sources.get(id);
    }

    public static void preview(ResourceLocation id, JsonObject json) {
        FxDefinition definition = FxDefinition.parse(id, json);
        var next = new HashMap<>(definitions);
        next.put(id, definition);
        definitions = next;
        var nextSources = new HashMap<>(sources);
        nextSources.put(id, json);
        sources = nextSources;
        generation++;
    }

    public static int generation() {
        return generation;
    }

    public static void reload(ResourceManager resources) {
        var loaded = new HashMap<ResourceLocation, FxDefinition>();
        var loadedSources = new HashMap<ResourceLocation, JsonObject>();
        for (var entry : resources.listResources(FOLDER, path -> path.getPath().endsWith(".json")).entrySet()) {
            ResourceLocation file = entry.getKey();
            String path = file.getPath();
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(file.getNamespace(), path.substring(FOLDER.length() + 1, path.length() - 5));
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                loaded.put(id, FxDefinition.parse(id, json));
                loadedSources.put(id, json);
            } catch (Exception e) {
                Ref.LOG.warn("Ignoring effect {}: {}", id, e.getMessage());
            }
        }
        definitions = Map.copyOf(loaded);
        sources = Map.copyOf(loadedSources);
        FxPortColor.clear();
        FxLog.reset();
        generation++;
        Ref.LOG.info("Loaded {} machine effects", loaded.size());
    }
}

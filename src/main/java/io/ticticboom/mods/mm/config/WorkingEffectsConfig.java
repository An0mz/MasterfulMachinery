package io.ticticboom.mods.mm.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ticticboom.mods.mm.Ref;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class WorkingEffectsConfig {

    public record Entry(@Nullable String sound, @Nullable Integer interval, @Nullable String particle) {
    }

    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("mm").resolve("working_effects.json");
    private static final String HELP = "Working sound and particle per machine controller (block id). Each field is optional and "
            + "overrides the controller's KubeJS/JSON settings; an empty string turns it off. interval = ticks between sounds. Reload with F3+T. "
            + "Example: \"mm:auto_crusher\": {\"sound\": \"minecraft:block.grindstone.use\", \"interval\": 25, \"particle\": \"minecraft:smoke\"}";

    private static Map<ResourceLocation, Entry> entries = Map.of();
    private static boolean loaded;
    private static int generation;

    private WorkingEffectsConfig() {
    }

    @Nullable
    public static Entry get(ResourceLocation controllerId) {
        if (!loaded) reload();
        return entries.get(controllerId);
    }

    public static int generation() {
        if (!loaded) reload();
        return generation;
    }

    public static synchronized void reload() {
        loaded = true;
        generation++;
        try {
            if (!Files.exists(FILE)) {
                writeDefault();
                entries = Map.of();
                return;
            }
            JsonObject json = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<ResourceLocation, Entry> read = new HashMap<>();
            for (Map.Entry<String, JsonElement> e : json.entrySet()) {
                if (e.getKey().startsWith("_")) continue;
                ResourceLocation id = ResourceLocation.tryParse(e.getKey());
                if (id == null || !e.getValue().isJsonObject()) {
                    Ref.LOG.warn("Ignoring {} in {}: expected a controller id with an object", e.getKey(), FILE);
                    continue;
                }
                JsonObject o = e.getValue().getAsJsonObject();
                read.put(id, new Entry(
                        o.has("sound") ? o.get("sound").getAsString() : null,
                        o.has("interval") ? Math.max(1, o.get("interval").getAsInt()) : null,
                        o.has("particle") ? o.get("particle").getAsString() : null));
            }
            entries = read;
        } catch (IOException | RuntimeException e) {
            Ref.LOG.error("Failed to read {}, machine working effects fall back to the controllers' own settings", FILE, e);
            entries = Map.of();
        }
    }

    private static void writeDefault() throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("_help", HELP);
        Files.createDirectories(FILE.getParent());
        Files.writeString(FILE, new GsonBuilder().setPrettyPrinting().create().toJson(json), StandardCharsets.UTF_8);
    }
}

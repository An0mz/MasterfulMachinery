package io.ticticboom.mods.mm.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;
import java.util.function.Supplier;


public class ParserUtils {

    public static ResourceLocation parseId(JsonElement json) {
        var s = json.getAsString();
        var rl = ResourceLocation.tryParse(s);
        if (rl == null) throw new RuntimeException("Invalid resource location: " + s);
        return rl;
    }

    public static ResourceLocation parseId(JsonObject json, String key) {
        return parseId(json.get(key));
    }

    public static ResourceLocation parseOptionalId(JsonObject json, String key) {
        if (json == null || !json.has(key) || json.get(key).isJsonNull()) {
            return null;
        }
        return parseId(json, key);
    }

    /**
     * Parses a display name that may be given either as a plain string or as a translation object:
     * <pre>
     *   "name": "Assembler"
     *   "name": { "translation": "mypack.machine.assembler" }
     * </pre>
     * Plain strings stay literal, so existing datapacks keep working unchanged.
     */
    public static Component parseComponent(JsonElement json) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            return Component.literal(json.getAsString());
        } else if (json.isJsonObject() && json.getAsJsonObject().has("translation")) {
            return Component.translatable(json.getAsJsonObject().get("translation").getAsString());
        }
        return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow(error -> new RuntimeException(
                        "Failed to parse text component: " + error + ". A name may be a plain string, "
                                + "a { \"translation\": \"key\" } object, or a full text component."));
    }

    /**
     * Raw string form of a name element, for serialization, data generation and NBT, where a
     * resolved {@link Component} is not usable. Returns the literal text for plain strings, or the
     * translation key for translation objects.
     */
    public static String parseComponentKey(JsonElement json) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            return json.getAsString();
        } else if (json.isJsonObject() && json.getAsJsonObject().has("translation")) {
            return json.getAsJsonObject().get("translation").getAsString();
        }
        return parseComponent(json).getString();
    }

    @SuppressWarnings("unused")
    public static Component parseComponent(JsonObject json, String key) {
        return parseComponent(json.get(key));
    }

    public static <T> T parseOrDefault(JsonObject json, String key, Supplier<T> defaultSupplier, Function<JsonObject, T> parser) {
        if (json.has(key)) {
            return parser.apply(json);
        }
        return defaultSupplier.get();
    }

    public static <T> Supplier<T> parseOrDefaultSupplier(JsonObject json, String key, Supplier<T> defaultSupplier, Function<JsonElement, T> getter) {
        Supplier<T> autoPushSupplier = defaultSupplier;
        if (json.has(key)) {
            T autoPush = getter.apply(json.get(key));
            autoPushSupplier = () -> autoPush;
        }
        return autoPushSupplier;
    }


    public static <T extends Enum<T>> T parseEnum(JsonObject json, String key, Class<T> cls) {
        var name = json.get(key).getAsString();
        return T.valueOf(cls, name.toUpperCase());
    }
}

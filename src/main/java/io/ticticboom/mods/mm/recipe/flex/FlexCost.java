package io.ticticboom.mods.mm.recipe.flex;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.config.MMConfig;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public record FlexCost(Mode mode, NavigableMap<Integer, Double> table) {

    public enum Mode {
        LINEAR,
        ONCE,
        TABLE
    }

    public static final FlexCost LINEAR = new FlexCost(Mode.LINEAR, new TreeMap<>());

    public double multiplier(int kinds) {
        int n = Math.max(1, kinds);
        return switch (mode) {
            case LINEAR -> n;
            case ONCE -> 1;
            case TABLE -> {
                var step = table.floorEntry(n);
                if (step != null) yield step.getValue();
                yield table.isEmpty() ? 1 : table.firstEntry().getValue();
            }
        };
    }

    public static FlexCost global() {
        return MMConfig.FLEX_COST;
    }

    public static Mode parseMode(String text) {
        try {
            return Mode.valueOf(text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unknown flex cost mode '" + text + "', expected 'linear', 'once' or 'table'");
        }
    }

    public static boolean isStep(String step) {
        try {
            parseStep(step, new TreeMap<>());
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static void parseStep(String step, NavigableMap<Integer, Double> into) {
        String[] parts = step.split("=");
        if (parts.length != 2) {
            throw new RuntimeException("Flex cost step '" + step + "' should look like \"kinds=multiplier\", e.g. \"4=2\"");
        }
        int kinds;
        double multiplier;
        try {
            kinds = Integer.parseInt(parts[0].trim());
            multiplier = Double.parseDouble(parts[1].trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Flex cost step '" + step + "' should look like \"kinds=multiplier\", e.g. \"4=2\"");
        }
        if (kinds < 1 || multiplier < 0) {
            throw new RuntimeException("Flex cost step '" + step + "': kinds must be at least 1 and the multiplier not negative");
        }
        into.put(kinds, multiplier);
    }

    public static FlexCost of(Mode mode, List<? extends String> steps) {
        var table = new TreeMap<Integer, Double>();
        for (String step : steps) {
            parseStep(step, table);
        }
        return new FlexCost(mode, table);
    }

    @Nullable
    public static FlexCost parse(JsonObject recipe) {
        JsonElement el = recipe.get("flex_cost");
        if (el == null || el.isJsonNull()) {
            return null;
        }
        if (el.isJsonPrimitive()) {
            return new FlexCost(parseMode(el.getAsString()), new TreeMap<>());
        }
        JsonObject json = el.getAsJsonObject();
        Mode mode = json.has("mode") ? parseMode(json.get("mode").getAsString()) : Mode.TABLE;
        var steps = new ArrayList<String>();
        if (json.has("table")) {
            JsonElement tableEl = json.get("table");
            if (tableEl.isJsonArray()) {
                tableEl.getAsJsonArray().forEach(e -> steps.add(e.getAsString()));
            } else {
                for (Map.Entry<String, JsonElement> e : tableEl.getAsJsonObject().entrySet()) {
                    steps.add(e.getKey() + "=" + e.getValue().getAsString());
                }
            }
        }
        if (mode == Mode.TABLE && steps.isEmpty()) {
            throw new RuntimeException("\"flex_cost\" with mode 'table' needs a 'table', e.g. {\"1\": 1, \"4\": 2}");
        }
        return of(mode, steps);
    }
}

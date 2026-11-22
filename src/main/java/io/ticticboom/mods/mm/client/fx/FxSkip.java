package io.ticticboom.mods.mm.client.fx;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

final class FxSkip {
    private static final int[] NONE = new int[0];

    private FxSkip() {
    }

    static int[] parse(JsonObject json) {
        if (!json.has("skip")) {
            return NONE;
        }
        JsonElement value = json.get("skip");
        if (value.isJsonNull()) {
            return NONE;
        }
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            return new int[]{value.getAsInt()};
        }
        if (!value.isJsonArray()) {
            throw new IllegalArgumentException("layer 'skip' must be a list of point numbers");
        }
        int[] result = new int[value.getAsJsonArray().size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = value.getAsJsonArray().get(i).getAsInt();
        }
        return result;
    }

    static boolean has(int[] skip, int index) {
        for (int s : skip) {
            if (s == index) {
                return true;
            }
        }
        return false;
    }

    static FxColumn[] keep(FxColumn[] columns, int[] skip) {
        if (columns == null || skip.length == 0) {
            return columns;
        }
        int count = 0;
        for (FxColumn c : columns) {
            if (!has(skip, c.index)) {
                count++;
            }
        }
        FxColumn[] result = new FxColumn[count];
        int i = 0;
        for (FxColumn c : columns) {
            if (!has(skip, c.index)) {
                result[i++] = c;
            }
        }
        return result;
    }

    static double[] keep(double[] points, int[] skip) {
        if (skip.length == 0) {
            return points;
        }
        int n = points.length / 3;
        int count = 0;
        for (int p = 0; p < n; p++) {
            if (!has(skip, p)) {
                count++;
            }
        }
        double[] result = new double[count * 3];
        int i = 0;
        for (int p = 0; p < n; p++) {
            if (!has(skip, p)) {
                result[i++] = points[p * 3];
                result[i++] = points[p * 3 + 1];
                result[i++] = points[p * 3 + 2];
            }
        }
        return result;
    }
}

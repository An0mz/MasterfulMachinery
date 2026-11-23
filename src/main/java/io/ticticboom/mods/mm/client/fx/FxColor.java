package io.ticticboom.mods.mm.client.fx;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

public final class FxColor {
    private FxColor() {
    }

    public static int parse(JsonObject json, String key, int fallback) {
        if (!json.has(key)) {
            return fallback;
        }
        String text = json.get(key).getAsString().trim();
        if (text.startsWith("#")) {
            text = text.substring(1);
        }
        if (text.length() != 6) {
            throw new IllegalArgumentException("'" + key + "' must be a colour like #7a0a0a");
        }
        return Integer.parseInt(text, 16);
    }

    public static boolean port(JsonObject json) {
        return json.has("port_color") && json.get("port_color").getAsBoolean();
    }

    public static boolean portTexture(JsonObject json) {
        return json.has("port_texture") && json.get("port_texture").getAsBoolean();
    }

    public static int pick(boolean port, int own, FxContext ctx) {
        return port && ctx.portColor >= 0 ? ctx.portColor : own;
    }

    public static int pick(boolean port, int own, FxContext ctx, @Nullable FxFluidSprite sprite) {
        return sprite != null ? ctx.portTint : pick(port, own, ctx);
    }

    public static double luminance(int rgb) {
        return (((rgb >> 16) & 0xFF) * 0.299 + ((rgb >> 8) & 0xFF) * 0.587 + (rgb & 0xFF) * 0.114) / 255.0;
    }

    public static int additive(int alpha, int rgb) {
        alpha = Math.max(0, Math.min(255, alpha));
        int r = ((rgb >> 16) & 0xFF) * alpha / 255;
        int g = ((rgb >> 8) & 0xFF) * alpha / 255;
        int b = (rgb & 0xFF) * alpha / 255;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    public static int argb(int alpha, int rgb, double shade) {
        alpha = Math.max(0, Math.min(255, alpha));
        int r = (int) Math.max(0, Math.min(255, ((rgb >> 16) & 0xFF) * shade));
        int g = (int) Math.max(0, Math.min(255, ((rgb >> 8) & 0xFF) * shade));
        int b = (int) Math.max(0, Math.min(255, (rgb & 0xFF) * shade));
        return alpha << 24 | r << 16 | g << 8 | b;
    }
}

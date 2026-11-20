package io.ticticboom.mods.mm.client.fx.studio;

import java.util.LinkedHashMap;
import java.util.Map;

final class FxSchema {
    static final String[] EFFECT = {"rise", "2.4", "fall", "3.6", "hold", "1.25", "view_distance", "96"};

    private static final String[] COLOR = {"texture", "", "color", "#ffffff", "port_color", "false", "glow", "false"};

    static final Map<String, String[]> LAYERS = new LinkedHashMap<>();

    static {
        LAYERS.put("stream", join(new String[]{"from", "drips", "to", "floor"}, COLOR, new String[]{
                "alpha", "0.925", "radius", "0.085", "gravity", "9.81", "start_speed", "0.55", "sway", "0.02",
                "bulge", "0.22", "flare", "0.55", "breakup", "true", "breakup_start", "2.2", "breakup_length", "3.6",
                "gaps", "0.1", "noise", "1", "trickle", "0.2", "texture_speed", "0.109", "texture_width", "0.25",
                "droplets", "true", "hide_when_blocked", "true", "needs_block_above", "true"}));
        LAYERS.put("drip", join(new String[]{"at", "drips", "to", "floor"}, COLOR, new String[]{
                "alpha", "0.96", "rate", "1", "size", "0.03", "drop_size", "0.036", "hide_when_blocked", "true"}));
        LAYERS.put("drops", join(COLOR, new String[]{
                "alpha", "0.96", "gravity", "9.81", "max", "48", "stretch", "0.06", "splash", "0.12"}));
        LAYERS.put("pool", join(new String[]{"at", "drips", "to", "floor"}, COLOR, new String[]{
                "alpha", "1", "size", "0.21", "base", "0.05", "fill", "0.55", "drain", "0.16", "swirl", "0",
                "hide_when_blocked", "true"}));
        LAYERS.put("ripple", join(COLOR, new String[]{
                "alpha", "1", "life", "0.55", "width", "0.014", "speed", "0.42", "max", "32"}));
        LAYERS.put("seep", new String[]{"at", "drips", "top", "seep_top", "texture", "", "palette", "", "port_color", "false",
                "glow", "false", "alpha", "1", "chance", "0.6", "speed", "1", "stains", "true", "drip_patch", "true"});
        LAYERS.put("emitter", join(new String[]{"at", "emit", "blend", "translucent"}, COLOR, new String[]{
                "rate", "10", "max", "128", "life", "1, 1.5", "velocity", "0, 0, 0", "velocity_spread", "0, 0, 0",
                "box", "0, 0, 0", "gravity", "0", "drag", "0", "size", "0.2, 0.2", "alpha", "1, 0", "spin", "0, 0",
                "frames", "1", "collide", "false"}));
        LAYERS.put("beam", join(new String[]{"from", "from", "to", "to", "blend", "additive",
                "texture", "", "color", "#ffffff", "port_color", "false", "glow", "true"}, new String[]{
                "width", "0.15", "alpha", "1", "segments", "16", "arc", "0", "jitter", "0", "jitter_rate", "12",
                "scroll", "1", "texture_length", "1", "pulse", "0", "pulse_speed", "2", "flicker", "0", "taper", "0"}));
        LAYERS.put("glow", join(new String[]{"at", "glow", "mode", "faces", "faces", "outer", "blend", "additive",
                "texture", "", "color", "#ffffff", "port_color", "false"}, new String[]{
                "offset", "0.01", "size", "1", "alpha", "1", "pulse", "0", "pulse_speed", "1", "flicker", "0",
                "frames", "1", "frame_rate", "10"}));
    }

    private FxSchema() {
    }

    private static String[] join(String[]... parts) {
        int n = 0;
        for (String[] part : parts) {
            n += part.length;
        }
        String[] result = new String[n];
        int i = 0;
        for (String[] part : parts) {
            System.arraycopy(part, 0, result, i, part.length);
            i += part.length;
        }
        return result;
    }
}

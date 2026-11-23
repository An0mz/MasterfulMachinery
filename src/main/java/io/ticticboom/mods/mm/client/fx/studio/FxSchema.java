package io.ticticboom.mods.mm.client.fx.studio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class FxSchema {
    enum Kind {
        TOGGLE, NUMBER, PAIR, COLOR, TEXTURE, CHOICE, TEXT, ANCHOR
    }

    record Field(String key, Kind kind, int part, int count, String def, double min, double max, double step, List<String> options,
                 boolean more, boolean invert) {
        Field asMore() {
            return new Field(key, kind, part, count, def, min, max, step, options, true, invert);
        }
    }

    static final List<Field> EFFECT = List.of(
            num("rise", "2.4", 0.1, 10, 0.1),
            num("fall", "3.6", 0.1, 10, 0.1),
            num("hold", "1.25", 0, 10, 0.05),
            num("view_distance", "96", 16, 256, 4),
            num("seed", "0", 0, 9999, 1).asMore());

    static final Map<String, List<Field>> LAYERS = new LinkedHashMap<>();

    static {
        LAYERS.put("stream", build(
                new Field[]{tex(), color("#ffffff"), pair("alpha", "0.925", 0, 1, 0.01), num("radius", "0.085", 0.01, 0.4, 0.005),
                        toggle("glow", false), toggle("port_color", false), toggle("port_texture", false)},
                new Field[]{anchor("from", "drips"), anchor("to", "floor"), num("gravity", "9.81", 1, 30, 0.1),
                        num("start_speed", "0.55", 0.05, 5, 0.05), num("sway", "0.02", 0, 0.2, 0.005), num("bulge", "0.22", 0, 1, 0.01),
                        num("flare", "0.55", 0, 2, 0.01), toggle("breakup", true), num("breakup_start", "2.2", 0, 12, 0.1),
                        num("breakup_length", "3.6", 0, 12, 0.1), num("gaps", "0.1", 0, 0.5, 0.01), num("noise", "1", 0, 1, 0.01),
                        num("trickle", "0.2", 0, 1, 0.01), num("texture_speed", "0.109375", -1, 1, 0.01),
                        num("texture_width", "0.25", 0.05, 1, 0.01), toggle("droplets", true), toggle("hide_when_blocked", true),
                        toggle("needs_block_above", true)}));
        LAYERS.put("drip", build(
                new Field[]{tex(), color("#ffffff"), num("alpha", "0.96", 0, 1, 0.01), pair("rate", "1", 0, 4, 0.05),
                        num("size", "0.03", 0.005, 0.1, 0.005), toggle("glow", false), toggle("port_color", false),
                        toggle("port_texture", false)},
                new Field[]{anchor("at", "drips"), anchor("to", "floor"), num("drop_size", "0.036", 0.01, 0.1, 0.002),
                        toggle("hide_when_blocked", true)}));
        LAYERS.put("drops", build(
                new Field[]{tex(), color("#ffffff"), num("alpha", "0.96", 0, 1, 0.01), toggle("glow", false), toggle("port_color", false),
                        toggle("port_texture", false)},
                new Field[]{num("gravity", "9.81", 1, 30, 0.1), num("max", "48", 1, 256, 1), num("stretch", "0.06", 0, 0.3, 0.01),
                        num("splash", "0.12", 0, 0.5, 0.01)}));
        LAYERS.put("pool", build(
                new Field[]{tex(), color("#ffffff"), num("alpha", "1", 0, 1, 0.01), num("size", "0.21", 0.05, 1, 0.01),
                        pair("swirl", "0", 0, 2, 0.01), toggle("glow", false), toggle("port_color", false), toggle("port_texture", false)},
                new Field[]{anchor("at", "drips"), anchor("to", "floor"), num("base", "0.05", 0, 0.3, 0.01), num("fill", "0.55", 0, 2, 0.01),
                        num("drain", "0.16", 0, 1, 0.01), toggle("hide_when_blocked", true)}));
        LAYERS.put("ripple", build(
                new Field[]{tex(), color("#ffffff"), num("alpha", "1", 0, 1, 0.01), toggle("glow", false), toggle("port_color", false),
                        toggle("port_texture", false)},
                new Field[]{num("life", "0.55", 0.1, 3, 0.05), num("width", "0.014", 0.002, 0.1, 0.002), num("speed", "0.42", 0, 2, 0.01),
                        num("max", "32", 1, 256, 1)}));
        LAYERS.put("seep", build(
                new Field[]{tex(), num("alpha", "1", 0, 1, 0.01), num("chance", "0.6", 0, 1, 0.05), num("speed", "1", 0, 4, 0.05),
                        num("streams", "1", 1, 4, 1), toggle("glow", false), toggle("port_color", false)},
                new Field[]{anchor("at", "drips"), anchor("top", "seep_top"), toggle("stains", true), toggle("drip_patch", true),
                        text("faces", ""), text("palette", "")}));
        LAYERS.put("emitter", build(
                new Field[]{tex(), colorPart("color", 0, 2, "#ffffff, #ffffff"), colorPart("color", 1, 2, "#ffffff, #ffffff"),
                        pair("rate", "10", 0, 100, 0.5), part("size", 0, 2, "0.2, 0.2", 0, 1.5, 0.01), part("size", 1, 2, "0.2, 0.2", 0, 1.5, 0.01),
                        part("life", 0, 2, "1, 1.5", 0.05, 10, 0.05), part("life", 1, 2, "1, 1.5", 0.05, 10, 0.05),
                        part("alpha", 0, 2, "1, 0", 0, 1, 0.01), part("alpha", 1, 2, "1, 0", 0, 1, 0.01),
                        choice("blend", "translucent", "translucent", "additive"), toggle("glow", false), toggle("port_color", false)},
                new Field[]{anchor("at", "emit"), num("max", "128", 1, 1024, 1),
                        part("velocity", 0, 3, "0, 0, 0", -3, 3, 0.05), part("velocity", 1, 3, "0, 0, 0", -3, 3, 0.05),
                        part("velocity", 2, 3, "0, 0, 0", -3, 3, 0.05),
                        part("velocity_spread", 0, 3, "0, 0, 0", 0, 2, 0.05), part("velocity_spread", 1, 3, "0, 0, 0", 0, 2, 0.05),
                        part("velocity_spread", 2, 3, "0, 0, 0", 0, 2, 0.05),
                        part("box", 0, 3, "0, 0, 0", 0, 4, 0.05), part("box", 1, 3, "0, 0, 0", 0, 4, 0.05), part("box", 2, 3, "0, 0, 0", 0, 4, 0.05),
                        num("gravity", "0", -2, 2, 0.01), num("drag", "0", 0, 3, 0.05),
                        part("spin", 0, 2, "0, 0", -720, 720, 5), part("spin", 1, 2, "0, 0", -720, 720, 5),
                        num("frames", "1", 1, 16, 1), toggle("collide", false), toggle("smooth", true), num("fade_in", "0", 0, 1, 0.01),
                        num("turbulence", "0", 0, 2, 0.01), num("size_spread", "0", 0, 1, 0.01),
                        choice("size_curve", "linear", "linear", "ease_out")}));
        LAYERS.put("beam", build(
                new Field[]{tex(), color("#ffffff"), pair("width", "0.15", 0, 1, 0.005), pair("alpha", "1", 0, 1, 0.01),
                        choice("blend", "additive", "additive", "translucent"), toggle("glow", true), toggle("port_color", false),
                        num("pulse", "0", 0, 1, 0.01)},
                new Field[]{anchor("from", "from"), anchor("to", "to"), text("links", ""), num("segments", "16", 1, 64, 1),
                        num("arc", "0", 0, 2, 0.05), num("jitter", "0", 0, 1, 0.01), num("jitter_rate", "12", 0, 60, 1),
                        num("scroll", "1", -10, 10, 0.1), num("texture_length", "1", 0.1, 8, 0.1), num("pulse_speed", "2", 0, 10, 0.1),
                        num("flicker", "0", 0, 1, 0.01), num("taper", "0", 0, 1, 0.01)}));
        LAYERS.put("glow", build(
                new Field[]{tex(), color("#ffffff"), choice("mode", "faces", "faces", "halo"), pair("size", "1", 0, 4, 0.05),
                        pair("alpha", "1", 0, 1, 0.01), choice("blend", "additive", "additive", "translucent"), toggle("port_color", false),
                        num("pulse", "0", 0, 1, 0.01)},
                new Field[]{anchor("at", "glow"), choice("faces", "outer", "outer", "all"), num("offset", "0.01", 0, 0.2, 0.005),
                        num("pulse_speed", "1", 0, 10, 0.1), num("flicker", "0", 0, 1, 0.01), num("frames", "1", 1, 16, 1),
                        num("frame_rate", "10", 1, 30, 1)}));
    }

    private FxSchema() {
    }

    static String[] rawKeys(List<Field> fields) {
        var result = new ArrayList<String>();
        var seen = new ArrayList<String>();
        for (Field f : fields) {
            if (seen.contains(f.key())) {
                continue;
            }
            seen.add(f.key());
            result.add(f.key());
            result.add(f.def());
        }
        return result.toArray(new String[0]);
    }

    private static List<Field> build(Field[] basic, Field[] more) {
        var result = new ArrayList<Field>();
        result.add(text("name", ""));
        result.add(new Field("off", Kind.TOGGLE, -1, 1, "false", 0, 1, 1, List.of(), false, true));
        for (Field f : basic) {
            result.add(f);
        }
        for (Field f : more) {
            result.add(f.asMore());
        }
        result.add(text("skip", "").asMore());
        return List.copyOf(result);
    }

    private static Field tex() {
        return new Field("texture", Kind.TEXTURE, -1, 1, "", 0, 0, 0, List.of(), false, false);
    }

    private static Field color(String def) {
        return new Field("color", Kind.COLOR, -1, 1, def, 0, 0, 0, List.of(), false, false);
    }

    private static Field colorPart(String key, int part, int count, String def) {
        return new Field(key, Kind.COLOR, part, count, def, 0, 0, 0, List.of(), false, false);
    }

    private static Field toggle(String key, boolean def) {
        return new Field(key, Kind.TOGGLE, -1, 1, String.valueOf(def), 0, 1, 1, List.of(), false, false);
    }

    private static Field num(String key, String def, double min, double max, double step) {
        return new Field(key, Kind.NUMBER, -1, 1, def, min, max, step, List.of(), false, false);
    }

    private static Field pair(String key, String def, double min, double max, double step) {
        return new Field(key, Kind.PAIR, -1, 1, def, min, max, step, List.of(), false, false);
    }

    private static Field part(String key, int part, int count, String def, double min, double max, double step) {
        return new Field(key, Kind.NUMBER, part, count, def, min, max, step, List.of(), false, false);
    }

    private static Field choice(String key, String def, String... options) {
        return new Field(key, Kind.CHOICE, -1, 1, def, 0, 0, 0, List.of(options), false, false);
    }

    private static Field text(String key, String def) {
        return new Field(key, Kind.TEXT, -1, 1, def, 0, 0, 0, List.of(), false, false);
    }

    private static Field anchor(String key, String def) {
        return new Field(key, Kind.ANCHOR, -1, 1, def, 0, 0, 0, List.of(), false, false);
    }
}

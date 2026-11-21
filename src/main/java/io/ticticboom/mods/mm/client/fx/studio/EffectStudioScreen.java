package io.ticticboom.mods.mm.client.fx.studio;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.vertex.PoseStack;
import io.ticticboom.mods.mm.client.fx.FxInstance;
import io.ticticboom.mods.mm.client.fx.FxManager;
import io.ticticboom.mods.mm.client.fx.FxRenderer;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import io.ticticboom.mods.mm.client.structure.GuiStructureRenderer;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.structure.layout.PositionedLayoutPiece;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EffectStudioScreen extends Screen {
    private static final String[] COLORS = {"#c01818", "#ff7a1a", "#ffd040", "#3aff7a", "#40e0ff", "#3f76e4", "#a040ff", "#ff60c0", "#ffffff", "#808080"};
    private static final Set<String> AMOUNT = Set.of("rate", "max", "chance");
    private static final Set<String> SIZE = Set.of("size", "radius", "width", "drop_size");
    private static final Set<String> SPEED = Set.of("velocity", "scroll", "speed", "start_speed", "pulse_speed", "jitter_rate", "frame_rate");
    private static final int PANEL = 130;
    private static final Set<String> AUTO = Set.of("floor", "landing", "sky", "block", "below");

    private final EffectStudio.Session session;
    private final Map<String, JsonObject> presets = EffectStudio.library("mm_fx_presets");
    private final Map<String, JsonObject> parts = EffectStudio.library("mm_fx_parts");
    private final List<String> partNames = new ArrayList<>(parts.keySet());
    private int part;
    @Nullable
    private BlockHitResult firstHit;
    private final Map<String, Direction> faces = new HashMap<>();
    private final List<String[]> steps = new ArrayList<>();
    private final JsonObject pending = new JsonObject();
    @Nullable
    private GuiStructureRenderer view;
    @Nullable
    private StructureModel model;
    @Nullable
    private FxInstance preview;
    private GuiPos viewport = GuiPos.of(0, 0, 1, 1);
    private float partialTick;
    @Nullable
    private String placing;
    private int colorsY;
    private double pressX;
    private double pressY;
    private long lastFrame;

    public EffectStudioScreen(EffectStudio.Session session) {
        super(Component.translatable("gui.mm.effect_studio.title"));
        this.session = session;
    }

    @Override
    protected void init() {
        var be = EffectStudio.machine(session);
        if (be == null) {
            onClose();
            return;
        }
        session.open = true;
        if (view == null || model != be.getStructure()) {
            model = be.getStructure();
            view = new GuiStructureRenderer(model);
            view.setOverlay(this::renderEffects);
            view.setStudio(true);
        }
        viewport = GuiPos.of(PANEL + 12, 20, width - PANEL * 2 - 24, height - 52);

        int y = 22;
        for (String name : presets.keySet()) {
            addRenderableWidget(Button.builder(Component.literal(pretty(name)), b -> startPlacing(name))
                    .bounds(6, y, PANEL, 18).build());
            y += 20;
        }

        int x = width - PANEL - 6;
        y = 22;
        for (int i = 0; i < session.effects.size(); i++) {
            int index = i;
            addRenderableWidget(Button.builder(Component.literal((i == session.selected ? "> " : "") + label(session.effects.get(i))), b -> {
                session.selected = index;
                rebuildWidgets();
            }).bounds(x, y, PANEL, 18).build());
            y += 20;
        }
        ResourceLocation id = selectedId();
        if (id != null) {
            addRenderableWidget(Button.builder(Component.translatable("gui.mm.effect_studio.advanced"), b -> {
                session.originals.remove(id);
                session.sliders.remove(id);
                session.colors.remove(id);
                minecraft.setScreen(new EffectLayersScreen(session, this, id));
            }).bounds(x, y, 64, 18).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.mm.effect_studio.remove"), b -> {
                session.edited.remove(id);
                session.effects.remove(session.selected);
                session.selected = -1;
                session.dirty = true;
                rebuildWidgets();
            }).bounds(x + 66, y, 64, 18).build());
            y += 24;
            double[] values = session.sliders.computeIfAbsent(id, k -> new double[]{0.5, 0.5, 0.5});
            String[] names = {"amount", "size", "speed"};
            for (int i = 0; i < 3; i++) {
                addRenderableWidget(new Slider(x, y, names[i], id, values, i));
                y += 22;
            }
            colorsY = y;
            for (int i = 0; i < COLORS.length; i++) {
                String color = COLORS[i];
                addRenderableWidget(Button.builder(Component.empty(), b -> {
                    session.colors.put(id, color);
                    rescale(id);
                }).bounds(x + (i % 5) * 26, y + (i / 5) * 18, 24, 16).build());
            }
            y += 40;
            JsonObject base = base(id);
            if (base != null && base.has("layers")) {
                JsonArray layers = base.getAsJsonArray("layers");
                for (int i = 0; i < layers.size(); i++) {
                    JsonObject layer = layers.get(i).getAsJsonObject();
                    boolean off = layer.has("off") && layer.get("off").getAsBoolean();
                    String name = layer.has("name") ? layer.get("name").getAsString() : pretty(layer.has("type") ? layer.get("type").getAsString() : "?");
                    addRenderableWidget(Button.builder(Component.literal(name).withStyle(off ? ChatFormatting.DARK_GRAY : ChatFormatting.GREEN), b -> {
                        setOff(layer, !off);
                        if (!rescale(id)) {
                            setOff(layer, off);
                            rescale(id);
                        }
                        rebuildWidgets();
                    }).bounds(x + (i % 2) * 66, y + (i / 2) * 18, 64, 16).build());
                }
                y += (layers.size() + 1) / 2 * 18 + 4;
            }
            if (!partNames.isEmpty()) {
                part = Math.min(part, partNames.size() - 1);
                addRenderableWidget(Button.builder(Component.literal(pretty(partNames.get(part))), b -> {
                    part = (part + 1) % partNames.size();
                    b.setMessage(Component.literal(pretty(partNames.get(part))));
                }).bounds(x, y, 64, 18).build());
                addRenderableWidget(Button.builder(Component.translatable("gui.mm.effect_studio.add_part"), b -> addPart(id, partNames.get(part)))
                        .bounds(x + 66, y, 64, 18).build());
                y += 22;
            }
        }

        int by = height - 26;
        addRenderableWidget(Button.builder(Component.translatable(session.idle ? "gui.mm.effect_studio.idle" : "gui.mm.effect_studio.working"), b -> {
            session.idle = !session.idle;
            b.setMessage(Component.translatable(session.idle ? "gui.mm.effect_studio.idle" : "gui.mm.effect_studio.working"));
        }).bounds(viewport.x(), by, 90, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.mm.effect_studio.save"), b -> {
            boolean ok = EffectStudio.save(session);
            minecraft.player.displayClientMessage(Component.translatable(ok ? "gui.mm.effect_studio.saved" : "gui.mm.effect_studio.save_failed"), true);
        }).bounds(viewport.x() + 94, by, 70, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.mm.effect_studio.close"), b -> onClose())
                .bounds(viewport.x() + 168, by, 70, 20).build());
    }

    @Nullable
    private ResourceLocation selectedId() {
        if (session.selected < 0 || session.selected >= session.effects.size()) {
            return null;
        }
        JsonObject effect = session.effects.get(session.selected);
        return effect.has("fx") ? ResourceLocation.tryParse(effect.get("fx").getAsString()) : null;
    }

    private String label(JsonObject effect) {
        ResourceLocation id = effect.has("fx") ? ResourceLocation.tryParse(effect.get("fx").getAsString()) : null;
        JsonObject json = id == null ? null : session.edited.getOrDefault(id, FxManager.source(id));
        if (json != null && json.has("name")) {
            return pretty(json.get("name").getAsString());
        }
        return id == null ? "?" : id.getPath();
    }

    private static String pretty(String name) {
        String text = name.replace('_', ' ');
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private void startPlacing(String preset) {
        placing = preset;
        firstHit = null;
        steps.clear();
        for (String key : pending.keySet().toArray(new String[0])) {
            pending.remove(key);
        }
        var seen = new LinkedHashMap<String, String>();
        for (JsonElement element : presets.get(preset).getAsJsonArray("layers")) {
            JsonObject layer = element.getAsJsonObject();
            boolean area = "glow".equals(layer.has("type") ? layer.get("type").getAsString() : "")
                    && !(layer.has("mode") && layer.get("mode").getAsString().equals("halo"));
            for (String key : new String[]{"at", "from", "to"}) {
                if (layer.has(key)) {
                    String name = layer.get(key).getAsString();
                    seen.putIfAbsent(name, AUTO.contains(name) ? "auto" : area ? "area" : "point");
                }
            }
        }
        seen.forEach((name, kind) -> steps.add(new String[]{name, kind}));
        if (currentStep() == null) {
            steps.add(0, new String[]{"spot", "point"});
        }
    }

    @Nullable
    private String[] currentStep() {
        for (String[] step : steps) {
            if (!step[1].equals("auto") && !pending.has(step[0])) {
                return step;
            }
        }
        return null;
    }

    private void click(BlockHitResult hit) {
        String[] step = currentStep();
        if (step == null) {
            return;
        }
        var origin = model.layout().getCharGrid().getControllerPos();
        if (step[1].equals("area")) {
            String key = keyAt(hit.getBlockPos());
            if (key == null) {
                return;
            }
            pending.addProperty(step[0], key);
        } else {
            Vec3 at = hit.getLocation();
            if (firstHit == null) {
                firstHit = hit;
            }
            faces.put(step[0], hit.getDirection());
            pending.add(step[0], point(at.x + origin.getX(), at.y + origin.getY(), at.z + origin.getZ()));
        }
        if (currentStep() == null) {
            finish();
        }
    }

    private void selectAt(double mouseX, double mouseY, @Nullable BlockHitResult hit) {
        var origin = model.layout().getCharGrid().getControllerPos();
        String key = hit == null ? null : keyAt(hit.getBlockPos());
        int best = -1;
        double bestDistance = 12 * 12;
        int keyed = -1;
        for (int i = 0; i < session.effects.size(); i++) {
            JsonObject effect = session.effects.get(i);
            JsonObject anchors = effect.has("anchors") && effect.get("anchors").isJsonObject() ? effect.getAsJsonObject("anchors") : new JsonObject();
            var points = new ArrayList<double[]>();
            var heights = new ArrayList<Double>();
            for (var entry : anchors.entrySet()) {
                JsonElement value = entry.getValue();
                if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                    heights.add(value.getAsDouble() - origin.getY());
                } else if (value.isJsonPrimitive() && value.getAsString().equals(key)) {
                    keyed = i;
                } else if (value.isJsonArray()) {
                    JsonArray list = value.getAsJsonArray();
                    boolean single = list.size() == 3 && list.get(0).isJsonPrimitive();
                    for (JsonElement p : single ? List.<JsonElement>of(list) : list.asList()) {
                        if (p.isJsonArray() && p.getAsJsonArray().size() >= 3) {
                            JsonArray a = p.getAsJsonArray();
                            points.add(new double[]{a.get(0).getAsDouble() - origin.getX(), a.get(1).getAsDouble() - origin.getY(), a.get(2).getAsDouble() - origin.getZ()});
                        }
                    }
                }
            }
            var segments = new ArrayList<double[][]>();
            for (double[] p : points) {
                segments.add(new double[][]{p, p});
                for (double h : heights) {
                    segments.add(new double[][]{p, {p[0], h, p[2]}});
                }
            }
            for (int a = 0; a < points.size(); a++) {
                for (int b = a + 1; b < points.size(); b++) {
                    segments.add(new double[][]{points.get(a), points.get(b)});
                }
            }
            for (double[][] segment : segments) {
                double[] s0 = view.project(segment[0][0], segment[0][1], segment[0][2]);
                double[] s1 = view.project(segment[1][0], segment[1][1], segment[1][2]);
                if (s0 == null || s1 == null) {
                    continue;
                }
                double d = distanceToSegment(mouseX, mouseY, s0, s1);
                if (d < bestDistance) {
                    bestDistance = d;
                    best = i;
                }
            }
        }
        if (best < 0) {
            best = keyed;
        }
        if (best >= 0) {
            session.selected = best;
            rebuildWidgets();
        }
    }

    private static double distanceToSegment(double x, double y, double[] a, double[] b) {
        double dx = b[0] - a[0];
        double dy = b[1] - a[1];
        double length = dx * dx + dy * dy;
        double t = length == 0 ? 0 : Math.max(0, Math.min(1, ((x - a[0]) * dx + (y - a[1]) * dy) / length));
        double px = a[0] + t * dx - x;
        double py = a[1] + t * dy - y;
        return px * px + py * py;
    }

    private void move() {
        long now = System.nanoTime();
        float dt = lastFrame == 0 ? 0 : Math.min(0.1f, (now - lastFrame) / 1e9f);
        lastFrame = now;
        long window = minecraft.getWindow().getWindow();
        float speed = 8 * dt;
        float forward = (key(window, GLFW.GLFW_KEY_W) ? speed : 0) - (key(window, GLFW.GLFW_KEY_S) ? speed : 0);
        float strafe = (key(window, GLFW.GLFW_KEY_D) ? speed : 0) - (key(window, GLFW.GLFW_KEY_A) ? speed : 0);
        float up = (key(window, GLFW.GLFW_KEY_SPACE) ? speed : 0) - (key(window, GLFW.GLFW_KEY_LEFT_SHIFT) ? speed : 0);
        if (forward != 0 || strafe != 0 || up != 0) {
            view.fly(forward, strafe, up, true);
        }
    }

    private static boolean key(long window, int key) {
        return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }

    @Nullable
    private String keyAt(BlockPos pos) {
        for (PositionedLayoutPiece piece : model.layout().getPositionedPieces()) {
            if (piece.pos().equals(pos)) {
                return piece.piece().valueId();
            }
        }
        return null;
    }

    private void finish() {
        JsonArray source = null;
        Direction face = Direction.DOWN;
        for (String[] step : steps) {
            if (step[1].equals("point") && pending.has(step[0])) {
                source = pending.getAsJsonArray(step[0]);
                face = faces.getOrDefault(step[0], Direction.DOWN);
                break;
            }
        }
        for (String[] step : steps) {
            if (step[1].equals("auto")) {
                JsonElement value = auto(step[0], source, face, firstHit == null ? null : firstHit.getBlockPos());
                if (value != null) {
                    pending.add(step[0], value);
                }
            }
        }
        int n = 1;
        ResourceLocation id;
        do {
            id = session.structure.withSuffix("_fx" + n++);
        } while (FxManager.source(id) != null || session.edited.containsKey(id));
        JsonObject json = presets.get(placing).deepCopy();
        json.addProperty("name", placing);
        try {
            FxManager.preview(id, json);
        } catch (Exception e) {
            placing = null;
            steps.clear();
            return;
        }
        session.edited.put(id, json);
        var effect = new JsonObject();
        effect.addProperty("fx", id.toString());
        effect.add("anchors", pending.deepCopy());
        session.effects.add(effect);
        session.selected = session.effects.size() - 1;
        session.dirty = true;
        placing = null;
        steps.clear();
        rebuildWidgets();
    }

    private static JsonArray point(double x, double y, double z) {
        var a = new JsonArray();
        a.add(Math.round(x * 16) / 16.0);
        a.add(Math.round(y * 16) / 16.0);
        a.add(Math.round(z * 16) / 16.0);
        return a;
    }

    @Nullable
    private JsonObject base(ResourceLocation id) {
        JsonObject current = session.edited.getOrDefault(id, FxManager.source(id));
        return current == null ? null : session.originals.computeIfAbsent(id, k -> current.deepCopy());
    }

    @Nullable
    private JsonElement auto(String name, @Nullable JsonArray source, Direction face, @Nullable BlockPos clicked) {
        var origin = model.layout().getCharGrid().getControllerPos();
        double sx = source == null ? origin.getX() + 0.5 : source.get(0).getAsDouble();
        double sy = source == null ? origin.getY() : source.get(1).getAsDouble();
        double sz = source == null ? origin.getZ() + 0.5 : source.get(2).getAsDouble();
        int cx = (int) Math.floor(sx - origin.getX() + face.getStepX() * 0.01);
        int cz = (int) Math.floor(sz - origin.getZ() + face.getStepZ() * 0.01);
        double rel = sy - origin.getY();
        var world = view.previewWorld();
        double fx = sx - origin.getX() + face.getStepX() * 0.01;
        double fz = sz - origin.getZ() + face.getStepZ() * 0.01;
        double floor = sy - 4;
        int base = (int) Math.floor(floor - origin.getY()) - 1;
        for (int y = (int) Math.floor(rel - 0.001); y >= (int) rel - 64; y--) {
            var pos = new BlockPos(cx, y, cz);
            var state = world.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            var shape = state.getShape(world, pos);
            var hit = (shape.isEmpty() ? Shapes.block() : shape).clip(new Vec3(fx, Math.min(rel - 0.001, y + 1), fz), new Vec3(fx, y, fz), pos);
            if (hit != null) {
                floor = hit.getLocation().y + origin.getY();
                base = y;
                break;
            }
        }
        if (clicked == null) {
            clicked = new BlockPos((int) Math.floor(sx - origin.getX() - face.getStepX() * 0.01),
                    (int) Math.floor(sy - origin.getY() - face.getStepY() * 0.01), (int) Math.floor(sz - origin.getZ() - face.getStepZ() * 0.01));
        }
        String key = switch (name) {
            case "block" -> keyAt(clicked);
            case "below" -> keyAt(new BlockPos(cx, base, cz));
            default -> null;
        };
        return switch (name) {
            case "floor" -> new JsonPrimitive(floor);
            case "landing" -> point(sx, floor, sz);
            case "sky" -> point(sx, sy + 12, sz);
            case "block", "below" -> key == null ? null : new JsonPrimitive(key);
            default -> null;
        };
    }

    @Nullable
    private static JsonArray firstPoint(JsonObject anchors) {
        JsonElement spot = anchors.get("spot");
        if (spot == null || !spot.isJsonArray()) {
            spot = null;
            for (var entry : anchors.entrySet()) {
                if (entry.getValue().isJsonArray() && !entry.getKey().equals("landing") && !entry.getKey().equals("sky")) {
                    spot = entry.getValue();
                    break;
                }
            }
        }
        if (spot == null) {
            return null;
        }
        JsonArray list = spot.getAsJsonArray();
        if (list.size() > 0 && list.get(0).isJsonArray()) {
            list = list.get(0).getAsJsonArray();
        }
        return list.size() >= 3 ? list : null;
    }

    private static void setOff(JsonObject layer, boolean off) {
        if (off) {
            layer.addProperty("off", true);
        } else {
            layer.remove("off");
        }
    }

    private void addPart(ResourceLocation id, String name) {
        JsonObject base = base(id);
        JsonObject effect = session.effects.get(session.selected);
        if (base == null || !base.has("layers")) {
            return;
        }
        if (!effect.has("anchors") || !effect.get("anchors").isJsonObject()) {
            effect.add("anchors", new JsonObject());
        }
        JsonObject anchors = effect.getAsJsonObject("anchors");
        JsonArray source = firstPoint(anchors);
        JsonArray layers = base.getAsJsonArray("layers");
        for (JsonElement element : parts.get(name).getAsJsonArray("layers")) {
            JsonObject layer = element.getAsJsonObject().deepCopy();
            String type = layer.has("type") ? layer.get("type").getAsString() : "";
            if (type.equals("drops") || type.equals("ripple")) {
                boolean exists = false;
                for (JsonElement other : layers) {
                    exists |= other.isJsonObject() && other.getAsJsonObject().has("type") && other.getAsJsonObject().get("type").getAsString().equals(type);
                }
                if (exists) {
                    continue;
                }
            }
            for (String key : new String[]{"at", "from", "to"}) {
                if (!layer.has(key)) {
                    continue;
                }
                String anchor = layer.get(key).getAsString();
                if (anchors.has(anchor)) {
                    continue;
                }
                JsonElement value = AUTO.contains(anchor) ? auto(anchor, source, Direction.DOWN, null) : source;
                if (value != null) {
                    anchors.add(anchor, value.deepCopy());
                }
            }
            layers.add(layer);
        }
        session.dirty = true;
        rescale(id);
        rebuildWidgets();
    }

    private boolean rescale(ResourceLocation id) {
        JsonObject base = base(id);
        if (base == null) {
            return false;
        }
        double[] values = session.sliders.computeIfAbsent(id, k -> new double[]{0.5, 0.5, 0.5});
        JsonObject json = base.deepCopy();
        String color = session.colors.get(id);
        if (json.has("layers")) {
            for (JsonElement element : json.getAsJsonArray("layers")) {
                JsonObject layer = element.getAsJsonObject();
                for (String key : layer.keySet()) {
                    double m = AMOUNT.contains(key) ? factor(values[0]) : SIZE.contains(key) ? factor(values[1]) : SPEED.contains(key) ? factor(values[2]) : 1;
                    if (m == 1) {
                        continue;
                    }
                    JsonElement scaled = scale(layer.get(key), m);
                    if (key.equals("max") && scaled.isJsonPrimitive()) {
                        scaled = new JsonPrimitive(Math.max(1, Math.round(scaled.getAsDouble())));
                    } else if (key.equals("chance") && scaled.isJsonPrimitive()) {
                        scaled = new JsonPrimitive(Math.min(1, scaled.getAsDouble()));
                    }
                    layer.add(key, scaled);
                }
                if (color != null && !(layer.has("type") && layer.get("type").getAsString().equals("seep"))) {
                    layer.addProperty("color", color);
                }
            }
        }
        try {
            FxManager.preview(id, json);
            session.edited.put(id, json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static double factor(double value) {
        return Math.pow(4, value * 2 - 1);
    }

    private static JsonElement scale(JsonElement value, double m) {
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            return new JsonPrimitive(Math.round(value.getAsDouble() * m * 10000) / 10000.0);
        }
        if (value.isJsonArray()) {
            var a = new JsonArray();
            value.getAsJsonArray().forEach(e -> a.add(scale(e, m)));
            return a;
        }
        if (value.isJsonObject()) {
            var o = new JsonObject();
            value.getAsJsonObject().entrySet().forEach(e -> o.add(e.getKey(), scale(e.getValue(), m)));
            return o;
        }
        return value;
    }

    private void renderEffects(PoseStack pose, MultiBufferSource buffers, Matrix4f modelView) {
        var be = EffectStudio.machine(session);
        var visuals = be == null ? null : EffectStudio.effects(be);
        if (visuals == null || model == null) {
            return;
        }
        preview = FxRenderer.preview(preview, model, visuals);
        var inverse = new Matrix4f(modelView).invert();
        Vector3f camera = inverse.transformPosition(new Vector3f());
        Vector3f look = inverse.transformDirection(new Vector3f(0, 0, -1)).normalize();
        Vector3f up = inverse.transformDirection(new Vector3f(0, 1, 0)).normalize();
        Vector3f left = inverse.transformDirection(new Vector3f(-1, 0, 0)).normalize();
        FxRenderer.renderPreview(preview, view.previewWorld(), pose, buffers, camera, left, up, look, !session.idle, partialTick);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);
        if (view == null) {
            return;
        }
        this.partialTick = partialTick;
        move();
        gfx.fill(viewport.x(), viewport.y(), viewport.x() + viewport.w(), viewport.y() + viewport.h(), 0x66000000);
        gfx.pose().pushPose();
        gfx.pose().setIdentity();
        view.setViewport(viewport);
        view.init();
        view.render(gfx, mouseX, mouseY, viewport.contains(mouseX, mouseY));
        gfx.pose().popPose();
        gfx.drawString(font, Component.translatable("gui.mm.effect_studio.add_effect"), 6, 8, 0xFFFFFF);
        gfx.drawString(font, Component.translatable("gui.mm.effect_studio.placed"), width - PANEL - 6, 8, 0xFFFFFF);
        String[] step = placing == null ? null : currentStep();
        Component hint = step != null
                ? Component.translatable("gui.mm.effect_studio.click." + step[1], pretty(placing), step[0])
                : Component.translatable("gui.mm.effect_studio.controls");
        gfx.drawCenteredString(font, hint, viewport.x() + viewport.w() / 2, viewport.y() + 4, step != null ? 0xFFFF55 : 0xA0A0A0);
        if (selectedId() != null) {
            int x = width - PANEL - 6;
            for (int i = 0; i < COLORS.length; i++) {
                int cx = x + (i % 5) * 26 + 3;
                int cy = colorsY + (i / 5) * 18 + 3;
                gfx.fill(cx, cy, cx + 18, cy + 10, 0xFF000000 | Integer.parseInt(COLORS[i].substring(1), 16));
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        pressX = mouseX;
        pressY = mouseY;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && view != null && viewport.contains((int) mouseX, (int) mouseY)
                && Math.abs(mouseX - pressX) + Math.abs(mouseY - pressY) < 4) {
            BlockHitResult hit = view.pick(mouseX, mouseY);
            if (placing != null) {
                if (hit != null) {
                    click(hit);
                }
            } else {
                selectAt(mouseX, mouseY, hit);
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void removed() {
        session.open = false;
        GuiStructureRenderer.clearHovered();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && placing != null) {
            placing = null;
            steps.clear();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_W || keyCode == GLFW.GLFW_KEY_A || keyCode == GLFW.GLFW_KEY_S
                || keyCode == GLFW.GLFW_KEY_D || keyCode == GLFW.GLFW_KEY_SPACE) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private final class Slider extends AbstractSliderButton {
        private final String name;
        private final ResourceLocation id;
        private final double[] values;
        private final int index;

        Slider(int x, int y, String name, ResourceLocation id, double[] values, int index) {
            super(x, y, PANEL, 20, Component.empty(), values[index]);
            this.name = name;
            this.id = id;
            this.values = values;
            this.index = index;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("gui.mm.effect_studio." + name, String.format("%.2fx", factor(value))));
        }

        @Override
        protected void applyValue() {
            values[index] = value;
            rescale(id);
        }
    }
}

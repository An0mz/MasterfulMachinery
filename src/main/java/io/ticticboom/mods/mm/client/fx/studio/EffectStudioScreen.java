package io.ticticboom.mods.mm.client.fx.studio;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
    private static final Set<String> FALLING = Set.of("drip", "stream", "pool", "seep");
    private static final Set<String> NOTES = Set.of("drip_working", "hidden_now");
    private static final Set<String> DERIVED = Set.of("floor", "landing", "sky");
    private static final int[] SPOT_COLORS = {0xFF40E0FF, 0xFFFFD040, 0xFFFF60C0, 0xFF3AFF7A, 0xFFFF7A1A, 0xFFA040FF};

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
    private final List<int[]> dots = new ArrayList<>();
    private final Set<String> undersides = new HashSet<>();
    @Nullable
    private BlockHitResult hover;
    @Nullable
    private Vec3 ghostAt;
    private double ghostFloor;
    @Nullable
    private String held;
    @Nullable
    private String adding;
    private final List<String> spots = new ArrayList<>();
    private int spotsY;

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
        dots.clear();
        spots.clear();
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
        if (selectedId() != null) {
            spots.addAll(spotNames(selectedAnchors()));
            if (adding != null && !spots.contains(adding)) {
                adding = null;
            }
            y += 14;
            spotsY = y;
            for (String spot : spots) {
                spotButton(y, 0, "+", "add", () -> {
                    adding = adding != null && adding.equals(spot) ? null : spot;
                    placing = null;
                    steps.clear();
                });
                spotButton(y, 1, "-", "remove", () -> removeLast(spot));
                spotButton(y, 2, "x4", "rotate", () -> symmetry(spot, 0));
                spotButton(y, 3, "X", "mirror_x", () -> symmetry(spot, 1));
                spotButton(y, 4, "Z", "mirror_z", () -> symmetry(spot, 2));
                y += 18;
            }
        }

        int x = width - PANEL - 6;
        y = 22;
        for (int i = 0; i < session.effects.size(); i++) {
            int index = i;
            addRenderableWidget(Button.builder(Component.literal((i == session.selected ? "> " : "") + label(session.effects.get(i))), b -> {
                session.selected = index;
                adding = null;
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
                int live = 0;
                for (int i = 0; i < layers.size(); i++) {
                    JsonObject layer = layers.get(i).getAsJsonObject();
                    boolean off = layer.has("off") && layer.get("off").getAsBoolean();
                    if (!off) {
                        dots.add(new int[]{x + (i % 2) * 66, y + (i / 2) * 18, live++});
                    }
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
        addRenderableWidget(Button.builder(Component.translatable(session.fast ? "gui.mm.effect_studio.fast_on" : "gui.mm.effect_studio.fast_off"), b -> {
            session.fast = !session.fast;
            b.setMessage(Component.translatable(session.fast ? "gui.mm.effect_studio.fast_on" : "gui.mm.effect_studio.fast_off"));
        }).bounds(viewport.x() + 94, by, 70, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.mm.effect_studio.save"), b -> {
            boolean ok = EffectStudio.save(session);
            minecraft.player.displayClientMessage(Component.translatable(ok ? "gui.mm.effect_studio.saved" : "gui.mm.effect_studio.save_failed"), true);
        }).bounds(viewport.x() + 168, by, 70, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.mm.effect_studio.close"), b -> onClose())
                .bounds(viewport.x() + 242, by, 70, 20).build());
    }

    private void spotButton(int y, int column, String text, String tip, Runnable action) {
        int[] widths = {14, 14, 20, 14, 14};
        int x = 6 + PANEL;
        for (int i = widths.length - 1; i >= column; i--) {
            x -= widths[i] + (i == widths.length - 1 ? 0 : 2);
        }
        addRenderableWidget(Button.builder(Component.literal(text), b -> {
                    action.run();
                    setFocused(null);
                })
                .tooltip(Tooltip.create(Component.translatable("gui.mm.effect_studio.spot." + tip)))
                .bounds(x, y, widths[column], 16).build());
    }

    private JsonObject selectedAnchors() {
        JsonObject effect = session.effects.get(session.selected);
        if (!effect.has("anchors") || !effect.get("anchors").isJsonObject()) {
            effect.add("anchors", new JsonObject());
        }
        return effect.getAsJsonObject("anchors");
    }

    private static List<String> spotNames(JsonObject anchors) {
        var names = new ArrayList<String>();
        for (var entry : anchors.entrySet()) {
            if (entry.getValue().isJsonArray() && !DERIVED.contains(entry.getKey())) {
                names.add(entry.getKey());
            }
        }
        return names;
    }

    private static List<double[]> pointsOf(@Nullable JsonElement element) {
        var result = new ArrayList<double[]>();
        if (element == null || !element.isJsonArray()) {
            return result;
        }
        JsonArray list = element.getAsJsonArray();
        if (list.size() == 3 && list.get(0).isJsonPrimitive()) {
            result.add(new double[]{list.get(0).getAsDouble(), list.get(1).getAsDouble(), list.get(2).getAsDouble()});
            return result;
        }
        for (JsonElement e : list) {
            if (e.isJsonArray() && e.getAsJsonArray().size() == 3) {
                JsonArray a = e.getAsJsonArray();
                result.add(new double[]{a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()});
            }
        }
        return result;
    }

    private static JsonElement pointsJson(List<double[]> points) {
        if (points.size() == 1) {
            return point(points.get(0)[0], points.get(0)[1], points.get(0)[2]);
        }
        var list = new JsonArray();
        for (double[] p : points) {
            list.add(point(p[0], p[1], p[2]));
        }
        return list;
    }

    private static void appendPoint(JsonObject anchors, String name, JsonArray point) {
        List<double[]> points = pointsOf(anchors.get(name));
        double[] next = {point.get(0).getAsDouble(), point.get(1).getAsDouble(), point.get(2).getAsDouble()};
        if (!contains(points, next)) {
            points.add(next);
        }
        anchors.add(name, pointsJson(points));
    }

    private static boolean contains(List<double[]> points, double[] p) {
        for (double[] q : points) {
            if (Math.abs(q[0] - p[0]) < 1e-3 && Math.abs(q[1] - p[1]) < 1e-3 && Math.abs(q[2] - p[2]) < 1e-3) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static String sourceName(JsonObject anchors) {
        if (anchors.has("spot") && anchors.get("spot").isJsonArray()) {
            return "spot";
        }
        List<String> names = spotNames(anchors);
        return names.isEmpty() ? null : names.get(0);
    }

    private Set<String> undersidesOf(JsonObject fx) {
        var result = new HashSet<String>();
        if (fx == null || !fx.has("layers")) {
            return result;
        }
        for (JsonElement element : fx.getAsJsonArray("layers")) {
            JsonObject layer = element.getAsJsonObject();
            String type = layer.has("type") ? layer.get("type").getAsString() : "";
            if (!FALLING.contains(type) || (layer.has("off") && layer.get("off").getAsBoolean())) {
                continue;
            }
            for (String key : new String[]{"at", "from"}) {
                if (layer.has(key)) {
                    result.add(layer.get(key).getAsString());
                }
            }
        }
        return result;
    }

    private void changed(String name) {
        JsonObject anchors = selectedAnchors();
        if (name.equals(sourceName(anchors))) {
            JsonElement source = anchors.get(name);
            for (String derived : DERIVED) {
                if (anchors.has(derived)) {
                    JsonElement value = autoAll(derived, source, null, null);
                    if (value != null) {
                        anchors.add(derived, value);
                    }
                }
            }
        }
        session.dirty = true;
    }

    private void addPoint(BlockHitResult hit) {
        ResourceLocation id = selectedId();
        if (id == null || adding == null) {
            adding = null;
            return;
        }
        if (undersidesOf(base(id)).contains(adding) && hit.getDirection() != Direction.DOWN) {
            return;
        }
        var origin = model.layout().getCharGrid().getControllerPos();
        Vec3 at = snap(hit);
        appendPoint(selectedAnchors(), adding, point(at.x + origin.getX(), at.y + origin.getY(), at.z + origin.getZ()));
        changed(adding);
        if (!hasControlDown()) {
            adding = null;
        }
    }

    private void removeLast(String name) {
        JsonObject anchors = selectedAnchors();
        List<double[]> points = pointsOf(anchors.get(name));
        if (points.size() <= 1) {
            return;
        }
        points.remove(points.size() - 1);
        anchors.add(name, pointsJson(points));
        changed(name);
    }

    private void symmetry(String name, int mode) {
        JsonObject anchors = selectedAnchors();
        List<double[]> points = pointsOf(anchors.get(name));
        if (points.isEmpty()) {
            return;
        }
        var origin = model.layout().getCharGrid().getControllerPos();
        int minX = 0, minZ = 0, maxX = 0, maxZ = 0;
        for (PositionedLayoutPiece piece : model.layout().getPositionedPieces()) {
            minX = Math.min(minX, piece.pos().getX());
            maxX = Math.max(maxX, piece.pos().getX());
            minZ = Math.min(minZ, piece.pos().getZ());
            maxZ = Math.max(maxZ, piece.pos().getZ());
        }
        double cx = (minX + maxX + 1) / 2.0 + origin.getX();
        double cz = (minZ + maxZ + 1) / 2.0 + origin.getZ();
        var result = new ArrayList<>(points);
        for (double[] p : points) {
            double dx = p[0] - cx;
            double dz = p[2] - cz;
            double[][] copies = switch (mode) {
                case 0 -> new double[][]{{cx - dz, p[1], cz + dx}, {cx - dx, p[1], cz - dz}, {cx + dz, p[1], cz - dx}};
                case 1 -> new double[][]{{cx - dx, p[1], p[2]}};
                default -> new double[][]{{p[0], p[1], cz - dz}};
            };
            for (double[] copy : copies) {
                if (!contains(result, copy)) {
                    result.add(copy);
                }
            }
        }
        anchors.add(name, pointsJson(result));
        changed(name);
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
        adding = null;
        held = null;
        firstHit = null;
        steps.clear();
        undersides.clear();
        for (String key : pending.keySet().toArray(new String[0])) {
            pending.remove(key);
        }
        var seen = new LinkedHashMap<String, String>();
        for (JsonElement element : presets.get(preset).getAsJsonArray("layers")) {
            JsonObject layer = element.getAsJsonObject();
            String type = layer.has("type") ? layer.get("type").getAsString() : "";
            boolean area = "glow".equals(type) && !(layer.has("mode") && layer.get("mode").getAsString().equals("halo"));
            for (String key : new String[]{"at", "from"}) {
                if (FALLING.contains(type) && layer.has(key) && !(layer.has("off") && layer.get("off").getAsBoolean())) {
                    undersides.add(layer.get(key).getAsString());
                }
            }
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
            if (step[0].equals(held)) {
                return step;
            }
        }
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
            if (undersides.contains(step[0]) && hit.getDirection() != Direction.DOWN) {
                return;
            }
            Vec3 at = snap(hit);
            if (firstHit == null) {
                firstHit = hit;
            }
            faces.put(step[0], hit.getDirection());
            appendPoint(pending, step[0], point(at.x + origin.getX(), at.y + origin.getY(), at.z + origin.getZ()));
            held = hasControlDown() ? step[0] : null;
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
        held = null;
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
                JsonElement value = autoAll(step[0], source, face, firstHit == null ? null : firstHit.getBlockPos());
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

    private static Vec3 snap(BlockHitResult hit) {
        Vec3 at = hit.getLocation();
        double x = Math.round(at.x * 16) / 16.0;
        double y = Math.round(at.y * 16) / 16.0;
        double z = Math.round(at.z * 16) / 16.0;
        Direction.Axis axis = hit.getDirection().getAxis();
        BlockPos block = hit.getBlockPos();
        return new Vec3(axis == Direction.Axis.X ? x : inset(x, block.getX()),
                axis == Direction.Axis.Y ? y : inset(y, block.getY()),
                axis == Direction.Axis.Z ? z : inset(z, block.getZ()));
    }

    private static double inset(double value, int block) {
        return Math.max(block + 1 / 16.0, Math.min(block + 15 / 16.0, value));
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

    private Direction faceOf(double[] p) {
        var origin = model.layout().getCharGrid().getControllerPos();
        var world = view.previewWorld();
        double x = p[0] - origin.getX();
        double y = p[1] - origin.getY();
        double z = p[2] - origin.getZ();
        int by = (int) Math.floor(y);
        if (x == Math.floor(x)) {
            int bz = (int) Math.floor(z);
            boolean west = !world.getBlockState(new BlockPos((int) x - 1, by, bz)).isAir();
            boolean east = !world.getBlockState(new BlockPos((int) x, by, bz)).isAir();
            if (west != east) {
                return west ? Direction.EAST : Direction.WEST;
            }
        }
        if (z == Math.floor(z)) {
            int bx = (int) Math.floor(x);
            boolean north = !world.getBlockState(new BlockPos(bx, by, (int) z - 1)).isAir();
            boolean south = !world.getBlockState(new BlockPos(bx, by, (int) z)).isAir();
            if (north != south) {
                return north ? Direction.SOUTH : Direction.NORTH;
            }
        }
        return Direction.DOWN;
    }

    @Nullable
    private JsonElement autoAll(String name, @Nullable JsonElement source, @Nullable Direction face, @Nullable BlockPos clicked) {
        List<double[]> points = pointsOf(source);
        if (points.isEmpty()) {
            return auto(name, null, face == null ? Direction.DOWN : face, clicked);
        }
        if (points.size() == 1 || !DERIVED.contains(name)) {
            double[] p = points.get(0);
            return auto(name, point(p[0], p[1], p[2]), face == null ? faceOf(p) : face, clicked);
        }
        var list = new JsonArray();
        Double same = null;
        boolean differ = false;
        for (double[] p : points) {
            JsonElement value = auto(name, point(p[0], p[1], p[2]), faceOf(p), null);
            if (value == null) {
                return null;
            }
            if (name.equals("floor")) {
                double h = value.getAsDouble();
                differ |= same != null && Math.abs(same - h) > 1e-6;
                same = same == null ? h : same;
                var a = new JsonArray();
                a.add(p[0]);
                a.add(h);
                a.add(p[2]);
                list.add(a);
            } else {
                list.add(value);
            }
        }
        return name.equals("floor") && !differ ? new JsonPrimitive(same) : list;
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
            var hit = (shape.isEmpty() ? Shapes.block() : shape).clip(new Vec3(fx, Math.min(rel - 0.001, y + 1.01), fz), new Vec3(fx, y, fz), pos);
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
        String sourceName = sourceName(anchors);
        JsonElement source = sourceName == null ? null : anchors.get(sourceName);
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
                JsonElement value = AUTO.contains(anchor) ? autoAll(anchor, source, null, null) : source;
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

    private void renderGhost(PoseStack pose, MultiBufferSource buffers) {
        String[] step = placing == null ? null : currentStep();
        String name = step != null ? step[0] : adding;
        if (hover == null || name == null || (step != null && step[1].equals("area"))) {
            return;
        }
        Vec3 at = snap(hover);
        boolean underside = step != null ? undersides.contains(name) : selectedId() != null && undersidesOf(base(selectedId())).contains(name);
        boolean ok = !underside || hover.getDirection() == Direction.DOWN;
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(pose, lines, new AABB(at.x - 0.06, at.y - 0.06, at.z - 0.06, at.x + 0.06, at.y + 0.06, at.z + 0.06),
                ok ? 0.25f : 1f, ok ? 1f : 0.2f, 0.2f, 1f);
        if (!underside || !ok) {
            return;
        }
        if (!at.equals(ghostAt)) {
            var origin = model.layout().getCharGrid().getControllerPos();
            JsonElement floor = auto("floor", point(at.x + origin.getX(), at.y + origin.getY(), at.z + origin.getZ()), Direction.DOWN, null);
            ghostFloor = floor == null ? at.y - 4 : floor.getAsDouble() - origin.getY();
            ghostAt = at;
        }
        boolean lands = at.y - ghostFloor > 0.05;
        float r = lands ? 0.25f : 1f;
        float g = lands ? 1f : 0.2f;
        LevelRenderer.renderLineBox(pose, lines, new AABB(at.x - 0.01, ghostFloor, at.z - 0.01, at.x + 0.01, at.y, at.z + 0.01), r, g, 0.2f, 1f);
        LevelRenderer.renderLineBox(pose, lines, new AABB(at.x - 0.2, ghostFloor, at.z - 0.2, at.x + 0.2, ghostFloor + 0.01, at.z + 0.2), r, g, 0.2f, 1f);
    }

    @Nullable
    private String problem(int layer) {
        return preview == null || session.selected < 0 ? null : preview.problem(session.selected, layer);
    }

    private void renderSpots(PoseStack pose, MultiBufferSource buffers) {
        if (spots.isEmpty() || session.selected < 0 || session.selected >= session.effects.size()) {
            return;
        }
        var origin = model.layout().getCharGrid().getControllerPos();
        JsonObject anchors = selectedAnchors();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (int i = 0; i < spots.size(); i++) {
            int color = SPOT_COLORS[i % SPOT_COLORS.length];
            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;
            double size = spots.get(i).equals(adding) ? 0.09 : 0.05;
            for (double[] p : pointsOf(anchors.get(spots.get(i)))) {
                double x = p[0] - origin.getX();
                double y = p[1] - origin.getY();
                double z = p[2] - origin.getZ();
                LevelRenderer.renderLineBox(pose, lines, new AABB(x - size, y - size, z - size, x + size, y + size, z + size), r, g, b, 1f);
            }
        }
    }

    private void renderEffects(PoseStack pose, MultiBufferSource buffers, Matrix4f modelView) {
        renderGhost(pose, buffers);
        renderSpots(pose, buffers);
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
        FxRenderer.renderPreview(preview, view.previewWorld(), pose, buffers, camera, left, up, look, !session.idle, session.fast ? 4 : 1, partialTick);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);
        if (view == null) {
            return;
        }
        this.partialTick = partialTick;
        move();
        hover = (placing != null || adding != null) && viewport.contains(mouseX, mouseY) ? view.pick(mouseX, mouseY) : null;
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
        boolean underside = step != null ? undersides.contains(step[0])
                : adding != null && selectedId() != null && undersidesOf(base(selectedId())).contains(adding);
        Component hint;
        if (step != null) {
            String kind = step[0].equals(held) ? "held" : underside ? "underside" : step[1];
            hint = Component.translatable("gui.mm.effect_studio.click." + kind, pretty(placing), step[0]);
        } else if (adding != null) {
            hint = Component.translatable(underside ? "gui.mm.effect_studio.click.add_underside" : "gui.mm.effect_studio.click.add", adding);
        } else {
            hint = Component.translatable("gui.mm.effect_studio.controls");
        }
        boolean wrong = underside && hover != null && hover.getDirection() != Direction.DOWN;
        gfx.drawCenteredString(font, hint, viewport.x() + viewport.w() / 2, viewport.y() + 4,
                wrong ? 0xFF5555 : step != null || adding != null ? 0xFFFF55 : 0xA0A0A0);
        if (!spots.isEmpty()) {
            gfx.drawString(font, Component.translatable("gui.mm.effect_studio.spots"), 6, spotsY - 11, 0xFFFFFF);
            JsonObject anchors = selectedAnchors();
            for (int i = 0; i < spots.size(); i++) {
                String spot = spots.get(i);
                String text = font.plainSubstrByWidth(spot, 30) + " " + pointsOf(anchors.get(spot)).size();
                gfx.drawString(font, text, 6, spotsY + i * 18 + 4, SPOT_COLORS[i % SPOT_COLORS.length]);
            }
        }
        String tip = null;
        for (int[] dot : dots) {
            String problem = problem(dot[2]);
            int color = problem == null ? 0xFF40E040 : NOTES.contains(problem) ? 0xFFE0C040 : 0xFFE04040;
            gfx.fill(dot[0] + 57, dot[1] + 2, dot[0] + 62, dot[1] + 7, 0xFF000000);
            gfx.fill(dot[0] + 58, dot[1] + 3, dot[0] + 61, dot[1] + 6, color);
            if (problem != null && mouseX >= dot[0] && mouseX < dot[0] + 64 && mouseY >= dot[1] && mouseY < dot[1] + 16) {
                tip = problem;
            }
        }
        if (tip != null) {
            gfx.renderTooltip(font, font.split(Component.translatable("gui.mm.effect_studio.health." + tip), 180), mouseX, mouseY);
        }
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
            } else if (adding != null) {
                if (hit != null) {
                    addPoint(hit);
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
        if (keyCode == 256 && (placing != null || adding != null)) {
            placing = null;
            adding = null;
            held = null;
            steps.clear();
            return true;
        }
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && (held != null || adding != null)) {
            adding = null;
            if (held != null) {
                held = null;
                if (placing != null && currentStep() == null) {
                    finish();
                }
            }
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

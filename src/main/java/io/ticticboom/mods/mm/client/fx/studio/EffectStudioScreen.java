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
import net.minecraft.client.gui.components.EditBox;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EffectStudioScreen extends Screen {
    private static final String[] COLORS = {"#c01818", "#ff7a1a", "#ffd040", "#3aff7a", "#40e0ff", "#3f76e4", "#a040ff", "#ff60c0", "#ffffff", "#808080"};
    private static final Set<String> AMOUNT = Set.of("rate", "max", "chance");
    private static final Set<String> SIZE = Set.of("size", "radius", "width", "drop_size");
    private static final Set<String> SPEED = Set.of("velocity", "scroll", "speed", "start_speed", "pulse_speed", "jitter_rate", "frame_rate");
    private static final int PANEL = 130;
    private static final Set<String> AUTO = Set.of("floor", "landing", "sky", "block", "below", "seep_top");
    private static final Set<String> FALLING = Set.of("drip", "stream", "pool", "seep");
    private static final Set<String> NOTES = Set.of("drip_working", "hidden_now");
    private static final Set<String> DERIVED = Set.of("floor", "landing", "sky");
    private static final Set<String> VERTICAL = Set.of("floor", "seep_top");
    private static final int[] SPOT_COLORS = {0xFF40E0FF, 0xFFFFD040, 0xFFFF60C0, 0xFF3AFF7A, 0xFFFF7A1A, 0xFFA040FF};

    private final EffectStudio.Session session;
    private final Map<String, JsonObject> presets = EffectStudio.library("mm_fx_presets");
    private final Map<String, JsonObject> parts = EffectStudio.library("mm_fx_parts");
    private final List<String> partNames = new ArrayList<>(parts.keySet());
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
    private int spotScroll;
    private int spotScrollMax;
    private static final String MINE = "my/";
    private static final String PART = "part/";
    private boolean dragFromLibrary;
    private int placingStarts;
    private Map<String, JsonObject> mine = EffectStudio.userPresets();
    private String search = "";
    private int libScroll;
    private int libScrollMax;
    @Nullable
    private String renaming;
    @Nullable
    private String confirmDelete;
    private boolean saving;
    @Nullable
    private EditBox searchBox;
    @Nullable
    private EditBox renameBox;
    @Nullable
    private EditBox presetNameBox;
    @Nullable
    private EditBox focusLater;
    private boolean dropdown;
    private int dropScroll;
    private int savingFor = -1;
    private final List<Object[]> sideHits = new ArrayList<>();
    private String renameText = "";
    @Nullable
    private String deleteArmed;
    private final List<Object[]> rowLabels = new ArrayList<>();
    @Nullable
    private String handleName;
    private int handleIndex;
    @Nullable
    private Handle hovered;
    private boolean dragging;
    private boolean dragMoved;
    private double dragY;
    private double lastMouseY;
    @Nullable
    private Handle menu;
    private int menuX;
    private int menuY;
    @Nullable
    private EditBox coords;
    @Nullable
    private Button undoButton;
    @Nullable
    private Button redoButton;
    private boolean swallowRelease;
    @Nullable
    private Handle linkTarget;
    private int linkFrom = -1;
    @Nullable
    private String pickingBlocks;
    private static final double ARROW = 0.6;
    private int arrowLayer = -1;
    private boolean arrowMoved;
    private double arrowTipY;
    private double arrowLastMouseY;
    private int pickingFor = -1;
    private final Set<Pick> group = new LinkedHashSet<>();
    private boolean marqueeArmed;
    private boolean marquee;
    private double marqueeX;
    private double marqueeY;
    private boolean facePicking;
    private final List<Face> centreFaces = new ArrayList<>();
    @Nullable
    private Button centreButton;
    @Nullable
    private Button facesButton;

    private record Handle(String name, int index, double x, double y, double z, boolean height) {
    }

    private record Pick(String name, int index) {
    }

    private record Face(BlockPos pos, Direction dir) {
    }

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
        rowLabels.clear();
        focusLater = null;
        coords = null;
        menu = null;
        if (view == null || model != be.getStructure()) {
            model = be.getStructure();
            view = GuiStructureRenderer.ofFormed(model, be.getLevel(), be.getBlockPos(), be.getFormedRotation());
            view.setOverlay(this::renderEffects);
            view.setStudio(true);
        }
        viewport = session.collapsed ? GuiPos.of(6, 20, width - 12, height - 52) : GuiPos.of(PANEL + 12, 20, width - PANEL * 2 - 24, height - 52);

        if (!session.collapsed) {
            buildLibrary();
        }
        if (selectedId() != null) {
            spots.addAll(spotNames(selectedAnchors()));
            if (adding != null && !spots.contains(adding)) {
                adding = null;
            }
            if (handle() == null) {
                deselect();
            } else {
                group.removeIf(pick -> findHandle(pick.name(), pick.index()) == null);
            }
        }
        if (facePicking && (placing != null || adding != null || pickingBlocks != null || selectedId() == null || handle() == null || handle().height() || !group.isEmpty())) {
            facePicking = false;
            centreFaces.clear();
        }
        if (pickingBlocks != null && (selectedId() == null || session.selected != pickingFor || !glowAnchors(base(selectedId())).contains(pickingBlocks))) {
            pickingBlocks = null;
        }
        if (!session.collapsed) {
            buildInspector();
        }

        int by = height - 26;
        addRenderableWidget(Button.builder(Component.translatable(session.idle ? "gui.mm.effect_studio.idle" : "gui.mm.effect_studio.working"), b -> {
            session.idle = !session.idle;
            b.setMessage(Component.translatable(session.idle ? "gui.mm.effect_studio.idle" : "gui.mm.effect_studio.working"));
        }).bounds(viewport.x(), by, 90, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.mm.effect_studio.save"), b -> {
            boolean ok = EffectStudio.save(session);
            minecraft.player.displayClientMessage(Component.translatable(ok ? "gui.mm.effect_studio.saved" : "gui.mm.effect_studio.save_failed"), true);
        }).bounds(viewport.x() + 94, by, 60, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.mm.effect_studio.close"), b -> onClose())
                .bounds(viewport.x() + 158, by, 60, 20).build());
        undoButton = addRenderableWidget(Button.builder(Component.literal("\u21B6"), b -> undo())
                .tooltip(Tooltip.create(Component.translatable("gui.mm.effect_studio.undo")))
                .bounds(viewport.x() + 222, by, 20, 20).build());
        redoButton = addRenderableWidget(Button.builder(Component.literal("\u21B7"), b -> redo())
                .tooltip(Tooltip.create(Component.translatable("gui.mm.effect_studio.redo")))
                .bounds(viewport.x() + 244, by, 20, 20).build());
        updateHistoryButtons();
        addRenderableWidget(Button.builder(Component.literal(session.collapsed ? "\u21D4" : "\u21D5"), b -> togglePanels())
                .tooltip(Tooltip.create(Component.translatable("gui.mm.effect_studio.hide_panels")))
                .bounds(viewport.x() + 268, by, 20, 20).build());
        centreButton = addWidget(Button.builder(Component.translatable("gui.mm.effect_studio.centre"), b -> centreSelected())
                .tooltip(Tooltip.create(Component.translatable("gui.mm.effect_studio.centre_tip")))
                .bounds(viewport.x() + 158, viewport.y() + viewport.h() - 21, 54, 18).build());
        facesButton = addWidget(Button.builder(Component.empty(), b -> facesPressed())
                .tooltip(Tooltip.create(Component.translatable("gui.mm.effect_studio.faces_tip")))
                .bounds(viewport.x() + 216, viewport.y() + viewport.h() - 21, 84, 18).build());
        updateCentreButtons();
        Handle selectedHandle = handle();
        if (selectedHandle != null) {
            coords = new EditBox(font, viewport.x() + 4, viewport.y() + viewport.h() - 20, 150, 16, Component.empty());
            coords.setMaxLength(64);
            coords.setValue(selectedHandle.height() ? format(selectedHandle.y()) : format(selectedHandle.x()) + ", " + format(selectedHandle.y()) + ", " + format(selectedHandle.z()));
            addWidget(coords);
        }
    }

    @Nullable
    private JsonObject preset(String key) {
        if (key.startsWith(PART)) {
            return parts.get(key.substring(PART.length()));
        }
        return key.startsWith(MINE) ? mine.get(key.substring(MINE.length())) : presets.get(key);
    }

    private static String presetName(String key) {
        return key.startsWith(PART) ? key.substring(PART.length()) : key.startsWith(MINE) ? key.substring(MINE.length()) : key;
    }

    private static String presetLabel(String key) {
        return pretty(presetName(key));
    }

    private static boolean anchored(@Nullable JsonObject fx) {
        if (fx == null || !fx.has("layers")) {
            return false;
        }
        for (JsonElement element : fx.getAsJsonArray("layers")) {
            JsonObject layer = element.getAsJsonObject();
            if (layer.has("at") || layer.has("from") || layer.has("to")) {
                return true;
            }
        }
        return false;
    }

    private void togglePanels() {
        session.collapsed = !session.collapsed;
        dropdown = false;
        rebuildWidgets();
        setFocused(null);
    }

    private boolean matches(String name) {
        String needle = search.trim().toLowerCase(java.util.Locale.ROOT);
        return needle.isEmpty() || name.replace('_', ' ').toLowerCase(java.util.Locale.ROOT).contains(needle);
    }

    private void buildLibrary() {
        int px = 6;
        searchBox = new EditBox(font, px, 20, PANEL, 16, Component.empty());
        searchBox.setMaxLength(40);
        searchBox.setValue(search);
        searchBox.setHint(Component.translatable("gui.mm.effect_studio.library.search").withStyle(ChatFormatting.DARK_GRAY));
        searchBox.setResponder(value -> {
            search = value;
            libScroll = 0;
            rebuildWidgets();
            setFocused(searchBox);
        });
        addRenderableWidget(searchBox);
        int top = 42;
        var rows = new ArrayList<java.util.function.IntConsumer>();
        rows.add(ry -> rowLabels.add(new Object[]{px, ry + 3, Component.translatable("gui.mm.effect_studio.library.builtin").getString(), 0xFFA0A0A0}));
        for (String name : presets.keySet()) {
            if (matches(name)) {
                rows.add(ry -> smallButton(px, ry, PANEL, Component.literal(font.plainSubstrByWidth(pretty(name), PANEL - 8)), null, () -> startPlacing(name)));
            }
        }
        rows.add(ry -> rowLabels.add(new Object[]{px, ry + 3, Component.translatable("gui.mm.effect_studio.library.mine").getString(), 0xFFA0A0A0}));
        int before = rows.size();
        for (String name : mine.keySet()) {
            if (!matches(name)) {
                continue;
            }
            rows.add(ry -> {
                if (name.equals(renaming)) {
                    renameBox = new EditBox(font, px, ry, PANEL - 16, 14, Component.empty());
                    renameBox.setMaxLength(48);
                    renameBox.setValue(renameText);
                    renameBox.setResponder(value -> renameText = value);
                    addRenderableWidget(renameBox);
                    smallButton(px + PANEL - 14, ry, 14, Component.literal("\u2713"), null, this::applyRename);
                    focusLater = renameBox;
                    return;
                }
                smallButton(px, ry, PANEL - 32, Component.literal(font.plainSubstrByWidth(pretty(name), PANEL - 40)), null, () -> startPlacing(MINE + name));
                smallButtonTip(px + PANEL - 30, ry, 14, Component.literal("\u270E"), "library.rename", () -> {
                    renaming = name;
                    renameText = pretty(name);
                    rebuildWidgets();
                });
                boolean confirm = name.equals(confirmDelete);
                smallButtonTip(px + PANEL - 14, ry, 14, Component.literal(confirm ? "?" : "x").withStyle(confirm ? ChatFormatting.RED : ChatFormatting.WHITE),
                        confirm ? "library.confirm" : "library.delete", () -> {
                            if (name.equals(deleteArmed)) {
                                EffectStudio.deleteUserPreset(name);
                                mine = EffectStudio.userPresets();
                                cancelPlacing(MINE + name);
                                confirmDelete = null;
                            } else {
                                confirmDelete = name;
                            }
                            rebuildWidgets();
                        });
            });
        }
        if (rows.size() == before) {
            rows.add(ry -> rowLabels.add(new Object[]{px + 4, ry + 3, Component.translatable("gui.mm.effect_studio.library.none").getString(), 0xFF707070}));
        }
        rows.add(ry -> rowLabels.add(new Object[]{px, ry + 3, Component.translatable("gui.mm.effect_studio.library.parts").getString(), 0xFFA0A0A0}));
        for (String name : partNames) {
            if (!matches(name)) {
                continue;
            }
            boolean standalone = anchored(parts.get(name));
            rows.add(ry -> {
                boolean canAdd = selectedId() != null;
                if (!standalone) {
                    Button only = addRenderableWidget(Button.builder(Component.literal(font.plainSubstrByWidth(pretty(name), PANEL - 8)), b -> {
                                if (selectedId() != null) {
                                    addPart(selectedId(), name);
                                }
                                setFocused(null);
                            })
                            .tooltip(Tooltip.create(Component.translatable("gui.mm.effect_studio.library.add_only")))
                            .bounds(px, ry, PANEL, 14).build());
                    only.active = canAdd;
                    return;
                }
                smallButton(px, ry, canAdd ? PANEL - 16 : PANEL, Component.literal(font.plainSubstrByWidth(pretty(name), PANEL - 24)), null,
                        () -> startPlacing(PART + name));
                if (canAdd) {
                    smallButtonTip(px + PANEL - 14, ry, 14, Component.literal("+"), "library.add_part", () -> addPart(selectedId(), name));
                }
            });
        }
        int visible = Math.max(1, (height - 8 - top) / 16);
        libScrollMax = Math.max(0, rows.size() - visible);
        libScroll = Math.max(0, Math.min(libScroll, libScrollMax));
        for (int r = libScroll; r < rows.size() && r < libScroll + visible; r++) {
            rows.get(r).accept(top + (r - libScroll) * 16);
        }
    }

    private void cancelPlacing(String key) {
        if (key.equals(placing)) {
            placing = null;
            held = null;
            steps.clear();
        }
    }

    @Override
    protected void setInitialFocus() {
    }

    private void applyRename() {
        if (renaming == null || renameBox == null) {
            return;
        }
        String name = EffectStudio.presetName(renameBox.getValue());
        if (!name.isEmpty() && !name.equals(renaming)) {
            if (!mine.containsKey(name) && EffectStudio.renameUserPreset(renaming, name)) {
                cancelPlacing(MINE + renaming);
                mine = EffectStudio.userPresets();
            } else {
                minecraft.player.displayClientMessage(Component.translatable("gui.mm.effect_studio.preset_failed"), true);
            }
        }
        renaming = null;
        rebuildWidgets();
        setFocused(null);
    }

    private void savePreset() {
        ResourceLocation id = selectedId();
        if (id == null || presetNameBox == null) {
            return;
        }
        String name = EffectStudio.presetName(presetNameBox.getValue());
        JsonObject current = session.edited.getOrDefault(id, FxManager.source(id));
        if (name.isEmpty() || current == null) {
            return;
        }
        JsonObject json = current.deepCopy();
        json.addProperty("name", name);
        if (EffectStudio.saveUserPreset(name, json)) {
            cancelPlacing(MINE + name);
            mine = EffectStudio.userPresets();
            minecraft.player.displayClientMessage(Component.translatable("gui.mm.effect_studio.preset_saved", pretty(name)), true);
        } else {
            minecraft.player.displayClientMessage(Component.translatable("gui.mm.effect_studio.preset_failed"), true);
        }
        saving = false;
        rebuildWidgets();
        setFocused(null);
    }

    private void buildInspector() {
        int px = width - PANEL - 6;
        ResourceLocation id = selectedId();
        String current = id != null ? label(session.effects.get(session.selected))
                : Component.translatable(session.effects.isEmpty() ? "gui.mm.effect_studio.dropdown.none" : "gui.mm.effect_studio.dropdown.pick",
                session.effects.size()).getString();
        addRenderableWidget(Button.builder(Component.literal(font.plainSubstrByWidth(current, PANEL - 20) + " \u25BC"), b -> {
            dropdown = !session.effects.isEmpty() && !dropdown;
            dropScroll = Math.max(0, session.selected - 3);
        }).bounds(px, 20, PANEL, 18).build());
        if (saving && savingFor != session.selected) {
            saving = false;
            presetNameBox = null;
        }
        if (id == null) {
            presetNameBox = null;
            return;
        }
        if (saving) {
            String suggested = presetNameBox != null ? presetNameBox.getValue() : EffectStudio.presetName(label(session.effects.get(session.selected)));
            presetNameBox = new EditBox(font, px, 41, PANEL - 46, 16, Component.empty());
            presetNameBox.setMaxLength(48);
            presetNameBox.setValue(suggested);
            presetNameBox.setHint(Component.translatable("gui.mm.effect_studio.preset_name").withStyle(ChatFormatting.DARK_GRAY));
            addRenderableWidget(presetNameBox);
            Button saveButton = addRenderableWidget(Button.builder(Component.empty(), b -> savePreset()).bounds(px + PANEL - 44, 40, 44, 18).build());
            java.util.function.Consumer<String> label = value -> saveButton.setMessage(Component.translatable(
                    mine.containsKey(EffectStudio.presetName(value)) ? "gui.mm.effect_studio.preset_replace" : "gui.mm.effect_studio.preset_save"));
            label.accept(suggested);
            presetNameBox.setResponder(label);
            if (focusLater == null) {
                focusLater = presetNameBox;
            }
        } else {
            presetNameBox = null;
            addRenderableWidget(Button.builder(Component.translatable("gui.mm.effect_studio.save_preset"), b -> {
                saving = true;
                savingFor = session.selected;
                rebuildWidgets();
            }).bounds(px, 40, PANEL - 48, 18).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.mm.effect_studio.duplicate"), b -> duplicate(id))
                    .tooltip(Tooltip.create(Component.translatable("gui.mm.effect_studio.duplicate_tip")))
                    .bounds(px + PANEL - 46, 40, 22, 18).build());
            addRenderableWidget(Button.builder(Component.literal("\u2715"), b -> {
                remember();
                session.edited.remove(id);
                session.effects.remove(session.selected);
                session.selected = -1;
                session.dirty = true;
                rebuildWidgets();
            }).tooltip(Tooltip.create(Component.translatable("gui.mm.effect_studio.remove_tip")))
                    .bounds(px + PANEL - 22, 40, 22, 18).build());
        }
        String[] tabs = {"spots", "layers", "look"};
        int tabWidth = (PANEL - 4) / 3;
        for (int t = 0; t < tabs.length; t++) {
            int index = t;
            Button tab = addRenderableWidget(Button.builder(Component.translatable("gui.mm.effect_studio.tab." + tabs[t]), b -> {
                session.tab = index;
                rebuildWidgets();
            }).bounds(px + t * (tabWidth + 2), 62, tabWidth, 16).build());
            tab.active = session.tab != t;
        }
        int y = 84;
        if (session.tab == 0) {
            spotsY = y;
            buildSpotRows(px, y);
        } else if (session.tab == 1) {
            JsonObject base = base(id);
            if (base != null && base.has("layers")) {
                JsonArray layers = base.getAsJsonArray("layers");
                int live = 0;
                for (int i = 0; i < layers.size(); i++) {
                    JsonObject layer = layers.get(i).getAsJsonObject();
                    boolean off = layer.has("off") && layer.get("off").getAsBoolean();
                    if (!off) {
                        dots.add(new int[]{px + (i % 2) * 66, y + (i / 2) * 18, live++});
                    }
                    String name = layer.has("name") ? layer.get("name").getAsString() : pretty(layer.has("type") ? layer.get("type").getAsString() : "?");
                    addRenderableWidget(Button.builder(Component.literal(name).withStyle(off ? ChatFormatting.DARK_GRAY : ChatFormatting.GREEN), b -> {
                        remember();
                        setOff(layer, !off);
                        if (!rescale(id)) {
                            setOff(layer, off);
                            rescale(id);
                        }
                        rebuildWidgets();
                    }).bounds(px + (i % 2) * 66, y + (i / 2) * 18, 64, 16).build());
                }
                y += (layers.size() + 1) / 2 * 18 + 6;
            }
            addRenderableWidget(Button.builder(Component.translatable("gui.mm.effect_studio.advanced"), b -> {
                remember();
                session.originals.remove(id);
                session.sliders.remove(id);
                session.colors.remove(id);
                minecraft.setScreen(new EffectLayersScreen(session, this, id));
            }).bounds(px, y, PANEL, 18).build());
        } else {
            double[] values = sliderValues(id);
            String[] modes = {"both", "idle", "working"};
            int modeWidth = (PANEL - 4) / 3;
            for (int m = 0; m < 3; m++) {
                int mode = m;
                Button modeButton = addRenderableWidget(Button.builder(Component.translatable("gui.mm.effect_studio.mode." + modes[m]), b -> {
                    session.sliderMode = mode;
                    rebuildWidgets();
                }).tooltip(Tooltip.create(Component.translatable("gui.mm.effect_studio.mode." + modes[m] + "_tip")))
                        .bounds(px + m * (modeWidth + 2), y, modeWidth, 14).build());
                modeButton.active = session.sliderMode != m;
            }
            y += 18;
            String[] names = {"amount", "size", "speed"};
            int count = session.sliderMode == 0 ? 3 : 2;
            for (int i = 0; i < count; i++) {
                addRenderableWidget(new Slider(px, y, names[i], id, values, session.sliderMode * 3 + i));
                y += 22;
            }
            if (count == 2) {
                y += 22;
            }
            addRenderableWidget(Button.builder(Component.translatable("gui.mm.effect_studio.reroll"), b -> reroll(id))
                    .tooltip(Tooltip.create(Component.translatable("gui.mm.effect_studio.reroll_tip")))
                    .bounds(px, y, PANEL, 18).build());
            y += 22;
            colorsY = y;
            for (int i = 0; i < COLORS.length; i++) {
                String color = COLORS[i];
                addRenderableWidget(Button.builder(Component.empty(), b -> {
                    remember();
                    session.colors.put(id, color);
                    rescale(id);
                }).bounds(px + (i % 5) * 26, y + (i / 5) * 18, 24, 16).build());
            }
            JsonObject seep = firstSeep(base(id));
            if (seep != null) {
                int sy = y + 42;
                rowLabels.add(new Object[]{px, sy, Component.translatable("gui.mm.effect_studio.seep.sides").getString(), 0xFFFFFFFF});
                sy += 11;
                Set<String> chosen = seepFaces(seep);
                String[] sides = {"north", "east", "south", "west"};
                for (int i = 0; i < sides.length; i++) {
                    String side = sides[i];
                    boolean on = chosen != null && chosen.contains(side);
                    smallButtonTip(px + i * 22, sy, 20, Component.literal(side.substring(0, 1).toUpperCase(java.util.Locale.ROOT))
                            .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY), "seep." + side, () -> toggleSeepFace(id, side, -1));
                }
                Button any = addRenderableWidget(Button.builder(Component.translatable("gui.mm.effect_studio.seep.any"), b -> setSeepFaces(id, null))
                        .tooltip(Tooltip.create(Component.translatable("gui.mm.effect_studio.seep.any_tip")))
                        .bounds(px + 90, sy, PANEL - 90, 14).build());
                any.active = chosen != null || seep.has("point_faces");
                sy += 18;
                JsonElement rawStreams = seep.get("streams");
                int streams = rawStreams != null && rawStreams.isJsonPrimitive() && rawStreams.getAsJsonPrimitive().isNumber()
                        ? Math.max(1, Math.min(4, rawStreams.getAsInt())) : 1;
                rowLabels.add(new Object[]{px, sy + 3, Component.translatable("gui.mm.effect_studio.seep.streams", streams).getString(), 0xFFFFFFFF});
                if (streams > 1) {
                    smallButton(px + PANEL - 30, sy, 14, Component.literal("-"), null,
                            () -> setSeep(id, "streams", streams > 2 ? new JsonPrimitive(streams - 1) : null));
                }
                if (streams < 4) {
                    smallButton(px + PANEL - 14, sy, 14, Component.literal("+"), null,
                            () -> setSeep(id, "streams", new JsonPrimitive(streams + 1)));
                }
            }
        }
    }

    @Nullable
    private static JsonObject firstSeep(@Nullable JsonObject fx) {
        if (fx == null || !fx.has("layers")) {
            return null;
        }
        for (JsonElement element : fx.getAsJsonArray("layers")) {
            JsonObject layer = element.getAsJsonObject();
            if ("seep".equals(layer.has("type") ? layer.get("type").getAsString() : "") && !(layer.has("off") && layer.get("off").getAsBoolean())) {
                return layer;
            }
        }
        return null;
    }

    @Nullable
    private static Set<String> seepFaces(JsonObject seep) {
        if (!seep.has("faces")) {
            return null;
        }
        var result = new HashSet<String>();
        JsonElement faces = seep.get("faces");
        for (JsonElement element : faces.isJsonArray() ? faces.getAsJsonArray().asList() : List.of(faces)) {
            if (element.isJsonPrimitive()) {
                result.add(element.getAsString().toLowerCase(java.util.Locale.ROOT));
            }
        }
        return result;
    }

    private void setSeep(ResourceLocation id, String key, @Nullable JsonElement value) {
        JsonObject fx = base(id);
        if (fx == null) {
            return;
        }
        remember();
        for (JsonElement element : fx.getAsJsonArray("layers")) {
            JsonObject layer = element.getAsJsonObject();
            if (!"seep".equals(layer.has("type") ? layer.get("type").getAsString() : "")) {
                continue;
            }
            if (value == null) {
                layer.remove(key);
            } else {
                layer.add(key, value.deepCopy());
            }
        }
        rescale(id);
        rebuildWidgets();
    }

    @Nullable
    private static Set<String> pointFaces(JsonObject seep, int point) {
        if (point < 0 || !seep.has("point_faces") || !seep.get("point_faces").isJsonArray()) {
            return null;
        }
        JsonArray list = seep.getAsJsonArray("point_faces");
        if (point >= list.size() || list.get(point).isJsonNull()) {
            return null;
        }
        var holder = new JsonObject();
        holder.add("faces", list.get(point));
        return seepFaces(holder);
    }

    private void setSeepFaces(ResourceLocation id, @Nullable JsonElement faces) {
        JsonObject fx = base(id);
        if (fx == null) {
            return;
        }
        remember();
        for (JsonElement element : fx.getAsJsonArray("layers")) {
            JsonObject layer = element.getAsJsonObject();
            if (!"seep".equals(layer.has("type") ? layer.get("type").getAsString() : "")) {
                continue;
            }
            layer.remove("point_faces");
            if (faces == null) {
                layer.remove("faces");
            } else {
                layer.add("faces", faces.deepCopy());
            }
        }
        rescale(id);
        rebuildWidgets();
    }

    private void toggleSeepFace(ResourceLocation id, String side, int point) {
        JsonObject seep = firstSeep(base(id));
        if (seep == null) {
            return;
        }
        Set<String> own = pointFaces(seep, point);
        Set<String> chosen = own != null ? own : seepFaces(seep);
        var next = new java.util.LinkedHashSet<String>();
        for (String s : new String[]{"north", "east", "south", "west"}) {
            boolean on = chosen != null && chosen.contains(s);
            if (s.equals(side) ? !on : on) {
                next.add(s);
            }
        }
        var list = new JsonArray();
        next.forEach(list::add);
        if (point < 0) {
            setSeepFaces(id, next.isEmpty() ? null : list);
            return;
        }
        JsonObject fx = base(id);
        String at = seep.has("at") && seep.get("at").isJsonPrimitive() ? seep.get("at").getAsString() : "drips";
        remember();
        for (JsonElement element : fx.getAsJsonArray("layers")) {
            JsonObject layer = element.getAsJsonObject();
            String layerAt = layer.has("at") && layer.get("at").isJsonPrimitive() ? layer.get("at").getAsString() : "drips";
            if (!"seep".equals(layer.has("type") ? layer.get("type").getAsString() : "") || !layerAt.equals(at)) {
                continue;
            }
            JsonArray perPoint = layer.has("point_faces") && layer.get("point_faces").isJsonArray() ? layer.getAsJsonArray("point_faces") : new JsonArray();
            while (perPoint.size() <= point) {
                perPoint.add(com.google.gson.JsonNull.INSTANCE);
            }
            perPoint.set(point, list.deepCopy());
            layer.add("point_faces", perPoint);
        }
        rescale(id);
        rebuildWidgets();
    }

    private void dropSeepPoint(String anchor, int index) {
        ResourceLocation id = selectedId();
        JsonObject fx = id == null ? null : base(id);
        if (fx == null || !fx.has("layers")) {
            return;
        }
        boolean touched = false;
        for (JsonElement element : fx.getAsJsonArray("layers")) {
            JsonObject layer = element.getAsJsonObject();
            String at = layer.has("at") && layer.get("at").isJsonPrimitive() ? layer.get("at").getAsString() : "drips";
            if (!"seep".equals(layer.has("type") ? layer.get("type").getAsString() : "") || !at.equals(anchor)
                    || !layer.has("point_faces") || !layer.get("point_faces").isJsonArray()) {
                continue;
            }
            JsonArray perPoint = layer.getAsJsonArray("point_faces");
            if (index < perPoint.size()) {
                perPoint.remove(index);
                touched = true;
            }
        }
        if (touched) {
            rescale(id);
        }
    }

    private List<double[]> arrows() {
        var result = new ArrayList<double[]>();
        if (session.tab != 2 || placing != null || adding != null || pickingBlocks != null || facePicking) {
            return result;
        }
        ResourceLocation id = selectedId();
        JsonObject fx = id == null ? null : base(id);
        if (fx == null || !fx.has("layers")) {
            return result;
        }
        JsonObject anchors = selectedAnchors();
        JsonArray layers = fx.getAsJsonArray("layers");
        for (int i = 0; i < layers.size(); i++) {
            JsonObject layer = layers.get(i).getAsJsonObject();
            if (!"emitter".equals(layer.has("type") ? layer.get("type").getAsString() : "") || (layer.has("off") && layer.get("off").getAsBoolean())) {
                continue;
            }
            String at = layer.has("at") ? layer.get("at").getAsString() : "emit";
            List<double[]> points = pointsOf(anchors.get(at));
            if (points.isEmpty()) {
                continue;
            }
            double[] p = points.get(0);
            double[] v = velocityOf(layer);
            double len = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
            double[] tip = len < 1e-4 ? new double[]{p[0], p[1] + 0.3, p[2]} : new double[]{p[0] + v[0] * ARROW, p[1] + v[1] * ARROW, p[2] + v[2] * ARROW};
            result.add(new double[]{i, p[0], p[1], p[2], tip[0], tip[1], tip[2]});
        }
        return result;
    }

    private static double[] velocityOf(JsonObject layer) {
        if (layer.has("velocity") && layer.get("velocity").isJsonArray() && layer.getAsJsonArray("velocity").size() == 3) {
            JsonArray v = layer.getAsJsonArray("velocity");
            return new double[]{v.get(0).getAsDouble(), v.get(1).getAsDouble(), v.get(2).getAsDouble()};
        }
        return new double[]{0, 0, 0};
    }

    @Nullable
    private double[] arrowAt(double mouseX, double mouseY) {
        if (view == null || !viewport.contains((int) mouseX, (int) mouseY)) {
            return null;
        }
        var origin = model.layout().getCharGrid().getControllerPos();
        double[] best = null;
        double bestDistance = 7 * 7;
        for (double[] a : arrows()) {
            double[] s = view.project(a[4] - origin.getX(), a[5] - origin.getY(), a[6] - origin.getZ());
            if (s == null) {
                continue;
            }
            double d = (s[0] - mouseX) * (s[0] - mouseX) + (s[1] - mouseY) * (s[1] - mouseY);
            if (d < bestDistance) {
                bestDistance = d;
                best = a;
            }
        }
        return best;
    }

    private void dragArrow(double mouseX, double mouseY) {
        ResourceLocation id = selectedId();
        double[] arrow = null;
        for (double[] a : arrows()) {
            if ((int) a[0] == arrowLayer) {
                arrow = a;
            }
        }
        if (id == null || arrow == null) {
            arrowLayer = -1;
            return;
        }
        if (!arrowMoved) {
            if (Math.abs(mouseX - pressX) + Math.abs(mouseY - pressY) < 3) {
                return;
            }
            remember();
            arrowMoved = true;
            arrowTipY = arrow[5];
            arrowLastMouseY = mouseY;
        }
        var origin = model.layout().getCharGrid().getControllerPos();
        double tx = arrow[4];
        double ty = arrow[5];
        double tz = arrow[6];
        if (hasAltDown()) {
            double[] s0 = view.project(tx - origin.getX(), arrowTipY - origin.getY(), tz - origin.getZ());
            double[] s1 = view.project(tx - origin.getX(), arrowTipY + 1 - origin.getY(), tz - origin.getZ());
            if (s0 == null || s1 == null || Math.abs(s1[1] - s0[1]) < 1e-3) {
                return;
            }
            arrowTipY += (mouseY - arrowLastMouseY) / (s1[1] - s0[1]);
            arrowLastMouseY = mouseY;
            ty = arrowTipY;
        } else {
            arrowLastMouseY = mouseY;
            double[] ray = view.ray(mouseX, mouseY);
            double planeY = ty - origin.getY();
            if (ray == null || Math.abs(ray[4]) < 1e-6) {
                return;
            }
            double t = (planeY - ray[1]) / ray[4];
            if (t <= 0) {
                return;
            }
            tx = ray[0] + ray[3] * t + origin.getX();
            tz = ray[2] + ray[5] * t + origin.getZ();
        }
        double dx = tx - arrow[1];
        double dy = ty - arrow[2];
        double dz = tz - arrow[3];
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length > 6) {
            dx *= 6 / length;
            dy *= 6 / length;
            dz *= 6 / length;
        }
        JsonObject layer = base(id).getAsJsonArray("layers").get(arrowLayer).getAsJsonObject();
        var v = new JsonArray();
        v.add(Math.round(dx / ARROW * 100) / 100.0);
        v.add(Math.round(dy / ARROW * 100) / 100.0);
        v.add(Math.round(dz / ARROW * 100) / 100.0);
        layer.add("velocity", v);
        rescale(id);
    }

    private void reroll(ResourceLocation id) {
        JsonObject fx = base(id);
        if (fx == null) {
            return;
        }
        remember();
        fx.addProperty("seed", java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 1_000_000));
        rescale(id);
        rebuildWidgets();
    }

    private void smallButtonTip(int x, int y, int w, Component text, String tipKey, Runnable action) {
        addRenderableWidget(Button.builder(text, b -> action.run())
                .tooltip(Tooltip.create(Component.translatable("gui.mm.effect_studio." + tipKey)))
                .bounds(x, y, w, 14).build());
    }

    private void smallButton(int x, int y, int w, Component text, @Nullable String tip, Runnable action) {
        Button.Builder builder = Button.builder(text, b -> {
            action.run();
            setFocused(null);
        });
        if (tip != null) {
            builder.tooltip(Tooltip.create(Component.translatable("gui.mm.effect_studio.spot." + tip)));
        }
        addRenderableWidget(builder.bounds(x, y, w, 14).build());
    }

    private void buildSpotRows(int px, int top) {
        JsonObject anchors = selectedAnchors();
        JsonObject fx = base(selectedId());
        Set<String> linked = linkedOf(fx);
        var rows = new ArrayList<Runnable[]>();
        int right = px + PANEL;
        for (int i = 0; i < spots.size(); i++) {
            String spot = spots.get(i);
            int color = SPOT_COLORS[i % SPOT_COLORS.length];
            boolean arcs = linked.contains(spot);
            List<double[]> points = pointsOf(anchors.get(spot));
            List<int[]> links = arcs ? linksOf(fx, spot) : List.of();
            int headerIndex = rows.size();
            int rowColor = color;
            rows.add(new Runnable[]{() -> {
                int ry = rowY(headerIndex, top);
                rowLabels.add(new Object[]{px, ry + 3, font.plainSubstrByWidth(pretty(spot), PANEL - 20), rowColor});
                if (!arcs) {
                    smallButton(right - 14, ry, 14, Component.literal("+"), "add", () -> {
                        adding = adding != null && adding.equals(spot) ? null : spot;
                        pickingBlocks = null;
                        linkFrom = -1;
                        deselect();
                        placing = null;
                        steps.clear();
                        rebuildWidgets();
                    });
                }
            }});
            for (int j = 0; j < points.size(); j++) {
                int index = j;
                int rowIndex = rows.size();
                rows.add(new Runnable[]{() -> {
                    int ry = rowY(rowIndex, top);
                    var neighbours = new StringBuilder();
                    for (int[] l : links) {
                        int other = l[0] == index ? l[1] : -1;
                        if (other >= 0) {
                            neighbours.append(neighbours.isEmpty() ? "" : ", ").append(other + 1);
                        }
                    }
                    boolean selected = isSelected(spot, index);
                    boolean from = spot.equals(adding) && index == linkFrom;
                    String text = (selected ? "> " : "") + Component.translatable("gui.mm.effect_studio.point", index + 1).getString()
                            + (arcs ? " \u2192 " + (neighbours.isEmpty() ? "-" : neighbours) : "");
                    int width = PANEL - (arcs ? 32 : 16);
                    smallButton(px, ry, width, Component.literal(font.plainSubstrByWidth(text, width - 6))
                            .withStyle(from ? ChatFormatting.YELLOW : ChatFormatting.WHITE), null, () -> pickRow(spot, index));
                    if (arcs) {
                        smallButton(right - 30, ry, 14, Component.literal("+"), "link", () -> {
                            selectOnly(spot, index);
                            startLinking(spot);
                            rebuildWidgets();
                        });
                    }
                    if (points.size() > 1) {
                        smallButton(right - 14, ry, 14, Component.literal("-"), "remove", () -> {
                            Handle h = findHandle(spot, index);
                            if (h != null) {
                                deleteHandle(h);
                            }
                        });
                    }
                }});
            }
            if (arcs && !links.isEmpty()) {
                int arcsHeader = rows.size();
                rows.add(new Runnable[]{() -> rowLabels.add(new Object[]{px + 4, rowY(arcsHeader, top) + 3,
                        Component.translatable("gui.mm.effect_studio.arcs").getString(), 0xFFA0A0A0})});
                for (int[] l : links) {
                    int a = l[0];
                    int b = l[1];
                    int rowIndex = rows.size();
                    rows.add(new Runnable[]{() -> {
                        int ry = rowY(rowIndex, top);
                        rowLabels.add(new Object[]{px + 8, ry + 3, (a + 1) + " \u2014 " + (b + 1), rowColor});
                        smallButton(right - 14, ry, 14, Component.literal("x"), "unlink_one", () -> removeLink(spot, a, b));
                    }});
                }
            }
        }
        for (String glow : glowAnchors(fx)) {
            JsonElement anchorValue = anchors.get(glow);
            boolean keyed = anchorValue != null && anchorValue.isJsonPrimitive() && anchorValue.getAsJsonPrimitive().isString();
            boolean listed = anchorValue != null && anchorValue.isJsonObject() && anchorValue.getAsJsonObject().has("blocks");
            if (!keyed && !listed) {
                continue;
            }
            int rowIndex = rows.size();
            rows.add(new Runnable[]{() -> {
                int ry = rowY(rowIndex, top);
                JsonElement value = anchors.get(glow);
                String status = value.isJsonPrimitive()
                        ? Component.translatable("gui.mm.effect_studio.blocks.all", value.getAsString()).getString()
                        : Component.translatable("gui.mm.effect_studio.blocks.count", blockList(value).size()).getString();
                rowLabels.add(new Object[]{px, ry + 3, font.plainSubstrByWidth(pretty(glow) + ": " + status, PANEL - 40), 0xFFFFE080});
                boolean picking = glow.equals(pickingBlocks);
                smallButtonTip(right - 36, ry, 36, Component.translatable(picking ? "gui.mm.effect_studio.blocks.done" : "gui.mm.effect_studio.blocks.pick"),
                        "blocks.pick_tip", () -> {
                            if (picking) {
                                pickingBlocks = null;
                            } else {
                                startPickingBlocks(glow);
                            }
                            rebuildWidgets();
                        });
            }});
        }
        int visible = Math.max(1, (height - 8 - top) / 16);
        spotScrollMax = Math.max(0, rows.size() - visible);
        spotScroll = Math.max(0, Math.min(spotScroll, spotScrollMax));
        for (int r = spotScroll; r < rows.size() && r < spotScroll + visible; r++) {
            rows.get(r)[0].run();
        }
    }

    private static Set<String> glowAnchors(@Nullable JsonObject fx) {
        var result = new java.util.LinkedHashSet<String>();
        if (fx == null || !fx.has("layers")) {
            return result;
        }
        for (JsonElement element : fx.getAsJsonArray("layers")) {
            JsonObject layer = element.getAsJsonObject();
            boolean halo = layer.has("mode") && "halo".equals(layer.get("mode").getAsString());
            if ("glow".equals(layer.has("type") ? layer.get("type").getAsString() : "") && !halo
                    && !(layer.has("off") && layer.get("off").getAsBoolean())) {
                result.add(layer.has("at") ? layer.get("at").getAsString() : "glow");
            }
        }
        return result;
    }

    private static List<int[]> blockList(@Nullable JsonElement value) {
        var result = new ArrayList<int[]>();
        if (value == null || !value.isJsonObject() || !value.getAsJsonObject().has("blocks")) {
            return result;
        }
        for (JsonElement entry : value.getAsJsonObject().getAsJsonArray("blocks")) {
            JsonArray b = entry.getAsJsonArray();
            result.add(new int[]{b.get(0).getAsInt(), b.get(1).getAsInt(), b.get(2).getAsInt()});
        }
        return result;
    }

    private static JsonObject blockJson(List<int[]> blocks) {
        var list = new JsonArray();
        for (int[] b : blocks) {
            var array = new JsonArray();
            array.add(b[0]);
            array.add(b[1]);
            array.add(b[2]);
            list.add(array);
        }
        var holder = new JsonObject();
        holder.add("blocks", list);
        return holder;
    }

    private void startPickingBlocks(String name) {
        JsonObject anchors = selectedAnchors();
        JsonElement value = anchors.get(name);
        if (value != null && value.isJsonPrimitive()) {
            remember();
            var origin = model.layout().getCharGrid().getControllerPos();
            var blocks = new ArrayList<int[]>();
            for (PositionedLayoutPiece piece : model.layout().getPositionedPieces()) {
                if (value.getAsString().equals(piece.piece().valueId())) {
                    blocks.add(new int[]{piece.pos().getX() + origin.getX(), piece.pos().getY() + origin.getY(), piece.pos().getZ() + origin.getZ()});
                }
            }
            anchors.add(name, blockJson(blocks));
            session.dirty = true;
        }
        pickingBlocks = name;
        pickingFor = session.selected;
        placing = null;
        adding = null;
        linkFrom = -1;
        deselect();
        steps.clear();
    }

    private void toggleBlock(BlockPos pos) {
        if (pickingBlocks == null || selectedId() == null) {
            pickingBlocks = null;
            return;
        }
        JsonObject anchors = selectedAnchors();
        if (!anchors.has(pickingBlocks) || !anchors.get(pickingBlocks).isJsonObject()) {
            pickingBlocks = null;
            return;
        }
        var origin = model.layout().getCharGrid().getControllerPos();
        int[] cell = {pos.getX() + origin.getX(), pos.getY() + origin.getY(), pos.getZ() + origin.getZ()};
        List<int[]> blocks = blockList(anchors.get(pickingBlocks));
        remember();
        if (!blocks.removeIf(b -> b[0] == cell[0] && b[1] == cell[1] && b[2] == cell[2])) {
            blocks.add(cell);
        }
        anchors.add(pickingBlocks, blockJson(blocks));
        session.dirty = true;
        rebuildWidgets();
    }

    private int rowY(int row, int top) {
        return top + (row - spotScroll) * 16;
    }

    @Nullable
    private Handle findHandle(String name, int index) {
        for (Handle h : handles()) {
            if (h.name().equals(name) && h.index() == index) {
                return h;
            }
        }
        return null;
    }

    private void pickRow(String name, int index) {
        if (name.equals(adding) && linking()) {
            Handle target = findHandle(name, index);
            if (target != null && index != linkFrom) {
                connect(target);
            }
            return;
        }
        Handle row = findHandle(name, index);
        if (hasControlDown() && handleName != null && row != null && adding == null && placing == null && pickingBlocks == null) {
            toggleSelect(row);
            rebuildWidgets();
            return;
        }
        adding = null;
        linkFrom = -1;
        pickingBlocks = null;
        placing = null;
        held = null;
        steps.clear();
        selectOnly(name, index);
        rebuildWidgets();
    }

    private void removeLink(String name, int a, int b) {
        remember();
        editLinks(name, links -> links.removeIf(l -> l[0] == a && l[1] == b));
        rebuildWidgets();
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
            if (entry.getValue().isJsonArray() && !DERIVED.contains(entry.getKey()) && !VERTICAL.contains(entry.getKey())) {
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

    private static Set<String> linkedOf(@Nullable JsonObject fx) {
        var result = new HashSet<String>();
        if (fx == null || !fx.has("layers")) {
            return result;
        }
        for (JsonElement element : fx.getAsJsonArray("layers")) {
            JsonObject layer = element.getAsJsonObject();
            if ("beam".equals(layer.has("type") ? layer.get("type").getAsString() : "") && layer.has("links")
                    && layer.has("from") && layer.has("to") && layer.get("from").getAsString().equals(layer.get("to").getAsString())) {
                result.add(layer.get("from").getAsString());
            }
        }
        return result;
    }

    private static List<int[]> linksOf(@Nullable JsonObject fx, String name) {
        var result = new ArrayList<int[]>();
        if (fx == null || !fx.has("layers")) {
            return result;
        }
        for (JsonElement element : fx.getAsJsonArray("layers")) {
            JsonObject layer = element.getAsJsonObject();
            if (layer.has("links") && layer.has("from") && name.equals(layer.get("from").getAsString())
                    && layer.has("to") && name.equals(layer.get("to").getAsString())) {
                JsonArray list = layer.getAsJsonArray("links");
                boolean flat = !list.isEmpty() && list.get(0).isJsonPrimitive();
                for (int i = 0; i < list.size(); i++) {
                    if (flat) {
                        if (i + 1 < list.size()) {
                            result.add(new int[]{list.get(i).getAsInt(), list.get(i + 1).getAsInt()});
                        }
                        i++;
                        continue;
                    }
                    JsonArray pair = list.get(i).getAsJsonArray();
                    if (pair.size() == 2) {
                        result.add(new int[]{pair.get(0).getAsInt(), pair.get(1).getAsInt()});
                    }
                }
                return result;
            }
        }
        return result;
    }

    private static void setLinks(JsonObject fx, String name, List<int[]> links) {
        for (JsonElement element : fx.getAsJsonArray("layers")) {
            JsonObject layer = element.getAsJsonObject();
            if (layer.has("links") && layer.has("from") && name.equals(layer.get("from").getAsString())
                    && layer.has("to") && name.equals(layer.get("to").getAsString())) {
                var list = new JsonArray();
                for (int[] link : links) {
                    var pair = new JsonArray();
                    pair.add(link[0]);
                    pair.add(link[1]);
                    list.add(pair);
                }
                layer.add("links", list);
            }
        }
    }

    private void editLinks(String name, java.util.function.Consumer<List<int[]>> change) {
        ResourceLocation id = selectedId();
        JsonObject fx = id == null ? null : base(id);
        if (fx == null || !linkedOf(fx).contains(name)) {
            return;
        }
        List<int[]> links = linksOf(fx, name);
        change.accept(links);
        setLinks(fx, name, links);
        rescale(id);
    }

    private void link(String name, int from, int to) {
        editLinks(name, links -> {
            if (from == to || from < 0 || to < 0) {
                return;
            }
            for (int[] l : links) {
                if ((l[0] == from && l[1] == to) || (l[0] == to && l[1] == from)) {
                    return;
                }
            }
            links.add(new int[]{from, to});
        });
    }

    private void dropPoint(String name, int index) {
        editLinks(name, links -> {
            links.removeIf(l -> l[0] == index || l[1] == index);
            for (int[] l : links) {
                if (l[0] > index) {
                    l[0]--;
                }
                if (l[1] > index) {
                    l[1]--;
                }
            }
        });
    }

    private void unlink(Handle h) {
        remember();
        editLinks(h.name(), links -> links.removeIf(l -> l[0] == h.index() || l[1] == h.index()));
    }

    private void startLinking(String name) {
        pickingBlocks = null;
        int count = pointsOf(selectedAnchors().get(name)).size();
        adding = name;
        linkFrom = name.equals(handleName) && handleIndex < count ? handleIndex : count - 1;
        deselect();
        placing = null;
        steps.clear();
    }

    private boolean linking() {
        ResourceLocation id = selectedId();
        return adding != null && linkFrom >= 0 && id != null && linkedOf(base(id)).contains(adding)
                && linkFrom < pointsOf(selectedAnchors().get(adding)).size();
    }

    private void connect(Handle target) {
        if (adding == null || target.index() == linkFrom) {
            return;
        }
        remember();
        link(adding, linkFrom, target.index());
        if (hasControlDown()) {
            linkFrom = target.index();
        } else {
            adding = null;
        }
        rebuildWidgets();
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
            if (anchors.has("seep_top")) {
                JsonElement value = autoAll("seep_top", source, null, null);
                if (value != null) {
                    anchors.add("seep_top", value);
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
        remember();
        var origin = model.layout().getCharGrid().getControllerPos();
        Vec3 at = snap(hit);
        boolean linking = linking();
        JsonArray point = point(at.x + origin.getX(), at.y + origin.getY(), at.z + origin.getZ());
        appendPoint(selectedAnchors(), adding, point);
        changed(adding);
        if (linking) {
            List<double[]> points = pointsOf(selectedAnchors().get(adding));
            int index = -1;
            for (int i = 0; i < points.size(); i++) {
                if (Math.abs(points.get(i)[0] - point.get(0).getAsDouble()) < 1e-3 && Math.abs(points.get(i)[1] - point.get(1).getAsDouble()) < 1e-3
                        && Math.abs(points.get(i)[2] - point.get(2).getAsDouble()) < 1e-3) {
                    index = i;
                }
            }
            link(adding, linkFrom, index);
            linkFrom = index;
        }
        if (!hasControlDown()) {
            adding = null;
        }
        rebuildWidgets();
    }

    private EffectStudio.Snapshot snapshot() {
        var effects = new ArrayList<JsonObject>();
        session.effects.forEach(e -> effects.add(e.deepCopy()));
        var edited = new LinkedHashMap<ResourceLocation, JsonObject>();
        session.edited.forEach((k, v) -> edited.put(k, v.deepCopy()));
        var originals = new HashMap<ResourceLocation, JsonObject>();
        session.originals.forEach((k, v) -> originals.put(k, v.deepCopy()));
        var sliders = new HashMap<ResourceLocation, double[]>();
        session.sliders.forEach((k, v) -> sliders.put(k, v.clone()));
        return new EffectStudio.Snapshot(effects, edited, originals, sliders, new HashMap<>(session.colors), session.selected);
    }

    private void remember() {
        session.undo.add(snapshot());
        if (session.undo.size() > 100) {
            session.undo.remove(0);
        }
        session.redo.clear();
        updateHistoryButtons();
    }

    private void updateHistoryButtons() {
        if (undoButton != null) {
            undoButton.active = !session.undo.isEmpty();
        }
        if (redoButton != null) {
            redoButton.active = !session.redo.isEmpty();
        }
    }

    private void restore(EffectStudio.Snapshot snapshot) {
        for (var entry : session.edited.entrySet()) {
            if (snapshot.edited().containsKey(entry.getKey())) {
                continue;
            }
            JsonObject original = snapshot.originals().getOrDefault(entry.getKey(), session.originals.get(entry.getKey()));
            if (original != null) {
                try {
                    FxManager.preview(entry.getKey(), original.deepCopy());
                } catch (Exception ignored) {
                }
            }
        }
        session.effects.clear();
        session.effects.addAll(snapshot.effects());
        session.edited.clear();
        session.edited.putAll(snapshot.edited());
        session.originals.clear();
        session.originals.putAll(snapshot.originals());
        session.sliders.clear();
        session.sliders.putAll(snapshot.sliders());
        session.colors.clear();
        session.colors.putAll(snapshot.colors());
        session.selected = snapshot.selected();
        for (var entry : session.edited.entrySet()) {
            try {
                FxManager.preview(entry.getKey(), entry.getValue());
            } catch (Exception ignored) {
            }
        }
        session.dirty = true;
        placing = null;
        adding = null;
        held = null;
        steps.clear();
        deselect();
        pickingBlocks = null;
        spotScroll = 0;
        rebuildWidgets();
        setFocused(null);
    }

    private void undo() {
        if (!session.undo.isEmpty()) {
            session.redo.add(snapshot());
            restore(session.undo.remove(session.undo.size() - 1));
        }
    }

    private void redo() {
        if (!session.redo.isEmpty()) {
            session.undo.add(snapshot());
            restore(session.redo.remove(session.redo.size() - 1));
        }
    }

    private static String format(double value) {
        String text = String.valueOf(Math.round(value * 10000) / 10000.0);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }

    private List<Handle> handles() {
        var result = new ArrayList<Handle>();
        if (selectedId() == null) {
            return result;
        }
        JsonObject anchors = selectedAnchors();
        var origin = model.layout().getCharGrid().getControllerPos();
        String source = sourceName(anchors);
        List<double[]> sourcePoints = source == null ? List.of() : pointsOf(anchors.get(source));
        double hx = sourcePoints.isEmpty() ? origin.getX() + 0.5 : sourcePoints.get(0)[0];
        double hz = sourcePoints.isEmpty() ? origin.getZ() + 0.5 : sourcePoints.get(0)[2];
        for (var entry : anchors.entrySet()) {
            String name = entry.getKey();
            JsonElement value = entry.getValue();
            if (DERIVED.contains(name) && !VERTICAL.contains(name)) {
                continue;
            }
            if (value.isJsonArray()) {
                boolean vertical = VERTICAL.contains(name);
                List<double[]> points = pointsOf(value);
                for (int i = 0; i < points.size(); i++) {
                    double[] p = points.get(i);
                    result.add(new Handle(name, i, p[0], p[1], p[2], vertical));
                }
            } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                result.add(new Handle(name, -1, hx, value.getAsDouble(), hz, true));
            }
        }
        return result;
    }

    @Nullable
    private Handle handle() {
        if (handleName == null) {
            return null;
        }
        for (Handle h : handles()) {
            if (h.name().equals(handleName) && h.index() == handleIndex) {
                return h;
            }
        }
        return null;
    }

    @Nullable
    private Handle handleAt(double mouseX, double mouseY) {
        if (placing != null || adding != null || pickingBlocks != null || facePicking) {
            return null;
        }
        return handleAt(mouseX, mouseY, null);
    }

    @Nullable
    private Handle handleAt(double mouseX, double mouseY, @Nullable String only) {
        if (!viewport.contains((int) mouseX, (int) mouseY)) {
            return null;
        }
        var origin = model.layout().getCharGrid().getControllerPos();
        Handle best = null;
        double bestDistance = 7 * 7;
        for (Handle h : handles()) {
            if (only != null && !only.equals(h.name())) {
                continue;
            }
            double[] s = view.project(h.x() - origin.getX(), h.y() - origin.getY(), h.z() - origin.getZ());
            if (s == null) {
                continue;
            }
            double d = (s[0] - mouseX) * (s[0] - mouseX) + (s[1] - mouseY) * (s[1] - mouseY);
            if (d < bestDistance) {
                bestDistance = d;
                best = h;
            }
        }
        return best;
    }

    private void setHandle(Handle h, double x, double y, double z) {
        JsonObject anchors = selectedAnchors();
        if (h.height() && anchors.has(h.name()) && anchors.get(h.name()).isJsonArray()) {
            List<double[]> points = pointsOf(anchors.get(h.name()));
            if (h.index() < 0 || h.index() >= points.size()) {
                return;
            }
            points.get(h.index())[1] = Math.round(y * 16) / 16.0;
            anchors.add(h.name(), pointsJson(points));
        } else if (h.height()) {
            anchors.addProperty(h.name(), Math.round(y * 16) / 16.0);
        } else {
            List<double[]> points = pointsOf(anchors.get(h.name()));
            if (h.index() < 0 || h.index() >= points.size()) {
                return;
            }
            points.set(h.index(), new double[]{x, y, z});
            anchors.add(h.name(), pointsJson(points));
        }
        changed(h.name());
    }

    private void drag(double mouseX, double mouseY) {
        Handle h = handle();
        if (h == null) {
            dragging = false;
            return;
        }
        if (!dragMoved) {
            if (Math.abs(mouseX - pressX) + Math.abs(mouseY - pressY) < 3) {
                return;
            }
            remember();
            dragMoved = true;
            dragY = h.y();
            lastMouseY = mouseY;
        }
        var origin = model.layout().getCharGrid().getControllerPos();
        if (h.height() || hasAltDown()) {
            double[] s0 = view.project(h.x() - origin.getX(), dragY - origin.getY(), h.z() - origin.getZ());
            double[] s1 = view.project(h.x() - origin.getX(), dragY + 1 - origin.getY(), h.z() - origin.getZ());
            if (s0 == null || s1 == null || Math.abs(s1[1] - s0[1]) < 1e-3) {
                return;
            }
            dragY += (mouseY - lastMouseY) / (s1[1] - s0[1]);
            lastMouseY = mouseY;
            setHandle(h, h.x(), Math.round(dragY * 16) / 16.0, h.z());
            return;
        }
        lastMouseY = mouseY;
        BlockHitResult hit = view.pick(mouseX, mouseY);
        if (hit == null || (undersidesOf(base(selectedId())).contains(h.name()) && hit.getDirection() != Direction.DOWN)) {
            return;
        }
        Vec3 at = snap(hit);
        dragY = at.y + origin.getY();
        setHandle(h, at.x + origin.getX(), at.y + origin.getY(), at.z + origin.getZ());
    }

    private void deleteHandle(Handle h) {
        JsonObject anchors = selectedAnchors();
        if (h.height() || pointsOf(anchors.get(h.name())).size() <= 1) {
            return;
        }
        remember();
        removePoint(h);
        deselect();
        rebuildWidgets();
        setFocused(null);
    }

    private void deleteSelected() {
        List<Handle> targets = selectedPoints();
        if (targets.size() < 2) {
            Handle h = handle();
            if (h != null) {
                deleteHandle(h);
            }
            return;
        }
        targets.sort((a, b) -> Integer.compare(b.index(), a.index()));
        var left = new HashMap<String, Integer>();
        var removable = new ArrayList<Handle>();
        for (Handle h : targets) {
            int remaining = left.computeIfAbsent(h.name(), n -> pointsOf(selectedAnchors().get(n)).size());
            if (remaining > 1) {
                removable.add(h);
                left.put(h.name(), remaining - 1);
            }
        }
        if (removable.size() < targets.size()) {
            minecraft.player.displayClientMessage(Component.translatable("gui.mm.effect_studio.kept_one"), true);
        }
        if (removable.isEmpty()) {
            return;
        }
        remember();
        for (Handle h : removable) {
            removePoint(h);
        }
        deselect();
        rebuildWidgets();
        setFocused(null);
    }

    private void removePoint(Handle h) {
        JsonObject anchors = selectedAnchors();
        List<double[]> points = pointsOf(anchors.get(h.name()));
        points.remove(h.index());
        anchors.add(h.name(), pointsJson(points));
        dropPoint(h.name(), h.index());
        dropSeepPoint(h.name(), h.index());
        dropSkipPoint(h.name(), h.index());
        if (h.name().equals(adding) && linkFrom >= 0) {
            if (linkFrom == h.index()) {
                adding = null;
                linkFrom = -1;
            } else if (linkFrom > h.index()) {
                linkFrom--;
            }
        }
        changed(h.name());
    }

    private ResourceLocation newEffectId() {
        int n = 1;
        ResourceLocation id;
        do {
            id = session.structure.withSuffix("_fx" + n++);
        } while (FxManager.source(id) != null || session.edited.containsKey(id));
        return id;
    }

    private boolean copyDefinition(ResourceLocation id, ResourceLocation copy, JsonObject json) {
        if (json.has("name")) {
            json.addProperty("name", json.get("name").getAsString() + " copy");
        }
        session.originals.put(copy, json);
        double[] sliders = session.sliders.get(id);
        if (sliders != null) {
            session.sliders.put(copy, sliders.clone());
        }
        if (session.colors.containsKey(id)) {
            session.colors.put(copy, session.colors.get(id));
        }
        if (!rescale(copy)) {
            session.originals.remove(copy);
            session.sliders.remove(copy);
            session.colors.remove(copy);
            return false;
        }
        return true;
    }

    private void duplicate(ResourceLocation id) {
        JsonObject current = base(id);
        if (current == null) {
            return;
        }
        remember();
        ResourceLocation copy = newEffectId();
        if (!copyDefinition(id, copy, current.deepCopy())) {
            session.undo.remove(session.undo.size() - 1);
            updateHistoryButtons();
            return;
        }
        JsonObject effect = session.effects.get(session.selected).deepCopy();
        effect.addProperty("fx", copy.toString());
        session.effects.add(effect);
        session.selected = session.effects.size() - 1;
        session.dirty = true;
        deselect();
        rebuildWidgets();
    }

    private void splitSpot(Handle h) {
        ResourceLocation id = selectedId();
        JsonObject anchors = selectedAnchors();
        List<double[]> points = pointsOf(anchors.get(h.name()));
        if (id == null || h.height() || points.size() <= 1 || h.index() >= points.size()) {
            return;
        }
        JsonObject current = base(id);
        if (current == null) {
            return;
        }
        JsonObject json = current.deepCopy();
        for (JsonElement element : json.getAsJsonArray("layers")) {
            JsonObject layer = element.getAsJsonObject();
            String type = layer.has("type") ? layer.get("type").getAsString() : "";
            if (!usesAnchor(layer, h.name())) {
                layer.addProperty("off", true);
                continue;
            }
            if (h.name().equals(mainAnchor(layer))) {
                boolean skipped = skips(layer, h.index());
                layer.remove("skip");
                if (skipped) {
                    var skip = new JsonArray();
                    skip.add(0);
                    layer.add("skip", skip);
                }
            }
            String at = layer.has("at") && layer.get("at").isJsonPrimitive() ? layer.get("at").getAsString() : "drips";
            if (type.equals("seep") && at.equals(h.name()) && layer.has("point_faces") && layer.get("point_faces").isJsonArray()) {
                JsonArray perPoint = layer.getAsJsonArray("point_faces");
                JsonElement own = h.index() < perPoint.size() ? perPoint.get(h.index()) : null;
                layer.remove("point_faces");
                if (own != null && !own.isJsonNull()) {
                    var list = new JsonArray();
                    list.add(own.deepCopy());
                    layer.add("point_faces", list);
                }
            }
            if (type.equals("beam") && layer.has("links") && layer.has("from") && layer.has("to")
                    && (h.name().equals(layer.get("from").getAsString()) || h.name().equals(layer.get("to").getAsString()))) {
                layer.add("links", new JsonArray());
            }
        }
        remember();
        ResourceLocation copy = newEffectId();
        if (!copyDefinition(id, copy, json)) {
            session.undo.remove(session.undo.size() - 1);
            updateHistoryButtons();
            return;
        }
        JsonObject effect = session.effects.get(session.selected).deepCopy();
        effect.addProperty("fx", copy.toString());
        JsonObject copyAnchors = effect.getAsJsonObject("anchors");
        double[] p = points.get(h.index());
        copyAnchors.add(h.name(), point(p[0], p[1], p[2]));
        if (h.name().equals(sourceName(anchors))) {
            for (String aligned : new String[]{"floor", "landing", "sky", "seep_top"}) {
                JsonElement value = copyAnchors.get(aligned);
                if (value != null && value.isJsonArray() && !value.getAsJsonArray().isEmpty() && value.getAsJsonArray().get(0).isJsonArray()) {
                    List<double[]> list = pointsOf(value);
                    double[] q = list.get(Math.min(h.index(), list.size() - 1));
                    copyAnchors.add(aligned, aligned.equals("floor") || aligned.equals("seep_top") ? new JsonPrimitive(q[1]) : point(q[0], q[1], q[2]));
                }
            }
        }
        removePoint(h);
        session.effects.add(effect);
        session.selected = session.effects.size() - 1;
        session.dirty = true;
        deselect();
        rebuildWidgets();
        setFocused(null);
    }

    private static boolean usesAnchor(JsonObject layer, String anchor) {
        for (String key : new String[]{"at", "from", "to", "top"}) {
            if (layer.has(key) && layer.get(key).isJsonPrimitive() && anchor.equals(layer.get(key).getAsString())) {
                return true;
            }
        }
        return anchor.equals(mainAnchor(layer));
    }

    private static boolean inside(@Nullable Button button, double mouseX, double mouseY) {
        return button != null && button.visible && mouseX >= button.getX() && mouseX < button.getX() + button.getWidth()
                && mouseY >= button.getY() && mouseY < button.getY() + button.getHeight();
    }

    private boolean overOverlay(double mouseX, double mouseY) {
        return coords != null && coords.isMouseOver(mouseX, mouseY) || inside(centreButton, mouseX, mouseY) || inside(facesButton, mouseX, mouseY);
    }

    private void deselect() {
        handleName = null;
        group.clear();
        updateCentreButtons();
    }

    private void selectOnly(String name, int index) {
        handleName = name;
        handleIndex = index;
        group.clear();
        updateCentreButtons();
    }

    private boolean isSelected(String name, int index) {
        return name.equals(handleName) && index == handleIndex || !group.isEmpty() && group.contains(new Pick(name, index));
    }

    private void toggleSelect(Handle h) {
        if (h.height()) {
            return;
        }
        Handle primary = handle();
        if (handleName == null || primary == null || primary.height()) {
            selectOnly(h.name(), h.index());
            return;
        }
        if (h.name().equals(handleName) && h.index() == handleIndex) {
            if (group.isEmpty()) {
                deselect();
                return;
            }
            Pick next = group.iterator().next();
            group.remove(next);
            handleName = next.name();
            handleIndex = next.index();
            return;
        }
        Pick pick = new Pick(h.name(), h.index());
        if (!group.remove(pick)) {
            group.add(pick);
        }
    }

    private List<Handle> selectedPoints() {
        var result = new ArrayList<Handle>();
        if (handleName == null) {
            return result;
        }
        for (Handle h : handles()) {
            if (!h.height() && isSelected(h.name(), h.index())) {
                result.add(h);
            }
        }
        return result;
    }

    private void selectBox(double x0, double y0, double x1, double y1, boolean add) {
        double minX = Math.min(x0, x1);
        double maxX = Math.max(x0, x1);
        double minY = Math.min(y0, y1);
        double maxY = Math.max(y0, y1);
        var origin = model.layout().getCharGrid().getControllerPos();
        var inside = new ArrayList<Handle>();
        for (Handle h : handles()) {
            if (h.height()) {
                continue;
            }
            double[] s = view.project(h.x() - origin.getX(), h.y() - origin.getY(), h.z() - origin.getZ());
            if (s != null && s[0] >= minX && s[0] <= maxX && s[1] >= minY && s[1] <= maxY) {
                inside.add(h);
            }
        }
        if (!add) {
            deselect();
        }
        Handle primary = handleName == null ? null : handle();
        boolean needPrimary = primary == null || primary.height();
        for (Handle h : inside) {
            if (needPrimary) {
                selectOnly(h.name(), h.index());
                needPrimary = false;
            } else if (!isSelected(h.name(), h.index())) {
                group.add(new Pick(h.name(), h.index()));
            }
        }
        rebuildWidgets();
        setFocused(null);
    }

    private void centreSelected() {
        List<Handle> targets = selectedPoints();
        if (targets.isEmpty() || facePicking) {
            return;
        }
        remember();
        for (Handle h : targets) {
            Direction face = faceOf(new double[]{h.x(), h.y(), h.z()});
            double x = face.getAxis() == Direction.Axis.X ? h.x() : Math.floor(h.x()) + 0.5;
            double y = face.getAxis() == Direction.Axis.Y ? h.y() : Math.floor(h.y()) + 0.5;
            double z = face.getAxis() == Direction.Axis.Z ? h.z() : Math.floor(h.z()) + 0.5;
            setHandle(h, x, y, z);
        }
        rebuildWidgets();
        setFocused(null);
    }

    private void facesPressed() {
        if (facePicking) {
            applyFaces();
            return;
        }
        Handle h = handle();
        if (h == null || h.height() || !group.isEmpty() || placing != null || adding != null || pickingBlocks != null) {
            return;
        }
        menu = null;
        facePicking = true;
        centreFaces.clear();
        updateCentreButtons();
    }

    private void toggleFace(BlockHitResult hit) {
        Face face = new Face(hit.getBlockPos(), hit.getDirection());
        if (!centreFaces.remove(face)) {
            centreFaces.add(face);
        }
        updateCentreButtons();
    }

    private void cancelFaces() {
        facePicking = false;
        centreFaces.clear();
        updateCentreButtons();
    }

    @Nullable
    private double[] facesCentre() {
        if (centreFaces.isEmpty()) {
            return null;
        }
        double x = 0;
        double y = 0;
        double z = 0;
        for (Face face : centreFaces) {
            x += face.pos().getX() + 0.5 + face.dir().getStepX() * 0.5;
            y += face.pos().getY() + 0.5 + face.dir().getStepY() * 0.5;
            z += face.pos().getZ() + 0.5 + face.dir().getStepZ() * 0.5;
        }
        int n = centreFaces.size();
        return new double[]{x / n, y / n, z / n};
    }

    private void applyFaces() {
        Handle h = handle();
        double[] middle = facesCentre();
        if (h == null || middle == null) {
            return;
        }
        var origin = model.layout().getCharGrid().getControllerPos();
        remember();
        setHandle(h, round(middle[0] + origin.getX()), round(middle[1] + origin.getY()), round(middle[2] + origin.getZ()));
        facePicking = false;
        centreFaces.clear();
        rebuildWidgets();
        setFocused(null);
    }

    private static double round(double value) {
        return Math.round(value * 1000) / 1000.0;
    }

    private void updateCentreButtons() {
        boolean any = !selectedPoints().isEmpty();
        if (centreButton != null) {
            centreButton.visible = any || facePicking;
            centreButton.active = !facePicking && any;
        }
        if (facesButton != null) {
            Handle h = handle();
            boolean single = h != null && !h.height() && group.isEmpty();
            facesButton.visible = facePicking || single;
            if (facePicking) {
                facesButton.setMessage(Component.translatable("gui.mm.effect_studio.faces_apply", centreFaces.size()));
                facesButton.active = !centreFaces.isEmpty();
            } else {
                facesButton.setMessage(Component.translatable("gui.mm.effect_studio.faces"));
                facesButton.active = single;
            }
        }
    }

    private void applyCoords() {
        Handle h = handle();
        if (h == null || coords == null) {
            return;
        }
        String[] parts = coords.getValue().trim().split("[,\\s]+");
        if (parts.length != (h.height() ? 1 : 3)) {
            return;
        }
        double[] values = new double[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) {
                values[i] = Double.parseDouble(parts[i]);
                if (!Double.isFinite(values[i])) {
                    return;
                }
            }
        } catch (NumberFormatException e) {
            return;
        }
        remember();
        if (h.height()) {
            setHandle(h, h.x(), values[0], h.z());
        } else {
            setHandle(h, values[0], values[1], values[2]);
        }
        rebuildWidgets();
        setFocused(null);
    }

    private int menuWidth(String[] entries, Handle h) {
        int w = 0;
        for (String entry : entries) {
            w = Math.max(w, font.width(menuLabel(entry, h)));
        }
        return w + 8;
    }

    private String[] menuEntries(Handle h) {
        if (h.height()) {
            return new String[0];
        }
        if (!group.isEmpty()) {
            return new String[]{"centre", "delete"};
        }
        ResourceLocation id = selectedId();
        var entries = new ArrayList<String>();
        String target = id == null ? null : arcTargetAt(h);
        if (target != null && selectedAnchors().has(target) && selectedAnchors().get(target).isJsonArray()) {
            entries.add("add_to");
        }
        if (id != null && linkedOf(base(id)).contains(h.name())) {
            entries.add("link");
            entries.add("unlink");
        }
        JsonObject fx = id == null ? null : base(id);
        if (fx != null && fx.has("layers")) {
            JsonArray layers = fx.getAsJsonArray("layers");
            for (int i = 0; i < layers.size(); i++) {
                JsonObject layer = layers.get(i).getAsJsonObject();
                if (!(layer.has("off") && layer.get("off").getAsBoolean()) && h.name().equals(mainAnchor(layer))) {
                    entries.add("layer:" + i);
                }
            }
        }
        if (fx != null && pointsOf(selectedAnchors().get(h.name())).size() > 1) {
            boolean used = false;
            for (JsonElement element : fx.getAsJsonArray("layers")) {
                used |= h.name().equals(mainAnchor(element.getAsJsonObject()));
            }
            if (used) {
                entries.add("split");
            }
        }
        entries.add("faces");
        entries.add("centre");
        entries.add("delete");
        return entries.toArray(new String[0]);
    }

    @Nullable
    private static String mainAnchor(JsonObject layer) {
        String type = layer.has("type") ? layer.get("type").getAsString() : "";
        boolean halo = layer.has("mode") && "halo".equals(layer.get("mode").getAsString());
        return switch (type) {
            case "drip", "pool", "seep" -> layer.has("at") ? layer.get("at").getAsString() : "drips";
            case "emitter" -> layer.has("at") ? layer.get("at").getAsString() : "emit";
            case "glow" -> halo ? (layer.has("at") ? layer.get("at").getAsString() : "glow") : null;
            case "stream" -> layer.has("from") ? layer.get("from").getAsString() : "drips";
            case "beam" -> layer.has("from") ? layer.get("from").getAsString() : "from";
            default -> null;
        };
    }

    private static boolean skips(JsonObject layer, int index) {
        if (!layer.has("skip")) {
            return false;
        }
        JsonElement listed = layer.get("skip");
        for (JsonElement e : listed.isJsonArray() ? listed.getAsJsonArray().asList() : List.of(listed)) {
            if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() && e.getAsInt() == index) {
                return true;
            }
        }
        return false;
    }

    private void toggleSkip(int layerIndex, int point) {
        ResourceLocation id = selectedId();
        JsonObject fx = id == null ? null : base(id);
        if (fx == null || layerIndex >= fx.getAsJsonArray("layers").size()) {
            return;
        }
        remember();
        JsonObject layer = fx.getAsJsonArray("layers").get(layerIndex).getAsJsonObject();
        var next = new JsonArray();
        boolean was = skips(layer, point);
        if (layer.has("skip")) {
            JsonElement listed = layer.get("skip");
            for (JsonElement e : listed.isJsonArray() ? listed.getAsJsonArray().asList() : List.of(listed)) {
                if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() && e.getAsInt() != point) {
                    next.add(e);
                }
            }
        }
        if (!was) {
            next.add(point);
        }
        if (next.isEmpty()) {
            layer.remove("skip");
        } else {
            layer.add("skip", next);
        }
        rescale(id);
        rebuildWidgets();
    }

    private void dropSkipPoint(String anchor, int index) {
        ResourceLocation id = selectedId();
        JsonObject fx = id == null ? null : base(id);
        if (fx == null || !fx.has("layers")) {
            return;
        }
        boolean touched = false;
        for (JsonElement element : fx.getAsJsonArray("layers")) {
            JsonObject layer = element.getAsJsonObject();
            if (!anchor.equals(mainAnchor(layer)) || !layer.has("skip")) {
                continue;
            }
            JsonElement listed = layer.get("skip");
            var next = new JsonArray();
            for (JsonElement e : listed.isJsonArray() ? listed.getAsJsonArray().asList() : List.of(listed)) {
                if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) {
                    continue;
                }
                int v = e.getAsInt();
                if (v != index) {
                    next.add(v > index ? v - 1 : v);
                }
            }
            if (next.isEmpty()) {
                layer.remove("skip");
            } else {
                layer.add("skip", next);
            }
            touched = true;
        }
        if (touched) {
            rescale(id);
        }
    }

    @Nullable
    private static String arcTarget(@Nullable JsonObject fx, String name) {
        if (fx == null || !fx.has("layers")) {
            return null;
        }
        for (JsonElement element : fx.getAsJsonArray("layers")) {
            JsonObject layer = element.getAsJsonObject();
            if (!"beam".equals(layer.has("type") ? layer.get("type").getAsString() : "") || !layer.has("from") || !layer.has("to")
                    || (layer.has("off") && layer.get("off").getAsBoolean())) {
                continue;
            }
            String from = layer.get("from").getAsString();
            String to = layer.get("to").getAsString();
            if (from.equals(to) || DERIVED.contains(to) || DERIVED.contains(from)) {
                continue;
            }
            if (name.equals(from)) {
                return to;
            }
        }
        return null;
    }

    @Nullable
    private String arcTargetAt(Handle h) {
        ResourceLocation id = selectedId();
        if (id == null) {
            return null;
        }
        JsonObject fx = base(id);
        String target = arcTarget(fx, h.name());
        if (target != null) {
            return target;
        }
        for (Handle other : handles()) {
            if (!other.height() && Math.abs(other.x() - h.x()) < 1e-3 && Math.abs(other.y() - h.y()) < 1e-3 && Math.abs(other.z() - h.z()) < 1e-3) {
                target = arcTarget(fx, other.name());
                if (target != null) {
                    return target;
                }
            }
        }
        return null;
    }

    private Component menuLabel(String entry, Handle h) {
        if (entry.startsWith("layer:")) {
            ResourceLocation id = selectedId();
            JsonObject fx = id == null ? null : base(id);
            int index = Integer.parseInt(entry.substring(6));
            if (fx == null || index >= fx.getAsJsonArray("layers").size()) {
                return Component.literal("?");
            }
            JsonObject layer = fx.getAsJsonArray("layers").get(index).getAsJsonObject();
            String name = layer.has("name") ? layer.get("name").getAsString() : pretty(layer.has("type") ? layer.get("type").getAsString() : "?");
            boolean skipped = skips(layer, h.index());
            return Component.literal((skipped ? "✗ " : "✓ ") + name).withStyle(skipped ? ChatFormatting.GRAY : ChatFormatting.GREEN);
        }
        if (entry.equals("add_to")) {
            String target = arcTargetAt(h);
            if (target != null && !target.equals("pillars")) {
                return Component.translatable("gui.mm.effect_studio.menu.add_to", pretty(target));
            }
            return Component.translatable("gui.mm.effect_studio.menu.add_pillar");
        }
        if (entry.equals("centre") && !group.isEmpty()) {
            return Component.translatable("gui.mm.effect_studio.menu.centre_many", group.size() + 1);
        }
        if (entry.equals("delete") && !group.isEmpty()) {
            return Component.translatable("gui.mm.effect_studio.menu.delete_many", group.size() + 1);
        }
        return Component.translatable("gui.mm.effect_studio.menu." + entry);
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
        placingStarts++;
        adding = null;
        facePicking = false;
        centreFaces.clear();
        updateCentreButtons();
        pickingBlocks = null;
        held = null;
        deselect();
        firstHit = null;
        steps.clear();
        undersides.clear();
        for (String key : pending.keySet().toArray(new String[0])) {
            pending.remove(key);
        }
        var seen = new LinkedHashMap<String, String>();
        for (JsonElement element : preset(preset).getAsJsonArray("layers")) {
            JsonObject layer = element.getAsJsonObject();
            String type = layer.has("type") ? layer.get("type").getAsString() : "";
            boolean area = "glow".equals(type) && !(layer.has("mode") && layer.get("mode").getAsString().equals("halo"));
            for (String key : new String[]{"at", "from"}) {
                if (FALLING.contains(type) && layer.has(key) && !(layer.has("off") && layer.get("off").getAsBoolean())) {
                    undersides.add(layer.get(key).getAsString());
                }
            }
            for (String key : new String[]{"at", "from", "to", "top"}) {
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
            boolean needsMore = linkedOf(preset(placing)).contains(step[0]) && pointsOf(pending.get(step[0])).size() < 2;
            held = hasControlDown() || needsMore ? step[0] : null;
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
                } else if (value.isJsonObject() && hit != null) {
                    BlockPos clicked = hit.getBlockPos();
                    for (int[] b : blockList(value)) {
                        if (b[0] - origin.getX() == clicked.getX() && b[1] - origin.getY() == clicked.getY() && b[2] - origin.getZ() == clicked.getZ()) {
                            keyed = i;
                        }
                    }
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
            if (best != session.selected) {
                deselect();
            }
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
        if (getFocused() instanceof EditBox) {
            return;
        }
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
        ResourceLocation id = newEffectId();
        JsonObject chosen = preset(placing);
        if (chosen == null) {
            placing = null;
            held = null;
            steps.clear();
            return;
        }
        JsonObject json = chosen.deepCopy();
        json.addProperty("name", presetName(placing));
        for (String name : linkedOf(json)) {
            var chain = new ArrayList<int[]>();
            int count = pointsOf(pending.get(name)).size();
            for (int i = 1; i < count; i++) {
                chain.add(new int[]{i - 1, i});
            }
            setLinks(json, name, chain);
        }
        try {
            FxManager.preview(id, json);
        } catch (Exception e) {
            placing = null;
            steps.clear();
            return;
        }
        remember();
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
        boolean perPoint = DERIVED.contains(name) || name.equals("seep_top");
        if (points.size() == 1 || !perPoint) {
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
            if (name.equals("floor") || name.equals("seep_top")) {
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
        return (name.equals("floor") || name.equals("seep_top")) && !differ ? new JsonPrimitive(same) : list;
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
            case "block" -> {
                String found = keyAt(clicked);
                for (Direction side : Direction.values()) {
                    if (found != null) {
                        break;
                    }
                    found = keyAt(new BlockPos((int) Math.floor(sx - origin.getX() - side.getStepX() * 0.01),
                            (int) Math.floor(sy - origin.getY() - side.getStepY() * 0.01), (int) Math.floor(sz - origin.getZ() - side.getStepZ() * 0.01)));
                }
                yield found;
            }
            case "below" -> keyAt(new BlockPos(cx, base, cz));
            default -> null;
        };
        return switch (name) {
            case "floor" -> new JsonPrimitive(floor);
            case "seep_top" -> {
                int top = (int) Math.floor(rel + 0.001);
                int limit = top + 16;
                while (top < limit && !world.getBlockState(new BlockPos(cx, top, cz)).isAir()) {
                    top++;
                }
                yield new JsonPrimitive(top + origin.getY());
            }
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
        remember();
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
            for (String key : new String[]{"at", "from", "to", "top"}) {
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

    private double[] sliderValues(ResourceLocation id) {
        double[] values = session.sliders.get(id);
        if (values == null || values.length < 9) {
            double[] next = {0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5};
            if (values != null) {
                System.arraycopy(values, 0, next, 0, Math.min(values.length, 9));
            }
            session.sliders.put(id, next);
            values = next;
        }
        return values;
    }

    private static boolean splits(String type, String key) {
        return switch (type) {
            case "drip", "emitter" -> key.equals("rate");
            case "glow" -> key.equals("size");
            case "beam" -> key.equals("width");
            default -> false;
        };
    }

    private boolean rescale(ResourceLocation id) {
        JsonObject base = base(id);
        if (base == null) {
            return false;
        }
        double[] values = sliderValues(id);
        JsonObject json = base.deepCopy();
        String color = session.colors.get(id);
        if (json.has("layers")) {
            for (JsonElement element : json.getAsJsonArray("layers")) {
                JsonObject layer = element.getAsJsonObject();
                String type = layer.has("type") ? layer.get("type").getAsString() : "";
                for (String key : layer.keySet().toArray(new String[0])) {
                    int c = AMOUNT.contains(key) ? 0 : SIZE.contains(key) ? 1 : SPEED.contains(key) ? 2 : -1;
                    if (c < 0) {
                        continue;
                    }
                    double m = factor(values[c]);
                    double idle = factor(values[3 + c]);
                    double working = factor(values[6 + c]);
                    JsonElement raw = layer.get(key);
                    boolean numeric = raw.isJsonPrimitive() && raw.getAsJsonPrimitive().isNumber();
                    if (splits(type, key) && (numeric || raw.isJsonObject()) && (idle != 1 || working != 1 || raw.isJsonObject())) {
                        double rawIdle = raw.isJsonObject() ? (raw.getAsJsonObject().has("idle") ? raw.getAsJsonObject().get("idle").getAsDouble() : Double.NaN) : raw.getAsDouble();
                        double rawWorking = raw.isJsonObject() ? (raw.getAsJsonObject().has("working") ? raw.getAsJsonObject().get("working").getAsDouble() : Double.NaN) : raw.getAsDouble();
                        var split = new JsonObject();
                        if (!Double.isNaN(rawIdle)) {
                            split.addProperty("idle", Math.round(rawIdle * m * idle * 10000) / 10000.0);
                        }
                        if (!Double.isNaN(rawWorking)) {
                            split.addProperty("working", Math.round(rawWorking * m * working * 10000) / 10000.0);
                        }
                        layer.add(key, split);
                        continue;
                    }
                    if (m == 1) {
                        continue;
                    }
                    JsonElement scaled = scale(raw, m);
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

    private void renderPending(PoseStack pose, MultiBufferSource buffers) {
        if (placing == null || steps.isEmpty()) {
            return;
        }
        var origin = model.layout().getCharGrid().getControllerPos();
        Set<String> linked = linkedOf(preset(placing));
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        int index = 0;
        for (String[] step : steps) {
            if (step[1].equals("auto")) {
                continue;
            }
            int color = SPOT_COLORS[index++ % SPOT_COLORS.length];
            if (!pending.has(step[0])) {
                continue;
            }
            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;
            if (step[1].equals("area")) {
                String key = pending.get(step[0]).getAsString();
                for (PositionedLayoutPiece piece : model.layout().getPositionedPieces()) {
                    if (key.equals(piece.piece().valueId())) {
                        BlockPos p = piece.pos();
                        LevelRenderer.renderLineBox(pose, lines, new AABB(p.getX() - 0.02, p.getY() - 0.02, p.getZ() - 0.02, p.getX() + 1.02, p.getY() + 1.02, p.getZ() + 1.02),
                                r, g, b, 1f);
                    }
                }
                continue;
            }
            List<double[]> points = pointsOf(pending.get(step[0]));
            for (int j = 0; j < points.size(); j++) {
                double x = points.get(j)[0] - origin.getX();
                double y = points.get(j)[1] - origin.getY();
                double z = points.get(j)[2] - origin.getZ();
                LevelRenderer.renderLineBox(pose, lines, new AABB(x - 0.08, y - 0.08, z - 0.08, x + 0.08, y + 0.08, z + 0.08), r, g, b, 1f);
                if (linked.contains(step[0]) && j > 0) {
                    double[] a = points.get(j - 1);
                    line(lines, pose, a[0] - origin.getX(), a[1] - origin.getY(), a[2] - origin.getZ(), x, y, z, 0xFF9FD8FF);
                }
            }
            if (!points.isEmpty() && step[0].equals(held) && linked.contains(step[0]) && hover != null) {
                double[] a = points.get(points.size() - 1);
                Vec3 end = snap(hover);
                line(lines, pose, a[0] - origin.getX(), a[1] - origin.getY(), a[2] - origin.getZ(), end.x, end.y, end.z, 0xFFFFFF55);
            }
        }
    }

    private static void faceOutline(PoseStack pose, VertexConsumer lines, Face face, float r, float g, float b) {
        double[] min = {face.pos().getX(), face.pos().getY(), face.pos().getZ()};
        double[] max = {min[0] + 1, min[1] + 1, min[2] + 1};
        int axis = face.dir().getAxis().ordinal();
        if (face.dir().getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            min[axis] = max[axis] - 0.01;
        } else {
            max[axis] = min[axis] + 0.01;
        }
        LevelRenderer.renderLineBox(pose, lines, new AABB(min[0], min[1], min[2], max[0], max[1], max[2]), r, g, b, 1f);
    }

    private void renderFaces(PoseStack pose, MultiBufferSource buffers) {
        if (!facePicking) {
            return;
        }
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (Face face : centreFaces) {
            faceOutline(pose, lines, face, 0.25f, 1f, 0.9f);
        }
        if (hover != null) {
            faceOutline(pose, lines, new Face(hover.getBlockPos(), hover.getDirection()), 1f, 1f, 0.33f);
        }
        double[] middle = facesCentre();
        Handle h = handle();
        if (middle == null || h == null) {
            return;
        }
        LevelRenderer.renderLineBox(pose, lines, new AABB(middle[0] - 0.08, middle[1] - 0.08, middle[2] - 0.08, middle[0] + 0.08, middle[1] + 0.08, middle[2] + 0.08), 1f, 1f, 1f, 1f);
        var origin = model.layout().getCharGrid().getControllerPos();
        line(lines, pose, h.x() - origin.getX(), h.y() - origin.getY(), h.z() - origin.getZ(), middle[0], middle[1], middle[2], 0xFFFFFF55);
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
            List<double[]> points = pointsOf(anchors.get(spots.get(i)));
            for (int j = 0; j < points.size(); j++) {
                double[] p = points.get(j);
                double x = p[0] - origin.getX();
                double y = p[1] - origin.getY();
                double z = p[2] - origin.getZ();
                boolean selected = isSelected(spots.get(i), j);
                boolean hot = hovered != null && hovered.name().equals(spots.get(i)) && hovered.index() == j;
                double size = selected || hot ? 0.09 : spots.get(i).equals(adding) ? 0.07 : 0.05;
                LevelRenderer.renderLineBox(pose, lines, new AABB(x - size, y - size, z - size, x + size, y + size, z + size),
                        selected ? 1f : r, selected ? 1f : g, selected ? 1f : b, 1f);
            }
        }
        ResourceLocation selectedFx = selectedId();
        JsonObject fx = selectedFx == null ? null : base(selectedFx);
        for (String name : linkedOf(fx)) {
            List<double[]> points = pointsOf(anchors.get(name));
            for (int[] l : linksOf(fx, name)) {
                if (l[0] < points.size() && l[1] < points.size()) {
                    double[] a = points.get(l[0]);
                    double[] b = points.get(l[1]);
                    line(lines, pose, a[0] - origin.getX(), a[1] - origin.getY(), a[2] - origin.getZ(),
                            b[0] - origin.getX(), b[1] - origin.getY(), b[2] - origin.getZ(), 0xFF9FD8FF);
                }
            }
            if (name.equals(adding) && linkFrom >= 0 && linkFrom < points.size()) {
                double[] a = points.get(linkFrom);
                Handle target = linkTarget;
                Vec3 end = target != null ? new Vec3(target.x(), target.y(), target.z())
                        : hover != null ? snap(hover).add(origin.getX(), origin.getY(), origin.getZ()) : null;
                if (end != null) {
                    line(lines, pose, a[0] - origin.getX(), a[1] - origin.getY(), a[2] - origin.getZ(),
                            end.x - origin.getX(), end.y - origin.getY(), end.z - origin.getZ(), 0xFFFFFF55);
                }
            }
        }
        for (Handle h : handles()) {
            if (!h.height()) {
                continue;
            }
            boolean selected = isSelected(h.name(), h.index());
            boolean hot = hovered != null && hovered.name().equals(h.name()) && hovered.height() && hovered.index() == h.index();
            double x = h.x() - origin.getX();
            double y = h.y() - origin.getY();
            double z = h.z() - origin.getZ();
            double size = selected || hot ? 0.3 : 0.2;
            LevelRenderer.renderLineBox(pose, lines, new AABB(x - size, y, z - size, x + size, y + 0.005, z + size),
                    selected ? 1f : 0.8f, selected ? 1f : 0.8f, selected ? 1f : 0.8f, 1f);
        }
    }

    private static void line(VertexConsumer vc, PoseStack pose, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1e-6) {
            return;
        }
        float nx = (float) (dx / length);
        float ny = (float) (dy / length);
        float nz = (float) (dz / length);
        PoseStack.Pose last = pose.last();
        vc.addVertex(last, (float) x1, (float) y1, (float) z1).setColor(color).setNormal(last, nx, ny, nz);
        vc.addVertex(last, (float) x2, (float) y2, (float) z2).setColor(color).setNormal(last, nx, ny, nz);
    }

    private void renderPickedBlocks(PoseStack pose, MultiBufferSource buffers) {
        if (pickingBlocks == null || selectedId() == null) {
            return;
        }
        var origin = model.layout().getCharGrid().getControllerPos();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (int[] b : blockList(selectedAnchors().get(pickingBlocks))) {
            double x = b[0] - origin.getX();
            double y = b[1] - origin.getY();
            double z = b[2] - origin.getZ();
            LevelRenderer.renderLineBox(pose, lines, new AABB(x - 0.02, y - 0.02, z - 0.02, x + 1.02, y + 1.02, z + 1.02), 1f, 0.85f, 0.3f, 1f);
        }
    }

    private void renderArrows(PoseStack pose, MultiBufferSource buffers) {
        List<double[]> arrows = arrows();
        if (arrows.isEmpty()) {
            return;
        }
        var origin = model.layout().getCharGrid().getControllerPos();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (double[] a : arrows) {
            double x0 = a[1] - origin.getX();
            double y0 = a[2] - origin.getY();
            double z0 = a[3] - origin.getZ();
            double x1 = a[4] - origin.getX();
            double y1 = a[5] - origin.getY();
            double z1 = a[6] - origin.getZ();
            line(lines, pose, x0, y0, z0, x1, y1, z1, 0xFFFF9A30);
            double size = (int) a[0] == arrowLayer ? 0.09 : 0.06;
            LevelRenderer.renderLineBox(pose, lines, new AABB(x1 - size, y1 - size, z1 - size, x1 + size, y1 + size, z1 + size), 1f, 0.6f, 0.2f, 1f);
        }
    }

    private void renderEffects(PoseStack pose, MultiBufferSource buffers, Matrix4f modelView) {
        renderGhost(pose, buffers);
        renderArrows(pose, buffers);
        renderPickedBlocks(pose, buffers);
        renderSpots(pose, buffers);
        renderPending(pose, buffers);
        renderFaces(pose, buffers);
        var be = EffectStudio.machine(session);
        var visuals = be == null ? null : EffectStudio.effects(be);
        if (visuals == null || model == null) {
            return;
        }
        preview = FxRenderer.preview(preview, model, be.getBlockPos(), visuals);
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
        hover = (placing != null || adding != null || facePicking) && viewport.contains(mouseX, mouseY) ? view.pick(mouseX, mouseY) : null;
        hovered = dragging ? handle() : menu != null ? null : handleAt(mouseX, mouseY);
        linkTarget = linking() ? handleAt(mouseX, mouseY, adding) : null;
        if (linkTarget != null && linkTarget.index() == linkFrom) {
            linkTarget = null;
        }
        gfx.fill(viewport.x(), viewport.y(), viewport.x() + viewport.w(), viewport.y() + viewport.h(), 0x66000000);
        gfx.pose().pushPose();
        gfx.pose().setIdentity();
        view.setViewport(viewport);
        view.init();
        view.render(gfx, mouseX, mouseY, viewport.contains(mouseX, mouseY));
        gfx.pose().popPose();
        if (!session.collapsed) {
            gfx.drawString(font, Component.translatable("gui.mm.effect_studio.add_effect"), 6, 8, 0xFFFFFF);
            gfx.drawString(font, Component.translatable("gui.mm.effect_studio.placed"), width - PANEL - 6, 8, 0xFFFFFF);
        }
        String[] step = placing == null ? null : currentStep();
        boolean underside = step != null ? undersides.contains(step[0])
                : adding != null && selectedId() != null && undersidesOf(base(selectedId())).contains(adding);
        Component hint;
        if (step != null) {
            String kind = step[0].equals(held) ? "held" : underside ? "underside" : step[1];
            hint = Component.translatable("gui.mm.effect_studio.click." + kind, presetLabel(placing), step[0]);
        } else if (pickingBlocks != null) {
            hint = Component.translatable("gui.mm.effect_studio.click.pick", pretty(pickingBlocks));
        } else if (facePicking) {
            hint = Component.translatable("gui.mm.effect_studio.click.faces", centreFaces.size());
        } else if (adding != null && linking()) {
            hint = Component.translatable("gui.mm.effect_studio.click.link", linkFrom + 1);
        } else if (adding != null) {
            hint = Component.translatable(underside ? "gui.mm.effect_studio.click.add_underside" : "gui.mm.effect_studio.click.add", adding);
        } else if (session.tab == 2 && !arrows().isEmpty()) {
            hint = Component.translatable("gui.mm.effect_studio.arrow_hint");
        } else if (selectedId() != null && !spots.isEmpty()) {
            hint = Component.translatable("gui.mm.effect_studio.drag_hint");
        } else {
            hint = Component.translatable("gui.mm.effect_studio.controls");
        }
        boolean wrong = underside && hover != null && hover.getDirection() != Direction.DOWN;
        gfx.drawCenteredString(font, hint, viewport.x() + viewport.w() / 2, viewport.y() + 4,
                wrong ? 0xFF5555 : step != null || adding != null || pickingBlocks != null || facePicking ? 0xFFFF55 : 0xA0A0A0);
        if (marquee) {
            int bx0 = (int) Math.min(pressX, marqueeX);
            int by0 = (int) Math.min(pressY, marqueeY);
            int bx1 = (int) Math.max(pressX, marqueeX);
            int by1 = (int) Math.max(pressY, marqueeY);
            gfx.fill(bx0, by0, bx1, by1, 0x3340C0FF);
            gfx.renderOutline(bx0, by0, bx1 - bx0, by1 - by0, 0xFF40C0FF);
        }
        if (!spots.isEmpty() && view != null) {
            var origin = model.layout().getCharGrid().getControllerPos();
            JsonObject labelled = selectedAnchors();
            for (int i = 0; i < spots.size(); i++) {
                List<double[]> points = pointsOf(labelled.get(spots.get(i)));
                if (points.size() < 2) {
                    continue;
                }
                for (int j = 0; j < points.size(); j++) {
                    double[] p = points.get(j);
                    double[] s = view.project(p[0] - origin.getX(), p[1] - origin.getY(), p[2] - origin.getZ());
                    if (s != null && viewport.contains((int) s[0], (int) s[1])) {
                        gfx.drawString(font, String.valueOf(j + 1), (int) s[0] + 5, (int) s[1] - 10, SPOT_COLORS[i % SPOT_COLORS.length]);
                    }
                }
            }
        }
        if (placing != null && view != null) {
            var origin = model.layout().getCharGrid().getControllerPos();
            int index = 0;
            for (String[] stepped : steps) {
                if (stepped[1].equals("auto")) {
                    continue;
                }
                int color = SPOT_COLORS[index++ % SPOT_COLORS.length];
                List<double[]> points = pointsOf(pending.get(stepped[0]));
                for (int j = 0; j < points.size(); j++) {
                    double[] p = points.get(j);
                    double[] s = view.project(p[0] - origin.getX(), p[1] - origin.getY(), p[2] - origin.getZ());
                    if (s != null && viewport.contains((int) s[0], (int) s[1])) {
                        gfx.drawString(font, points.size() > 1 ? pretty(stepped[0]) + " " + (j + 1) : pretty(stepped[0]), (int) s[0] + 5, (int) s[1] - 10, color);
                    }
                }
            }
        }
        for (Object[] label : rowLabels) {
            gfx.drawString(font, (String) label[2], (int) label[0], (int) label[1], (int) label[3]);
        }
        drawSeepSides(gfx);
        if (dragFromLibrary && placing != null && Math.abs(mouseX - pressX) + Math.abs(mouseY - pressY) >= 4) {
            String text = presetLabel(placing);
            int tw = font.width(text);
            gfx.fill(mouseX + 8, mouseY + 6, mouseX + 14 + tw, mouseY + 19, 0xE0101010);
            gfx.drawString(font, text, mouseX + 11, mouseY + 9, 0xFFFF55);
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
        if (selectedId() != null && session.tab == 2 && !session.collapsed) {
            int x = width - PANEL - 6;
            for (int i = 0; i < COLORS.length; i++) {
                int cx = x + (i % 5) * 26 + 3;
                int cy = colorsY + (i / 5) * 18 + 3;
                gfx.fill(cx, cy, cx + 18, cy + 10, 0xFF000000 | Integer.parseInt(COLORS[i].substring(1), 16));
            }
        }
        Handle selectedHandle = handle();
        if (coords != null && selectedHandle != null) {
            String label = selectedHandle.index() < 0 ? pretty(selectedHandle.name()) : pretty(selectedHandle.name()) + " #" + (selectedHandle.index() + 1);
            if (!group.isEmpty()) {
                label += "  " + Component.translatable("gui.mm.effect_studio.selected_more", group.size()).getString();
            }
            gfx.drawString(font, label, coords.getX(), coords.getY() - 10, 0xFFFFFF);
            coords.render(gfx, mouseX, mouseY, partialTick);
        }
        if (centreButton != null) {
            centreButton.render(gfx, mouseX, mouseY, partialTick);
        }
        if (facesButton != null) {
            facesButton.render(gfx, mouseX, mouseY, partialTick);
        }
        if (dropdown) {
            int px = width - PANEL - 6;
            int rows = dropRows();
            gfx.fill(px, 40, px + PANEL, 40 + rows * 16 + 2, 0xF0101010);
            for (int r = 0; r < rows; r++) {
                int i = dropScroll + r;
                int ry = 41 + r * 16;
                boolean over = mouseX >= px && mouseX < px + PANEL && mouseY >= ry && mouseY < ry + 16;
                if (over || i == session.selected) {
                    gfx.fill(px + 1, ry, px + PANEL - 1, ry + 16, over ? 0xFF3A3A3A : 0xFF263040);
                }
                gfx.drawString(font, font.plainSubstrByWidth((i + 1) + ". " + label(session.effects.get(i)), PANEL - 8), px + 4, ry + 4, 0xFFFFFF);
            }
            if (dropScroll + rows < session.effects.size()) {
                gfx.drawCenteredString(font, "\u25BC", px + PANEL / 2, 40 + rows * 16 - 6, 0xFF808080);
            }
        }
        if (menu != null) {
            String[] entries = menuEntries(menu);
            int menuW = menuWidth(entries, menu);
            gfx.fill(menuX, menuY, menuX + menuW, menuY + entries.length * 14 + 2, 0xEE101010);
            for (int i = 0; i < entries.length; i++) {
                int ey = menuY + 1 + i * 14;
                boolean over = mouseX >= menuX && mouseX < menuX + menuW && mouseY >= ey && mouseY < ey + 14;
                if (over) {
                    gfx.fill(menuX + 1, ey, menuX + menuW - 1, ey + 14, 0xFF3A3A3A);
                }
                gfx.drawString(font, menuLabel(entries[i], menu), menuX + 4, ey + 3, 0xFFFFFF);
            }
        }
    }

    private void drawSeepSides(GuiGraphics gfx) {
        sideHits.clear();
        ResourceLocation id = selectedId();
        JsonObject seep = id == null || view == null || placing != null || adding != null || facePicking ? null : firstSeep(base(id));
        if (seep == null) {
            return;
        }
        String at = seep.has("at") && seep.get("at").isJsonPrimitive() ? seep.get("at").getAsString() : "drips";
        List<double[]> points = pointsOf(selectedAnchors().get(at));
        if (points.isEmpty()) {
            return;
        }
        var origin = model.layout().getCharGrid().getControllerPos();
        Set<String> layerFaces = seepFaces(seep);
        String[] sides = {"north", "east", "south", "west"};
        double[][] offsets = {{0, -0.62}, {0.62, 0}, {0, 0.62}, {-0.62, 0}};
        for (int j = 0; j < points.size(); j++) {
            if (!at.equals(handleName) || j != handleIndex) {
                continue;
            }
            double[] p = points.get(j);
            double cx = Math.floor(p[0] - origin.getX()) + 0.5;
            double cy = Math.floor(p[1] - origin.getY() + 0.001) + 0.5;
            double cz = Math.floor(p[2] - origin.getZ()) + 0.5;
            Set<String> own = pointFaces(seep, j);
            Set<String> chosen = own != null ? own : layerFaces;
            int[][] steps = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
            var world = view.previewWorld();
            for (int i = 0; i < 4; i++) {
                BlockPos beside = new BlockPos((int) Math.floor(cx) + steps[i][0], (int) Math.floor(cy), (int) Math.floor(cz) + steps[i][1]);
                var state = world.getBlockState(beside);
                if (!state.isAir() && !state.getCollisionShape(world, beside).isEmpty()) {
                    continue;
                }
                double[] s = view.project(cx + offsets[i][0], cy, cz + offsets[i][1]);
                if (s == null || !viewport.contains((int) s[0], (int) s[1])) {
                    continue;
                }
                boolean on = chosen != null && chosen.contains(sides[i]);
                int x = (int) s[0];
                int y = (int) s[1];
                gfx.fill(x - 6, y - 6, x + 6, y + 6, on ? 0xC0105010 : 0xC0101010);
                gfx.drawCenteredString(font, sides[i].substring(0, 1).toUpperCase(java.util.Locale.ROOT), x, y - 4,
                        chosen == null ? 0xFFC0C0C0 : on ? 0xFF55FF55 : 0xFF808080);
                sideHits.add(new Object[]{x, y, sides[i], j});
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        pressX = mouseX;
        pressY = mouseY;
        marqueeArmed = false;
        marquee = false;
        deleteArmed = confirmDelete;
        if (confirmDelete != null) {
            confirmDelete = null;
            rebuildWidgets();
        }
        focusLater = null;
        if (getFocused() instanceof EditBox box && !box.isMouseOver(mouseX, mouseY)) {
            setFocused(null);
        }
        if (dropdown) {
            dropdown = false;
            swallowRelease = true;
            int px = width - PANEL - 6;
            int r = (int) ((mouseY - 41) / 16);
            if (button == 0 && mouseX >= px && mouseX < px + PANEL && mouseY >= 41 && r >= 0 && r < dropRows()) {
                session.selected = dropScroll + r;
                adding = null;
                linkFrom = -1;
                deselect();
                saving = false;
                spotScroll = 0;
                rebuildWidgets();
            }
            return true;
        }
        if (menu == null && overOverlay(mouseX, mouseY)) {
            boolean pressed = super.mouseClicked(mouseX, mouseY, button);
            if (getFocused() instanceof Button) {
                setFocused(null);
            }
            return pressed;
        }
        if (button == 0 && menu == null && selectedId() != null && handleAt(mouseX, mouseY) == null) {
            Object[] best = null;
            double bestDistance = Double.MAX_VALUE;
            for (Object[] hit : sideHits) {
                double dx = mouseX - (int) hit[0];
                double dy = mouseY - (int) hit[1];
                if (Math.abs(dx) <= 6 && Math.abs(dy) <= 6 && dx * dx + dy * dy < bestDistance) {
                    bestDistance = dx * dx + dy * dy;
                    best = hit;
                }
            }
            if (best != null) {
                toggleSeepFace(selectedId(), (String) best[2], (int) best[3]);
                swallowRelease = true;
                return true;
            }
        }
        if (menu != null) {
            swallowRelease = true;
            Handle target = menu;
            String[] entries = menuEntries(target);
            menu = null;
            int index = (int) ((mouseY - menuY - 1) / 14);
            if (button == 0 && mouseX >= menuX && mouseX < menuX + menuWidth(entries, target) && mouseY >= menuY && index >= 0 && index < entries.length) {
                if (entries[index].startsWith("layer:")) {
                    toggleSkip(Integer.parseInt(entries[index].substring(6)), target.index());
                    return true;
                }
                switch (entries[index]) {
                    case "split" -> splitSpot(target);
                    case "delete" -> deleteSelected();
                    case "unlink" -> unlink(target);
                    case "add_to" -> {
                        String into = arcTargetAt(target);
                        if (into != null) {
                            adding = into;
                            linkFrom = -1;
                            deselect();
                            placing = null;
                            steps.clear();
                            rebuildWidgets();
                        }
                    }
                    case "link" -> {
                        selectOnly(target.name(), target.index());
                        startLinking(target.name());
                    }
                    case "faces" -> facesPressed();
                    default -> centreSelected();
                }
            }
            return true;
        }
        if (button == 0 && view != null) {
            double[] arrow = arrowAt(mouseX, mouseY);
            Handle near = arrow == null ? null : handleAt(mouseX, mouseY);
            if (arrow != null && near != null) {
                var origin = model.layout().getCharGrid().getControllerPos();
                double[] sa = view.project(arrow[4] - origin.getX(), arrow[5] - origin.getY(), arrow[6] - origin.getZ());
                double[] sh = view.project(near.x() - origin.getX(), near.y() - origin.getY(), near.z() - origin.getZ());
                if (sa != null && sh != null && (sh[0] - mouseX) * (sh[0] - mouseX) + (sh[1] - mouseY) * (sh[1] - mouseY)
                        < (sa[0] - mouseX) * (sa[0] - mouseX) + (sa[1] - mouseY) * (sa[1] - mouseY)) {
                    arrow = null;
                }
            }
            if (arrow != null) {
                setFocused(null);
                arrowLayer = (int) arrow[0];
                arrowMoved = false;
                return true;
            }
        }
        if (button == 0 && view != null) {
            Handle h = handleAt(mouseX, mouseY);
            if (h != null) {
                setFocused(null);
                if (hasControlDown()) {
                    toggleSelect(h);
                    rebuildWidgets();
                    swallowRelease = true;
                    return true;
                }
                if (!(h.name().equals(handleName) && h.index() == handleIndex) || !group.isEmpty()) {
                    selectOnly(h.name(), h.index());
                    rebuildWidgets();
                }
                dragging = true;
                dragMoved = false;
                return true;
            }
        }
        marqueeArmed = button == 0 && view != null && viewport.contains((int) mouseX, (int) mouseY) && selectedId() != null && menu == null
                && placing == null && adding == null && pickingBlocks == null && !facePicking && !handles().isEmpty();
        int startsBefore = placingStarts;
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        dragFromLibrary = button == 0 && !session.collapsed && mouseX < PANEL + 8 && placing != null && placingStarts != startsBefore;
        deleteArmed = null;
        if (focusLater != null) {
            setFocused(focusLater);
            focusLater = null;
        } else if (handled && getFocused() instanceof Button) {
            setFocused(null);
        }
        return handled;
    }

    private int dropRows() {
        int fit = Math.max(1, (height - 48) / 16);
        dropScroll = Math.max(0, Math.min(dropScroll, session.effects.size() - fit));
        return Math.min(fit, session.effects.size() - dropScroll);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int step = (int) Math.signum(scrollY);
        if (dropdown && mouseX >= width - PANEL - 6) {
            dropScroll = Math.max(0, dropScroll - step);
            return true;
        }
        if (!session.collapsed && mouseX < PANEL + 8 && mouseY >= 42) {
            int next = Math.max(0, Math.min(libScrollMax, libScroll - step));
            if (next != libScroll) {
                libScroll = next;
                rebuildWidgets();
            }
            return true;
        }
        if (!session.collapsed && session.tab == 0 && !spots.isEmpty() && mouseX >= width - PANEL - 8 && mouseY >= spotsY) {
            int next = Math.max(0, Math.min(spotScrollMax, spotScroll - (int) Math.signum(scrollY)));
            if (next != spotScroll) {
                spotScroll = next;
                rebuildWidgets();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (arrowLayer >= 0 && button == 0) {
            dragArrow(mouseX, mouseY);
            return true;
        }
        if (dragging && button == 0) {
            drag(mouseX, mouseY);
            return true;
        }
        if (marqueeArmed && button == 0) {
            marqueeX = mouseX;
            marqueeY = mouseY;
            if (!marquee && Math.abs(mouseX - pressX) + Math.abs(mouseY - pressY) >= 4) {
                marquee = true;
            }
            if (marquee) {
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (swallowRelease) {
            swallowRelease = false;
            return true;
        }
        if (dragFromLibrary && button == 0) {
            dragFromLibrary = false;
            boolean moved = Math.abs(mouseX - pressX) + Math.abs(mouseY - pressY) >= 4;
            if (moved && placing != null && view != null) {
                ResourceLocation id = selectedId();
                if (placing.startsWith(PART) && id != null && !session.collapsed && mouseX >= width - PANEL - 6) {
                    String name = placing.substring(PART.length());
                    placing = null;
                    held = null;
                    steps.clear();
                    addPart(id, name);
                    return true;
                }
                if (viewport.contains((int) mouseX, (int) mouseY)) {
                    BlockHitResult hit = view.pick(mouseX, mouseY);
                    if (hit != null) {
                        click(hit);
                    }
                    return true;
                }
            }
        }
        if (marquee && button == 0) {
            marquee = false;
            marqueeArmed = false;
            selectBox(pressX, pressY, mouseX, mouseY, hasControlDown());
            return true;
        }
        if (overOverlay(mouseX, mouseY)) {
            arrowLayer = -1;
            marqueeArmed = false;
            if (dragging) {
                dragging = false;
                if (dragMoved) {
                    rebuildWidgets();
                }
            }
            return super.mouseReleased(mouseX, mouseY, button);
        }
        if (arrowLayer >= 0 && button == 0) {
            arrowLayer = -1;
            return true;
        }
        marqueeArmed = false;
        if (dragging && button == 0) {
            dragging = false;
            if (dragMoved) {
                rebuildWidgets();
            }
            return true;
        }
        if (button == 1 && view != null && Math.abs(mouseX - pressX) + Math.abs(mouseY - pressY) < 4) {
            Handle h = handleAt(mouseX, mouseY);
            if (h != null && menuEntries(h).length > 0) {
                if (!isSelected(h.name(), h.index())) {
                    selectOnly(h.name(), h.index());
                }
                rebuildWidgets();
                menu = h;
                menuX = (int) mouseX;
                menuY = (int) mouseY;
                return true;
            }
        }
        if (button == 0 && view != null && placing == null && adding == null && viewport.contains((int) mouseX, (int) mouseY)
                && !facePicking && !hasControlDown() && Math.abs(mouseX - pressX) + Math.abs(mouseY - pressY) < 4 && handleName != null) {
            deselect();
            rebuildWidgets();
        }
        if (button == 0 && view != null && viewport.contains((int) mouseX, (int) mouseY)
                && Math.abs(mouseX - pressX) + Math.abs(mouseY - pressY) < 4) {
            BlockHitResult hit = view.pick(mouseX, mouseY);
            if (placing != null) {
                if (hit != null) {
                    click(hit);
                }
            } else if (pickingBlocks != null) {
                if (hit != null) {
                    toggleBlock(hit.getBlockPos());
                }
            } else if (facePicking) {
                if (hit != null) {
                    toggleFace(hit);
                }
            } else if (adding != null) {
                Handle target = linking() ? handleAt(mouseX, mouseY, adding) : null;
                if (target != null) {
                    connect(target);
                } else if (hit != null) {
                    addPoint(hit);
                }
            } else if (!hasControlDown()) {
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
        boolean typing = getFocused() instanceof EditBox;
        if (typing && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            if (getFocused() == coords) {
                applyCoords();
            } else if (getFocused() == presetNameBox) {
                savePreset();
            } else if (getFocused() == renameBox) {
                applyRename();
            } else {
                setFocused(null);
            }
            return true;
        }
        if (typing && keyCode == 256) {
            setFocused(null);
            if (saving || renaming != null) {
                saving = false;
                renaming = null;
                rebuildWidgets();
                setFocused(null);
            }
            return true;
        }
        if (dropdown && keyCode == 256) {
            dropdown = false;
            return true;
        }
        if (menu != null && keyCode == 256) {
            menu = null;
            return true;
        }
        if (!typing && hasControlDown() && keyCode == GLFW.GLFW_KEY_Z) {
            if (hasShiftDown()) {
                redo();
            } else {
                undo();
            }
            return true;
        }
        if (!typing && hasControlDown() && keyCode == GLFW.GLFW_KEY_Y) {
            redo();
            return true;
        }
        if (!typing && !facePicking && (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) && handle() != null) {
            deleteSelected();
            return true;
        }
        if (typing) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            togglePanels();
            return true;
        }
        if (facePicking && keyCode == 256) {
            cancelFaces();
            return true;
        }
        if (facePicking && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            applyFaces();
            return true;
        }
        if ((keyCode == 256 || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && pickingBlocks != null) {
            pickingBlocks = null;
            rebuildWidgets();
            return true;
        }
        if (keyCode == 256 && (placing != null || adding != null)) {
            placing = null;
            adding = null;
            linkFrom = -1;
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
            String prefix = index >= 6 ? "gui.mm.effect_studio.working_" : index >= 3 ? "gui.mm.effect_studio.idle_" : "gui.mm.effect_studio.";
            setMessage(Component.translatable(prefix + name, String.format("%.2fx", factor(value))));
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            remember();
            super.onClick(mouseX, mouseY);
        }

        @Override
        protected void applyValue() {
            values[index] = value;
            rescale(id);
        }
    }
}

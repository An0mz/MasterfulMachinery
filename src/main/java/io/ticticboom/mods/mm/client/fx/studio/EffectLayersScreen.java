package io.ticticboom.mods.mm.client.fx.studio;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.NativeImage;
import io.ticticboom.mods.mm.client.fx.FxManager;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.lwjgl.glfw.GLFW;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class EffectLayersScreen extends Screen {
    private static final List<String> TYPES = List.copyOf(FxSchema.LAYERS.keySet());
    private static final int ROW = 22;
    private static final int CELL = 44;
    private static final int[] SWATCHES = {0xC01818, 0xFF8A1A, 0xFFD040, 0x3AFF7A, 0x40E0FF, 0x3060FF, 0x8020FF, 0xFFFFFF};
    private static boolean expert;

    private record Row(@Nullable FxSchema.Field field, int hidden) {
    }

    private final EffectStudio.Session session;
    private final Screen parent;
    private final ResourceLocation id;
    private final JsonObject json;
    private List<ResourceLocation> textures;
    @Nullable
    private List<ResourceLocation> allTextures;
    @Nullable
    private List<ResourceLocation> pickerCache;
    private final EditBox pickerSearch;
    @Nullable
    private List<ResourceLocation> fluidTextures;
    private int pickerMode;
    private boolean reloadingTextures;
    @Nullable
    private String folderError;
    private final Map<String, JsonObject> presets = EffectStudio.library("mm_fx_parts");
    private final List<String> presetNames = new ArrayList<>(presets.keySet());
    private final Map<ResourceLocation, int[]> textureInfo = new HashMap<>();
    private final List<Row> rows = new ArrayList<>();
    private final Map<String, List<Button>> resets = new HashMap<>();
    private final List<Button> layerButtons = new ArrayList<>();
    private int selected = -1;
    private int scroll;
    private int listScroll;
    private int listFit = 1;
    private boolean showMore;
    private boolean pickerOpen;
    private int pickerScroll;
    private boolean chooserOpen;
    private String chooserTitle = "";
    private List<String[]> chooserEntries = List.of();
    private Consumer<Integer> chooserAction = i -> {
    };
    private int chooserScroll;
    private int panelX;
    private int ctrlX;
    private int ctrlW;
    private int rowsTop;
    private int rowsFit;
    @Nullable
    private String error;

    public EffectLayersScreen(EffectStudio.Session session, Screen parent, ResourceLocation id) {
        super(Component.literal(id.toString()));
        this.session = session;
        this.parent = parent;
        this.id = id;
        JsonObject source = FxManager.source(id);
        this.json = session.edited.computeIfAbsent(id, x -> source == null ? new JsonObject() : source.deepCopy());
        if (!json.has("layers")) {
            json.add("layers", new JsonArray());
        }
        this.textures = listTextures("textures/fx");
        this.pickerSearch = new EditBox(Minecraft.getInstance().font, 0, 0, 100, 14, Component.empty());
        this.pickerSearch.setMaxLength(60);
        this.pickerSearch.setResponder(value -> {
            pickerCache = null;
            pickerScroll = 0;
        });
    }

    private static List<ResourceLocation> listTextures(String folder) {
        var resources = Minecraft.getInstance().getResourceManager();
        return resources.listResources(folder, path -> path.getPath().endsWith(".png")).keySet().stream().sorted().toList();
    }

    private static List<ResourceLocation> listFluidTextures(List<ResourceLocation> all) {
        var resources = Minecraft.getInstance().getResourceManager();
        var found = new TreeSet<ResourceLocation>();
        for (var fluid : BuiltInRegistries.FLUID) {
            try {
                var ext = IClientFluidTypeExtensions.of(fluid);
                for (ResourceLocation sprite : new ResourceLocation[]{ext.getStillTexture(), ext.getFlowingTexture()}) {
                    if (sprite == null) {
                        continue;
                    }
                    ResourceLocation file = ResourceLocation.fromNamespaceAndPath(sprite.getNamespace(), "textures/" + sprite.getPath() + ".png");
                    if (resources.getResource(file).isPresent()) {
                        found.add(file);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        for (ResourceLocation texture : all) {
            String path = texture.getPath();
            String name = path.substring(path.lastIndexOf('/') + 1, path.length() - 4);
            if (name.endsWith("_still") || name.endsWith("_flow") || name.endsWith("_flowing") || path.contains("/fluid/") || path.contains("/fluids/")) {
                found.add(texture);
            }
        }
        return List.copyOf(found);
    }

    private List<ResourceLocation> pickerList() {
        if (pickerCache == null) {
            if (pickerMode > 0 && allTextures == null) {
                allTextures = listTextures("textures");
            }
            if (pickerMode == 1 && fluidTextures == null) {
                fluidTextures = listFluidTextures(allTextures);
            }
            List<ResourceLocation> source = pickerMode == 0 ? textures : pickerMode == 1 ? fluidTextures : allTextures;
            String needle = pickerSearch.getValue().trim().toLowerCase(Locale.ROOT);
            pickerCache = needle.isEmpty() ? source : source.stream().filter(t -> t.toString().toLowerCase(Locale.ROOT).contains(needle)).toList();
        }
        return pickerCache;
    }

    private void openPicker() {
        pickerOpen = true;
        pickerScroll = 0;
        pickerCache = null;
        folderError = null;
        pickerSearch.setFocused(true);
    }

    private void refreshTextures() {
        textures = listTextures("textures/fx");
        allTextures = null;
        fluidTextures = null;
        pickerCache = null;
        textureInfo.clear();
    }

    private void openTextureFolder() {
        Path dir = FMLPaths.GAMEDIR.get().resolve("kubejs").resolve("assets").resolve(session.structure.getNamespace())
                .resolve("textures").resolve("fx");
        try {
            Files.createDirectories(dir);
            Util.getPlatform().openFile(dir.toFile());
        } catch (Exception e) {
            folderError = e.getMessage() != null ? e.getMessage() : e.toString();
        }
    }

    private void reloadTextures() {
        if (reloadingTextures) {
            return;
        }
        reloadingTextures = true;
        Minecraft mc = Minecraft.getInstance();
        mc.reloadResourcePacks().thenRun(() -> mc.execute(() -> {
            try {
                session.edited.forEach((editedId, edited) -> {
                    try {
                        FxManager.preview(editedId, edited);
                    } catch (Exception ignored) {
                    }
                });
                refreshTextures();
            } finally {
                reloadingTextures = false;
            }
        }));
    }

    private JsonArray layers() {
        return json.getAsJsonArray("layers");
    }

    private static String type(JsonObject layer) {
        return layer.has("type") ? layer.get("type").getAsString() : "?";
    }

    private JsonObject target() {
        return selected < 0 || !layers().get(selected).isJsonObject() ? json : layers().get(selected).getAsJsonObject();
    }

    private List<FxSchema.Field> fields() {
        return selected < 0 ? FxSchema.EFFECT : FxSchema.LAYERS.getOrDefault(type(target()), List.of());
    }

    private static String typeName(String type) {
        String key = "gui.mm.effect_studio.layer." + type;
        return I18n.exists(key) ? I18n.get(key) : type;
    }

    private String layerName(int index) {
        if (!layers().get(index).isJsonObject()) {
            return "?";
        }
        JsonObject layer = layers().get(index).getAsJsonObject();
        if (layer.has("name") && layer.get("name").isJsonPrimitive() && !layer.get("name").getAsString().isBlank()) {
            return layer.get("name").getAsString();
        }
        return typeName(type(layer));
    }

    private static String pretty(String text) {
        String spaced = text.replace('_', ' ');
        return spaced.isEmpty() ? spaced : Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    @Override
    protected void init() {
        panelX = 160;
        ctrlX = panelX + Math.max(70, Math.min(134, width - panelX - 36 - 120));
        ctrlW = Math.max(60, width - 36 - ctrlX);
        rowsTop = 44;
        rowsFit = Math.max(1, (infoY() - 4 - rowsTop) / ROW);
        resets.clear();
        buildList();
        if (expert) {
            buildRaw();
        } else {
            buildRows();
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.mm.effect_studio.back"), b ->
                minecraft.setScreen(parent)).bounds(10, height - 26, 80, 20).build());
        addRenderableWidget(Button.builder(Component.translatable(expert ? "gui.mm.effect_studio.expert_on" : "gui.mm.effect_studio.expert_off"), b -> {
            expert = !expert;
            scroll = 0;
            rebuildWidgets();
        }).tooltip(Tooltip.create(Component.translatable("gui.mm.effect_studio.expert_tip"))).bounds(width - 134, height - 26, 124, 20).build());
    }

    private int infoY() {
        return height - 68;
    }

    private void buildList() {
        layerButtons.clear();
        addRenderableWidget(Button.builder(Component.translatable("gui.mm.effect_studio.settings"), b -> select(-1))
                .bounds(10, 28, 140, 16).build());
        int y = height - 48;
        int partsY = presetNames.isEmpty() ? -1 : y;
        if (partsY >= 0) {
            y -= 18;
        }
        int layerY = y;
        y -= 22;
        int actionsY = selected >= 0 ? y : -1;
        int listBottom = (selected >= 0 ? y : layerY) - 4;
        listFit = Math.max(1, (listBottom - 46) / 18);
        listScroll = Math.max(0, Math.min(listScroll, layers().size() - listFit));
        int end = Math.min(layers().size(), listScroll + listFit);
        for (int i = listScroll; i < end; i++) {
            int index = i;
            int ry = 46 + (i - listScroll) * 18;
            boolean off = layerOff(i);
            addRenderableWidget(Button.builder(Component.literal(off ? "○" : "●").withStyle(off ? ChatFormatting.GRAY : ChatFormatting.GREEN), b -> toggleLayer(index))
                    .tooltip(Tooltip.create(Component.translatable("gui.mm.effect_studio.layer_on_tip"))).bounds(10, ry, 16, 16).build());
            String label = (i == selected ? "> " : "") + (i + 1) + ". " + font.plainSubstrByWidth(layerName(i), 96);
            layerButtons.add(addRenderableWidget(Button.builder(Component.literal(label).withStyle(off ? ChatFormatting.DARK_GRAY : ChatFormatting.WHITE), b -> select(index))
                    .bounds(28, ry, 122, 16).build()));
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.mm.effect_studio.add_layer"), b -> openLayerChooser())
                .bounds(10, layerY, 140, 16).build());
        if (partsY >= 0) {
            addRenderableWidget(Button.builder(Component.translatable("gui.mm.effect_studio.add_part"), b -> openPartChooser())
                    .bounds(10, partsY, 140, 16).build());
        }
        if (actionsY >= 0) {
            addRenderableWidget(Button.builder(Component.literal("^"), b -> move(-1))
                    .tooltip(Tooltip.create(Component.translatable("gui.mm.effect_studio.move_up"))).bounds(10, actionsY, 20, 16).build());
            addRenderableWidget(Button.builder(Component.literal("v"), b -> move(1))
                    .tooltip(Tooltip.create(Component.translatable("gui.mm.effect_studio.move_down"))).bounds(32, actionsY, 20, 16).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.mm.effect_studio.copy"), b -> {
                layers().add(target().deepCopy());
                select(layers().size() - 1);
                apply();
            }).tooltip(Tooltip.create(Component.translatable("gui.mm.effect_studio.copy_tip"))).bounds(54, actionsY, 44, 16).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.mm.effect_studio.remove"), b -> {
                layers().remove(selected);
                select(-1);
                apply();
            }).tooltip(Tooltip.create(Component.translatable("gui.mm.effect_studio.delete_layer_tip"))).bounds(100, actionsY, 50, 16).build());
        }
    }

    private void refreshLayerLabel() {
        int slot = selected - listScroll;
        if (selected >= 0 && slot >= 0 && slot < layerButtons.size() && layerButtons.get(slot) != null) {
            boolean off = layerOff(selected);
            layerButtons.get(slot).setMessage(Component.literal("> " + (selected + 1) + ". " + font.plainSubstrByWidth(layerName(selected), 96))
                    .withStyle(off ? ChatFormatting.DARK_GRAY : ChatFormatting.WHITE));
        }
    }

    private boolean layerOff(int index) {
        return layers().get(index).isJsonObject() && layers().get(index).getAsJsonObject().has("off")
                && layers().get(index).getAsJsonObject().get("off").getAsBoolean();
    }

    private void toggleLayer(int index) {
        if (!layers().get(index).isJsonObject()) {
            return;
        }
        JsonObject layer = layers().get(index).getAsJsonObject();
        if (layerOff(index)) {
            layer.remove("off");
        } else {
            layer.addProperty("off", true);
        }
        apply();
        rebuildWidgets();
    }

    private void openChooser(String title, List<String[]> entries, Consumer<Integer> action) {
        chooserOpen = true;
        chooserTitle = title;
        chooserEntries = entries;
        chooserAction = action;
        chooserScroll = 0;
    }

    private void openLayerChooser() {
        var entries = new ArrayList<String[]>();
        for (String layerType : TYPES) {
            String tipKey = "gui.mm.effect_studio.layer." + layerType + ".tip";
            entries.add(new String[]{typeName(layerType), I18n.exists(tipKey) ? I18n.get(tipKey) : ""});
        }
        openChooser(I18n.get("gui.mm.effect_studio.choose_layer"), entries, i -> addLayer(TYPES.get(i)));
    }

    private void openPartChooser() {
        var entries = new ArrayList<String[]>();
        for (String part : presetNames) {
            String nameKey = "gui.mm.effect_studio.part." + part;
            String tipKey = nameKey + ".tip";
            entries.add(new String[]{I18n.exists(nameKey) ? I18n.get(nameKey) : pretty(part), I18n.exists(tipKey) ? I18n.get(tipKey) : ""});
        }
        openChooser(I18n.get("gui.mm.effect_studio.choose_part"), entries, i -> {
            for (JsonElement layer : presets.get(presetNames.get(i)).getAsJsonArray("layers")) {
                layers().add(layer.deepCopy());
            }
            select(layers().size() - 1);
            apply();
        });
    }

    private void addLayer(String layerType) {
        var layer = new JsonObject();
        layer.addProperty("type", layerType);
        ResourceLocation texture = defaultTexture(layerType);
        if (texture != null) {
            layer.addProperty("texture", texture.toString());
        }
        layers().add(layer);
        select(layers().size() - 1);
        apply();
    }

    @Nullable
    private ResourceLocation defaultTexture(String layerType) {
        String word = switch (layerType) {
            case "stream", "drip", "drops" -> "flow";
            case "pool", "ripple" -> "pool";
            case "seep" -> "white";
            case "emitter" -> "spark";
            case "beam" -> "beam";
            default -> "glow";
        };
        for (ResourceLocation texture : textures) {
            if (texture.getPath().contains(word)) {
                return texture;
            }
        }
        return textures.isEmpty() ? null : textures.get(0);
    }

    private void buildRows() {
        rows.clear();
        List<FxSchema.Field> fields = fields();
        int hidden = 0;
        for (FxSchema.Field f : fields) {
            if (f.more()) {
                hidden++;
            }
        }
        for (FxSchema.Field f : fields) {
            if (!f.more()) {
                rows.add(new Row(f, 0));
            }
        }
        if (hidden > 0) {
            rows.add(new Row(null, hidden));
            if (showMore) {
                for (FxSchema.Field f : fields) {
                    if (f.more()) {
                        rows.add(new Row(f, 0));
                    }
                }
            }
        }
        scroll = Math.max(0, Math.min(scroll, rows.size() - rowsFit));
        int end = Math.min(rows.size(), scroll + rowsFit);
        for (int i = scroll; i < end; i++) {
            addRow(rows.get(i), rowsTop + (i - scroll) * ROW);
        }
    }

    private void addRow(Row row, int y) {
        FxSchema.Field f = row.field();
        if (f == null) {
            addRenderableWidget(Button.builder(Component.translatable(showMore ? "gui.mm.effect_studio.fewer" : "gui.mm.effect_studio.more", row.hidden()), b -> {
                showMore = !showMore;
                rebuildWidgets();
            }).bounds(panelX, y, width - 10 - panelX, 18).build());
            return;
        }
        switch (f.kind()) {
            case TOGGLE -> addToggle(f, y);
            case NUMBER -> addNumber(f, y);
            case PAIR -> addPair(f, y);
            case COLOR -> addColor(f, y);
            case TEXTURE -> addTexture(f, y);
            case CHOICE -> addChoice(f, y, f.options());
            case ANCHOR -> addChoice(f, y, anchorOptions(f));
            case TEXT -> addText(f, y);
        }
        if (f.kind() != FxSchema.Kind.TEXTURE) {
            var reset = Button.builder(Component.literal("↺"), b -> {
                target().remove(f.key());
                apply();
                rebuildWidgets();
            }).tooltip(Tooltip.create(Component.translatable("gui.mm.effect_studio.reset"))).bounds(width - 28, y, 18, 18).build();
            reset.active = target().has(f.key());
            resets.computeIfAbsent(f.key(), k -> new ArrayList<>()).add(reset);
            addRenderableWidget(reset);
        }
    }

    private void touched(String key) {
        for (Button b : resets.getOrDefault(key, List.of())) {
            b.active = true;
        }
    }

    private Component toggleText(boolean on) {
        return Component.translatable(on ? "gui.mm.effect_studio.toggle_on" : "gui.mm.effect_studio.toggle_off")
                .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY);
    }

    private void addToggle(FxSchema.Field f, int y) {
        boolean[] shown = {toggleValue(f)};
        addRenderableWidget(Button.builder(toggleText(shown[0]), b -> {
            shown[0] = !shown[0];
            boolean value = f.invert() != shown[0];
            if (f.invert() && !value) {
                target().remove(f.key());
            } else {
                target().addProperty(f.key(), value);
            }
            b.setMessage(toggleText(shown[0]));
            if (target().has(f.key())) {
                touched(f.key());
            }
            apply();
        }).bounds(ctrlX, y, 70, 18).build());
    }

    private boolean toggleValue(FxSchema.Field f) {
        boolean value = Boolean.parseBoolean(f.def());
        JsonElement e = target().get(f.key());
        if (e != null && e.isJsonPrimitive()) {
            try {
                value = e.getAsBoolean();
            } catch (Exception ignored) {
            }
        }
        return f.invert() != value;
    }

    private void addNumber(FxSchema.Field f, int y) {
        double[] parts = numberParts(f);
        double current = parts[Math.min(f.part() < 0 ? 0 : f.part(), parts.length - 1)];
        addRenderableWidget(new NumberSlider(ctrlX, y, Math.min(ctrlW, 190), 18, "", f, current, value -> {
            double[] now = numberParts(f);
            now[f.part() < 0 ? 0 : f.part()] = value;
            writeNumberParts(f, now);
            touched(f.key());
            apply();
        }));
    }

    private void addPair(FxSchema.Field f, int y) {
        double[] iw = idleWorking(f);
        int half = (Math.min(ctrlW, 190) - 4) / 2;
        for (int k = 0; k < 2; k++) {
            int which = k;
            addRenderableWidget(new NumberSlider(ctrlX + k * (half + 4), y, half, 18,
                    I18n.get(k == 0 ? "gui.mm.effect_studio.pair_idle" : "gui.mm.effect_studio.pair_working"), f, iw[k], value -> {
                double[] now = idleWorking(f);
                now[which] = value;
                if (now[0] == now[1]) {
                    target().add(f.key(), number(now[0]));
                } else {
                    var pair = new JsonObject();
                    pair.add("idle", number(now[0]));
                    pair.add("working", number(now[1]));
                    target().add(f.key(), pair);
                }
                touched(f.key());
                apply();
            }));
        }
    }

    private double[] idleWorking(FxSchema.Field f) {
        double def = parse(f.def(), 0);
        double[] result = {def, def};
        JsonElement e = target().get(f.key());
        try {
            if (e != null && e.isJsonObject()) {
                JsonObject o = e.getAsJsonObject();
                result[0] = o.has("idle") ? o.get("idle").getAsDouble() : def;
                result[1] = o.has("working") ? o.get("working").getAsDouble() : def;
            } else if (e != null && e.isJsonPrimitive()) {
                result[0] = e.getAsDouble();
                result[1] = result[0];
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private double[] numberParts(FxSchema.Field f) {
        int n = Math.max(1, f.count());
        String[] defs = f.def().split(",");
        double[] result = new double[n];
        JsonElement e = target().get(f.key());
        for (int i = 0; i < n; i++) {
            result[i] = parse(defs[Math.min(i, defs.length - 1)], 0);
            try {
                if (e != null && e.isJsonArray() && e.getAsJsonArray().size() > i) {
                    result[i] = e.getAsJsonArray().get(i).getAsDouble();
                } else if (e != null && e.isJsonPrimitive()) {
                    result[i] = e.getAsDouble();
                }
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    private void writeNumberParts(FxSchema.Field f, double[] values) {
        if (values.length == 1 || (values.length == 2 && values[0] == values[1])) {
            target().add(f.key(), number(values[0]));
            return;
        }
        var array = new JsonArray();
        for (double v : values) {
            array.add(number(v));
        }
        target().add(f.key(), array);
    }

    private static double parse(String text, double fallback) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static JsonPrimitive number(double value) {
        double rounded = Math.round(value * 1_000_000) / 1_000_000.0;
        if (Math.abs(rounded - Math.rint(rounded)) < 1e-9) {
            return new JsonPrimitive((long) Math.rint(rounded));
        }
        return new JsonPrimitive(rounded);
    }

    private void addColor(FxSchema.Field f, int y) {
        int swatches = Math.max(0, Math.min(SWATCHES.length, (ctrlW - 22 - 58) / 13));
        int x = ctrlX + 20;
        for (int i = 0; i < swatches; i++) {
            int rgb = SWATCHES[i];
            addRenderableWidget(new Swatch(x, y + 3, rgb, () -> {
                writeColor(f, rgb);
                touched(f.key());
                apply();
                rebuildWidgets();
            }));
            x += 13;
        }
        var box = new EditBox(font, ctrlX + 20 + swatches * 13 + 2, y, 56, 18, Component.literal(f.key()));
        box.setMaxLength(7);
        box.setFilter(text -> text.matches("#?[0-9a-fA-F]{0,6}"));
        box.setValue(String.format(Locale.ROOT, "#%06x", colorValue(f)));
        box.setResponder(text -> {
            int rgb = hex(text);
            if (rgb >= 0 && (text.length() == 6 || text.length() == 7)) {
                writeColor(f, rgb);
                touched(f.key());
                apply();
            }
        });
        addRenderableWidget(box);
    }

    private static int hex(String text) {
        String digits = text.startsWith("#") ? text.substring(1) : text;
        if (digits.length() != 6) {
            return -1;
        }
        try {
            return Integer.parseInt(digits, 16);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String[] colorParts(FxSchema.Field f) {
        int n = Math.max(1, f.count());
        String[] defs = f.def().split(",");
        String[] result = new String[n];
        JsonElement e = target().get(f.key());
        for (int i = 0; i < n; i++) {
            result[i] = defs[Math.min(i, defs.length - 1)].trim();
            try {
                if (e != null && e.isJsonArray() && e.getAsJsonArray().size() > i) {
                    result[i] = e.getAsJsonArray().get(i).getAsString();
                } else if (e != null && e.isJsonPrimitive()) {
                    result[i] = e.getAsString();
                }
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    private int colorValue(FxSchema.Field f) {
        String[] parts = colorParts(f);
        int rgb = hex(parts[Math.min(f.part() < 0 ? 0 : f.part(), parts.length - 1)]);
        return rgb >= 0 ? rgb : 0xFFFFFF;
    }

    private void writeColor(FxSchema.Field f, int rgb) {
        String[] parts = colorParts(f);
        parts[f.part() < 0 ? 0 : f.part()] = String.format(Locale.ROOT, "#%06x", rgb);
        if (parts.length == 1 || parts[0].equalsIgnoreCase(parts[1])) {
            target().addProperty(f.key(), parts[0]);
            return;
        }
        var array = new JsonArray();
        for (String part : parts) {
            array.add(part);
        }
        target().add(f.key(), array);
    }

    private void addTexture(FxSchema.Field f, int y) {
        String current = target().has("texture") && target().get("texture").isJsonPrimitive() ? target().get("texture").getAsString() : "";
        Component label = current.isEmpty() ? Component.translatable("gui.mm.effect_studio.texture_none") : Component.literal(font.plainSubstrByWidth(textureName(current), ctrlW - 28));
        addRenderableWidget(Button.builder(label, b -> openPicker()).tooltip(Tooltip.create(Component.translatable("gui.mm.effect_studio.texture_pick"))).bounds(ctrlX + 22, y, Math.min(ctrlW - 22, 168), 18).build());
    }

    private static String textureName(String texture) {
        ResourceLocation rl = ResourceLocation.tryParse(texture);
        if (rl == null) {
            return texture;
        }
        String path = rl.getPath();
        int slash = path.lastIndexOf('/');
        String name = path.substring(slash + 1);
        if (name.endsWith(".png")) {
            name = name.substring(0, name.length() - 4);
        }
        return rl.getNamespace().equals("mm") ? name : rl.getNamespace() + ": " + name;
    }

    private List<String> anchorOptions(FxSchema.Field f) {
        var names = new ArrayList<String>();
        if (session.selected >= 0 && session.selected < session.effects.size()) {
            JsonObject effect = session.effects.get(session.selected);
            if (effect.has("anchors") && effect.get("anchors").isJsonObject()) {
                Set<String> kinds = anchorKinds(f);
                for (var entry : effect.getAsJsonObject("anchors").entrySet()) {
                    if (kinds.contains(anchorKind(entry.getValue()))) {
                        names.add(entry.getKey());
                    }
                }
            }
        }
        String current = choiceValue(f);
        if (!current.isEmpty() && !names.contains(current)) {
            names.add(0, current);
        }
        if (names.isEmpty()) {
            names.add(f.def());
        }
        return names;
    }

    private static String anchorKind(JsonElement anchor) {
        if (anchor.isJsonPrimitive() && anchor.getAsJsonPrimitive().isNumber()) {
            return "height";
        }
        return anchor.isJsonArray() ? "point" : "area";
    }

    private Set<String> anchorKinds(FxSchema.Field f) {
        String layer = type(target());
        return switch (layer + "." + f.key()) {
            case "stream.to", "drip.to", "pool.to" -> Set.of("height", "point");
            case "seep.top" -> Set.of("height");
            case "glow.at" -> target().has("mode") && "halo".equals(choiceMode()) ? Set.of("point") : Set.of("area");
            default -> Set.of("point");
        };
    }

    private String choiceMode() {
        JsonElement mode = target().get("mode");
        return mode != null && mode.isJsonPrimitive() ? mode.getAsString() : "faces";
    }

    private String choiceValue(FxSchema.Field f) {
        JsonElement e = target().get(f.key());
        if (e == null || e.isJsonNull()) {
            return f.def();
        }
        return e.isJsonPrimitive() ? e.getAsString() : text(e);
    }

    private Component optionText(FxSchema.Field f, String option) {
        String key = "gui.mm.effect_studio.opt." + f.key() + "." + option;
        return I18n.exists(key) ? Component.translatable(key) : Component.literal(option);
    }

    private void addChoice(FxSchema.Field f, int y, List<String> options) {
        String[] current = {choiceValue(f)};
        addRenderableWidget(Button.builder(optionText(f, current[0]), b -> {
            int next = options.indexOf(current[0]) + 1;
            current[0] = options.get(next % options.size());
            target().addProperty(f.key(), current[0]);
            b.setMessage(optionText(f, current[0]));
            touched(f.key());
            apply();
        }).bounds(ctrlX, y, Math.min(ctrlW, 168), 18).build());
    }

    private void addText(FxSchema.Field f, int y) {
        var box = new EditBox(font, ctrlX, y, Math.min(ctrlW, 168), 18, Component.literal(f.key()));
        box.setMaxLength(512);
        box.setValue(target().has(f.key()) ? text(target().get(f.key())) : "");
        if (!f.def().isEmpty()) {
            box.setHint(Component.literal(f.def()).withStyle(ChatFormatting.DARK_GRAY));
        }
        box.setResponder(value -> {
            if (f.key().equals("name")) {
                if (value.isBlank()) {
                    target().remove("name");
                } else {
                    target().addProperty("name", value);
                }
                refreshLayerLabel();
            } else if (f.key().equals("palette")) {
                var colours = new JsonArray();
                for (String part : value.split(",")) {
                    if (!part.isBlank()) {
                        colours.add(part.trim());
                    }
                }
                if (colours.isEmpty()) {
                    target().remove("palette");
                } else {
                    target().add("palette", colours);
                }
            } else {
                JsonElement parsed = parse(value);
                if (parsed == null) {
                    target().remove(f.key());
                } else {
                    target().add(f.key(), parsed);
                }
            }
            touched(f.key());
            apply();
        });
        addRenderableWidget(box);
    }

    private void buildRaw() {
        String[] keys = FxSchema.rawKeys(fields());
        scroll = Math.max(0, Math.min(scroll, keys.length / 2 - rowsFit));
        int ry = rowsTop;
        for (int k = scroll * 2; k < keys.length && ry < rowsTop + rowsFit * ROW; k += 2) {
            String key = keys[k];
            var box = new EditBox(font, ctrlX, ry, ctrlW, 18, Component.literal(key));
            box.setMaxLength(512);
            box.setValue(target().has(key) ? text(target().get(key)) : "");
            box.setHint(Component.literal(keys[k + 1]).withStyle(ChatFormatting.DARK_GRAY));
            box.setResponder(value -> {
                JsonElement parsed = parse(value);
                if (parsed == null) {
                    target().remove(key);
                } else {
                    target().add(key, parsed);
                }
                apply();
            });
            addRenderableWidget(box);
            if (key.equals("texture") && !textures.isEmpty()) {
                addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                    int next = (textures.indexOf(ResourceLocation.tryParse(box.getValue())) + 1) % textures.size();
                    box.setValue(textures.get(next).toString());
                }).bounds(ctrlX + ctrlW + 2, ry, 16, 18).build());
            }
            ry += ROW;
        }
    }

    private void select(int index) {
        if (index >= 0 && index < listScroll) {
            listScroll = index;
        } else if (index >= listScroll + listFit) {
            listScroll = index - listFit + 1;
        }
        selected = index;
        scroll = 0;
        showMore = false;
        rebuildWidgets();
    }

    private void move(int by) {
        int to = selected + by;
        if (to < 0 || to >= layers().size()) {
            return;
        }
        JsonElement layer = layers().get(selected);
        layers().set(selected, layers().get(to));
        layers().set(to, layer);
        select(to);
        apply();
    }

    private void apply() {
        try {
            FxManager.preview(id, json);
            error = null;
        } catch (Exception e) {
            error = e.getMessage() == null ? e.toString() : e.getMessage();
        }
    }

    @Nullable
    static JsonElement parse(String text) {
        String value = text.trim();
        if (value.isEmpty()) {
            return null;
        }
        int slash = value.indexOf(" / ");
        if (slash > 0) {
            var pair = new JsonObject();
            pair.add("idle", parse(value.substring(0, slash)));
            pair.add("working", parse(value.substring(slash + 3)));
            return pair;
        }
        if (value.contains(",")) {
            var array = new JsonArray();
            for (String part : value.split(",")) {
                JsonElement element = parse(part);
                if (element != null) {
                    array.add(element);
                }
            }
            return array;
        }
        if (value.equals("true") || value.equals("false")) {
            return new JsonPrimitive(Boolean.parseBoolean(value));
        }
        try {
            return new JsonPrimitive(Double.parseDouble(value));
        } catch (NumberFormatException e) {
            return new JsonPrimitive(value);
        }
    }

    static String text(JsonElement value) {
        if (value.isJsonObject() && value.getAsJsonObject().has("idle")) {
            return text(value.getAsJsonObject().get("idle")) + " / " + text(value.getAsJsonObject().get("working"));
        }
        if (value.isJsonArray()) {
            var parts = new ArrayList<String>();
            value.getAsJsonArray().forEach(e -> parts.add(text(e)));
            return String.join(", ", parts);
        }
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            double d = value.getAsDouble();
            return d == Math.rint(d) ? Long.toString((long) d) : Double.toString(d);
        }
        return value.isJsonPrimitive() ? value.getAsString() : value.toString();
    }

    private static String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 1e-9) {
            return Long.toString((long) Math.rint(value));
        }
        String text = String.format(Locale.ROOT, "%.3f", value);
        while (text.endsWith("0")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private String fieldKey(FxSchema.Field f, String suffix) {
        String layer = selected < 0 ? "effect" : type(target());
        String part = f.count() > 1 ? "." + f.part() : "";
        String specific = "gui.mm.effect_studio.f." + layer + "." + f.key() + part + suffix;
        return I18n.exists(specific) ? specific : "gui.mm.effect_studio.f." + f.key() + part + suffix;
    }

    private String fieldName(FxSchema.Field f) {
        String key = fieldKey(f, "");
        return I18n.exists(key) ? I18n.get(key) : pretty(f.key());
    }

    private String fieldTip(FxSchema.Field f) {
        String key = fieldKey(f, ".tip");
        String tip = I18n.exists(key) ? I18n.get(key) : "";
        return f.kind() == FxSchema.Kind.PAIR ? (tip + " " + I18n.get("gui.mm.effect_studio.pair_tip")).trim() : tip;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (chooserOpen) {
            chooserScroll = Math.max(0, chooserScroll - (int) Math.signum(scrollY));
            return true;
        }
        if (pickerOpen) {
            pickerScroll = Math.max(0, pickerScroll - (int) Math.signum(scrollY));
            return true;
        }
        if (mouseX < panelX - 4 && mouseY > 44) {
            listScroll = Math.max(0, listScroll - (int) Math.signum(scrollY));
            rebuildWidgets();
            return true;
        }
        if (mouseX >= panelX) {
            scroll -= (int) Math.signum(scrollY);
            if (scroll < 0) {
                scroll = 0;
            }
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (chooserOpen) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                chooserOpen = false;
            }
            return true;
        }
        if (pickerOpen) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                pickerOpen = false;
            } else {
                pickerSearch.keyPressed(keyCode, scanCode, modifiers);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (pickerOpen) {
            pickerSearch.charTyped(codePoint, modifiers);
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private int[] chooserBox() {
        int w = Math.min(width - 40, 360);
        int h = Math.min(height - 60, 30 + chooserEntries.size() * 26);
        return new int[]{(width - w) / 2, (height - h) / 2, w, h};
    }

    private int chooserRows() {
        return Math.max(1, (chooserBox()[3] - 30) / 26);
    }

    private int chooserIndexAt(double mouseX, double mouseY) {
        int[] box = chooserBox();
        if (mouseX < box[0] + 6 || mouseX >= box[0] + box[2] - 6 || mouseY < box[1] + 22) {
            return -1;
        }
        int row = (int) (mouseY - box[1] - 22) / 26;
        return row >= chooserRows() ? -1 : chooserScroll + row;
    }

    private void renderChooser(GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.pose().pushPose();
        gfx.pose().translate(0, 0, 400);
        gfx.fill(0, 0, width, height, 0xA0000000);
        int[] box = chooserBox();
        gfx.fill(box[0] - 1, box[1] - 1, box[0] + box[2] + 1, box[1] + box[3] + 1, 0xFF8080A0);
        gfx.fill(box[0], box[1], box[0] + box[2], box[1] + box[3], 0xFF1E1E28);
        gfx.drawString(font, chooserTitle, box[0] + 8, box[1] + 6, 0xFFFFFF);
        int rowsShown = chooserRows();
        chooserScroll = Math.max(0, Math.min(chooserScroll, Math.max(0, chooserEntries.size() - rowsShown)));
        int hover = chooserIndexAt(mouseX, mouseY);
        for (int r = 0; r < rowsShown; r++) {
            int index = chooserScroll + r;
            if (index >= chooserEntries.size()) {
                break;
            }
            int y = box[1] + 22 + r * 26;
            gfx.fill(box[0] + 6, y, box[0] + box[2] - 6, y + 24, index == hover ? 0xFF3A4A66 : 0xFF2A2A38);
            gfx.drawString(font, font.plainSubstrByWidth(chooserEntries.get(index)[0], box[2] - 24), box[0] + 10, y + 3, 0xFFFFFF);
            gfx.drawString(font, font.plainSubstrByWidth(chooserEntries.get(index)[1], box[2] - 24), box[0] + 10, y + 13, 0xA0A0A0);
        }
        gfx.pose().popPose();
    }

    private static final int MAX_PREVIEW = 512;
    private static final int GRID_TOP = 42;
    private static final int GRID_FOOT = 30;
    private static final String[] PICKER_BUTTONS = {"effects", "fluids", "all", "folder", "reload"};

    private int[] pickerBox() {
        int w = Math.min(width - 40, 420);
        int h = Math.min(height - 40, 330);
        return new int[]{(width - w) / 2, (height - h) / 2, w, h};
    }

    private int pickerCols() {
        return Math.max(1, (pickerBox()[2] - 16) / CELL);
    }

    private int pickerRows() {
        return Math.max(1, (pickerBox()[3] - GRID_TOP - GRID_FOOT) / CELL);
    }

    private int[][] pickerButtonRects(int[] box) {
        int[][] rects = new int[PICKER_BUTTONS.length][];
        int x = box[0] + box[2] - 8;
        for (int k = PICKER_BUTTONS.length - 1; k >= 0; k--) {
            int w = font.width(Component.translatable("gui.mm.effect_studio.texture_btn." + PICKER_BUTTONS[k])) + 10;
            x -= w;
            rects[k] = new int[]{x, box[1] + 4, w, 14};
            x -= 2;
        }
        return rects;
    }

    private void pickerButtonPressed(int index) {
        switch (index) {
            case 0, 1, 2 -> {
                pickerMode = index;
                pickerCache = null;
                pickerScroll = 0;
            }
            case 3 -> openTextureFolder();
            default -> reloadTextures();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (chooserOpen) {
            int[] box = chooserBox();
            if (mouseX < box[0] || mouseX >= box[0] + box[2] || mouseY < box[1] || mouseY >= box[1] + box[3]) {
                chooserOpen = false;
                return true;
            }
            int index = chooserIndexAt(mouseX, mouseY);
            if (button == 0 && index >= 0 && index < chooserEntries.size()) {
                chooserOpen = false;
                chooserAction.accept(index);
            }
            return true;
        }
        if (!pickerOpen) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int[] box = pickerBox();
        if (mouseX < box[0] || mouseX >= box[0] + box[2] || mouseY < box[1] || mouseY >= box[1] + box[3]) {
            pickerOpen = false;
            return true;
        }
        if (button == 0) {
            int[][] rects = pickerButtonRects(box);
            for (int k = 0; k < rects.length; k++) {
                if (mouseX >= rects[k][0] && mouseX < rects[k][0] + rects[k][2] && mouseY >= rects[k][1] && mouseY < rects[k][1] + rects[k][3]) {
                    pickerButtonPressed(k);
                    return true;
                }
            }
            pickerSearch.mouseClicked(mouseX, mouseY, button);
            pickerSearch.setFocused(true);
        }
        int index = pickerIndexAt(mouseX, mouseY);
        List<ResourceLocation> list = pickerList();
        if (button == 0 && index >= 0 && index < list.size()) {
            target().addProperty("texture", list.get(index).toString());
            pickerOpen = false;
            apply();
            rebuildWidgets();
        }
        return true;
    }

    private int pickerIndexAt(double mouseX, double mouseY) {
        int[] box = pickerBox();
        int cols = pickerCols();
        int cx = (int) (mouseX - box[0] - 8) / CELL;
        int cy = (int) (mouseY - box[1] - GRID_TOP) / CELL;
        if (mouseX < box[0] + 8 || mouseY < box[1] + GRID_TOP || cx >= cols || cy >= pickerRows()) {
            return -1;
        }
        return (pickerScroll + cy) * cols + cx;
    }

    private int[] info(ResourceLocation texture) {
        return textureInfo.computeIfAbsent(texture, EffectLayersScreen::readInfo);
    }

    private static int[] readInfo(ResourceLocation texture) {
        var resources = Minecraft.getInstance().getResourceManager();
        try (InputStream in = resources.open(texture)) {
            byte[] data = in.readAllBytes();
            if (data.length >= 24 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G') {
                int w = (data[16] & 0xFF) << 24 | (data[17] & 0xFF) << 16 | (data[18] & 0xFF) << 8 | (data[19] & 0xFF);
                int h = (data[20] & 0xFF) << 24 | (data[21] & 0xFF) << 16 | (data[22] & 0xFF) << 8 | (data[23] & 0xFF);
                if (w > MAX_PREVIEW || h > MAX_PREVIEW) {
                    return new int[]{3, w, h};
                }
            }
            return decodeInfo(data);
        } catch (Exception e) {
            return new int[]{0, 1, 1};
        }
    }

    private static int[] decodeInfo(byte[] data) {
        try (NativeImage image = NativeImage.read(new java.io.ByteArrayInputStream(data))) {
            int corner = image.getPixelRGBA(0, 0);
            int kind = (corner >>> 24) == 0 ? 1 : (corner & 0xFFFFFF) == 0 ? 2 : 3;
            return new int[]{kind, image.getWidth(), image.getHeight()};
        } catch (Exception e) {
            return new int[]{0, 1, 1};
        }
    }

    private void thumb(GuiGraphics gfx, @Nullable ResourceLocation texture, int x, int y, int size) {
        gfx.fill(x, y, x + size, y + size, 0xFF101010);
        if (texture == null) {
            return;
        }
        int[] info = info(texture);
        if (info[0] == 0) {
            gfx.drawCenteredString(font, "?", x + size / 2, y + size / 2 - 4, 0xFF5555);
            return;
        }
        if (info[1] > MAX_PREVIEW || info[2] > MAX_PREVIEW) {
            gfx.drawCenteredString(font, "\u2026", x + size / 2, y + size / 2 - 4, 0xFFAAAAAA);
            return;
        }
        int lo = Math.min(info[1], info[2]);
        int hi = Math.max(info[1], info[2]);
        if (hi % lo == 0) {
            gfx.blit(texture, x, y, size, size, 0, 0, lo, lo, info[1], info[2]);
        } else {
            gfx.blit(texture, x, y, size, size, 0, 0, info[1], info[2], info[1], info[2]);
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);
        gfx.drawString(font, title, 10, 10, 0xFFFFFF);
        int textWidth = width - panelX - 10;
        String header = selected < 0 ? I18n.get("gui.mm.effect_studio.settings") : typeName(type(target()));
        String tipKey = selected < 0 ? "gui.mm.effect_studio.settings.tip" : "gui.mm.effect_studio.layer." + type(target()) + ".tip";
        gfx.drawString(font, header, panelX, 8, 0xFFFFFF);
        if (I18n.exists(tipKey)) {
            gfx.drawWordWrap(font, Component.translatable(tipKey), panelX, 20, textWidth, 0xA0A0A0);
        }
        if (expert) {
            renderRawLabels(gfx);
        } else {
            renderRows(gfx, mouseX, mouseY);
        }
        String warning = selected < 0 ? null : textureWarning(target());
        if (error != null) {
            gfx.drawString(font, font.plainSubstrByWidth(error, width - 250), 100, height - 20, 0xFF5555);
        } else if (warning != null) {
            gfx.drawString(font, font.plainSubstrByWidth(Component.translatable(warning).getString(), width - 250), 100, height - 20, 0xFFAA00);
        }
        if (layers().isEmpty() && selected < 0 && !expert) {
            gfx.drawWordWrap(font, Component.translatable("gui.mm.effect_studio.no_layers"), panelX, rowsTop + Math.min(rows.size(), rowsFit) * ROW + 8, textWidth, 0xFFD040);
        }
        if (pickerOpen) {
            renderPicker(gfx, mouseX, mouseY, partialTick);
        }
        if (chooserOpen) {
            renderChooser(gfx, mouseX, mouseY);
        }
    }

    private void renderRawLabels(GuiGraphics gfx) {
        String[] keys = FxSchema.rawKeys(fields());
        int ry = rowsTop;
        for (int k = scroll * 2; k < keys.length && ry < rowsTop + rowsFit * ROW; k += 2) {
            gfx.drawString(font, keys[k], panelX, ry + 5, 0xC0C0C0);
            ry += ROW;
        }
    }

    private void renderRows(GuiGraphics gfx, int mouseX, int mouseY) {
        FxSchema.Field hovered = null;
        int end = Math.min(rows.size(), scroll + rowsFit);
        for (int i = scroll; i < end; i++) {
            FxSchema.Field f = rows.get(i).field();
            int y = rowsTop + (i - scroll) * ROW;
            if (f == null) {
                continue;
            }
            if (mouseX >= panelX && mouseX < width - 8 && mouseY >= y && mouseY < y + ROW) {
                hovered = f;
            }
            gfx.drawString(font, font.plainSubstrByWidth(fieldName(f), ctrlX - panelX - 6), panelX, y + 5, f.more() ? 0xB0B0B0 : 0xE0E0E0);
            if (f.kind() == FxSchema.Kind.COLOR) {
                gfx.fill(ctrlX, y + 1, ctrlX + 16, y + 17, 0xFFFFFFFF);
                gfx.fill(ctrlX + 1, y + 2, ctrlX + 15, y + 16, 0xFF000000 | colorValue(f));
            } else if (f.kind() == FxSchema.Kind.TEXTURE) {
                String current = target().has("texture") && target().get("texture").isJsonPrimitive() ? target().get("texture").getAsString() : "";
                thumb(gfx, current.isEmpty() ? null : ResourceLocation.tryParse(current), ctrlX, y, 18);
            }
        }
        if (rows.size() > rowsFit) {
            int trackTop = rowsTop;
            int trackHeight = rowsFit * ROW;
            int barHeight = Math.max(10, trackHeight * rowsFit / rows.size());
            int barY = trackTop + (trackHeight - barHeight) * scroll / Math.max(1, rows.size() - rowsFit);
            gfx.fill(width - 6, trackTop, width - 3, trackTop + trackHeight, 0x40FFFFFF);
            gfx.fill(width - 6, barY, width - 3, barY + barHeight, 0xC0FFFFFF);
        }
        if (hovered != null) {
            gfx.drawString(font, fieldName(hovered), panelX, infoY(), 0xFFFFFF);
            String tip = fieldTip(hovered);
            if (!tip.isEmpty()) {
                gfx.drawWordWrap(font, Component.literal(tip), panelX, infoY() + 11, width - panelX - 20, 0xA0A0A0);
            }
        }
    }

    private void renderPicker(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.pose().pushPose();
        gfx.pose().translate(0, 0, 400);
        gfx.fill(0, 0, width, height, 0xA0000000);
        int[] box = pickerBox();
        gfx.fill(box[0] - 1, box[1] - 1, box[0] + box[2] + 1, box[1] + box[3] + 1, 0xFF8080A0);
        gfx.fill(box[0], box[1], box[0] + box[2], box[1] + box[3], 0xFF1E1E28);
        gfx.drawString(font, Component.translatable("gui.mm.effect_studio.texture_pick"), box[0] + 8, box[1] + 8, 0xFFFFFF);
        int[][] rects = pickerButtonRects(box);
        String tip = null;
        for (int k = 0; k < rects.length; k++) {
            int[] r = rects[k];
            boolean over = mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3];
            boolean on = k < 3 && k == pickerMode || k == 4 && reloadingTextures;
            gfx.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], on ? 0xFF3A6A4A : over ? 0xFF4A4A60 : 0xFF2E2E40);
            gfx.renderOutline(r[0], r[1], r[2], r[3], over ? 0xFFFFFFFF : 0xFF606078);
            gfx.drawCenteredString(font, Component.translatable("gui.mm.effect_studio.texture_btn." + PICKER_BUTTONS[k]), r[0] + r[2] / 2, r[1] + 3, 0xFFFFFF);
            if (over) {
                tip = PICKER_BUTTONS[k];
            }
        }
        pickerSearch.setX(box[0] + 8);
        pickerSearch.setY(box[1] + 24);
        pickerSearch.setWidth(box[2] - 16);
        pickerSearch.render(gfx, mouseX, mouseY, partialTick);
        if (pickerSearch.getValue().isEmpty()) {
            gfx.drawString(font, Component.translatable("gui.mm.effect_studio.texture_search"), box[0] + 12, box[1] + 27, 0xFF707080);
        }
        List<ResourceLocation> list = pickerList();
        int cols = pickerCols();
        int visible = pickerRows();
        int total = (list.size() + cols - 1) / cols;
        pickerScroll = Math.max(0, Math.min(pickerScroll, Math.max(0, total - visible)));
        String current = target().has("texture") && target().get("texture").isJsonPrimitive() ? target().get("texture").getAsString() : "";
        int hover = pickerIndexAt(mouseX, mouseY);
        for (int r = 0; r < visible; r++) {
            for (int c = 0; c < cols; c++) {
                int index = (pickerScroll + r) * cols + c;
                if (index >= list.size()) {
                    break;
                }
                int x = box[0] + 8 + c * CELL;
                int y = box[1] + GRID_TOP + r * CELL;
                ResourceLocation texture = list.get(index);
                boolean chosen = texture.toString().equals(current);
                gfx.fill(x, y, x + CELL - 4, y + CELL - 4, chosen ? 0xFF50C878 : index == hover ? 0xFFFFFFFF : 0xFF404050);
                thumb(gfx, texture, x + 2, y + 2, CELL - 8);
            }
        }
        if (total > visible) {
            int trackH = visible * CELL - 4;
            int barH = Math.max(10, trackH * visible / total);
            int barY = box[1] + GRID_TOP + (trackH - barH) * pickerScroll / Math.max(1, total - visible);
            gfx.fill(box[0] + box[2] - 6, box[1] + GRID_TOP, box[0] + box[2] - 3, box[1] + GRID_TOP + trackH, 0x40FFFFFF);
            gfx.fill(box[0] + box[2] - 6, barY, box[0] + box[2] - 3, barY + barH, 0xC0FFFFFF);
        }
        int footY = box[1] + box[3] - GRID_FOOT + 6;
        if (hover >= 0 && hover < list.size()) {
            gfx.drawString(font, font.plainSubstrByWidth(list.get(hover).toString(), box[2] - 16), box[0] + 8, footY, 0xE0E0E0);
        } else if (folderError != null) {
            gfx.drawString(font, font.plainSubstrByWidth(folderError, box[2] - 16), box[0] + 8, footY, 0xFF5555);
        } else if (list.isEmpty()) {
            gfx.drawString(font, Component.translatable("gui.mm.effect_studio.texture_empty"), box[0] + 8, footY, 0xFFAA00);
        } else {
            var hintLines = font.split(Component.translatable("gui.mm.effect_studio.texture_hint", session.structure.getNamespace()), box[2] - 16);
            for (int line = 0; line < Math.min(2, hintLines.size()); line++) {
                gfx.drawString(font, hintLines.get(line), box[0] + 8, footY + line * 10, 0x909090);
            }
        }
        if (tip != null) {
            gfx.renderTooltip(font, font.split(Component.translatable("gui.mm.effect_studio.texture_btn." + tip + ".tip"), 200), mouseX, mouseY);
        }
        gfx.pose().popPose();
    }

    @Nullable
    private String textureWarning(JsonObject layer) {
        String type = type(layer);
        if (!layer.has("texture")) {
            return null;
        }
        ResourceLocation texture = ResourceLocation.tryParse(layer.get("texture").getAsString());
        int kind = texture == null ? 0 : info(texture)[0];
        if (kind == 0) {
            return "gui.mm.effect_studio.warn.missing";
        }
        String fallback = type.equals("emitter") ? "translucent" : (type.equals("beam") || type.equals("glow") ? "additive" : null);
        if (fallback == null) {
            return null;
        }
        String blend = layer.has("blend") ? layer.get("blend").getAsString() : fallback;
        if (blend.equals("additive") && kind == 1) {
            return "gui.mm.effect_studio.warn.additive";
        }
        if (blend.equals("translucent") && kind == 2) {
            return "gui.mm.effect_studio.warn.translucent";
        }
        return null;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class NumberSlider extends AbstractSliderButton {
        private final String prefix;
        private final double min;
        private final double max;
        private final double step;
        private final Consumer<Double> onChange;
        private double last = Double.NaN;

        NumberSlider(int x, int y, int width, int height, String prefix, FxSchema.Field f, double current, Consumer<Double> onChange) {
            super(x, y, width, height, Component.empty(), 0);
            this.prefix = prefix;
            this.min = Math.min(f.min(), current);
            this.max = Math.max(f.max(), current);
            this.step = f.step();
            this.onChange = onChange;
            this.value = max > min ? (current - min) / (max - min) : 0;
            setMessage(Component.literal(prefix + format(current)));
        }

        private double real() {
            double raw = min + value * (max - min);
            double snapped = Math.round(raw / step) * step;
            return Math.max(min, Math.min(max, Math.round(snapped * 1_000_000) / 1_000_000.0));
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(prefix + format(real())));
        }

        @Override
        protected void applyValue() {
            double now = real();
            if (now != last) {
                last = now;
                onChange.accept(now);
            }
        }
    }

    private static final class Swatch extends AbstractWidget {
        private final int rgb;
        private final Runnable onPick;

        Swatch(int x, int y, int rgb, Runnable onPick) {
            super(x, y, 11, 11, Component.empty());
            this.rgb = rgb;
            this.onPick = onPick;
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            onPick.run();
        }

        @Override
        protected void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
            gfx.fill(getX() - 1, getY() - 1, getX() + width + 1, getY() + height + 1, isHovered() ? 0xFFFFFFFF : 0xFF000000);
            gfx.fill(getX(), getY(), getX() + width, getY() + height, 0xFF000000 | rgb);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }
    }
}

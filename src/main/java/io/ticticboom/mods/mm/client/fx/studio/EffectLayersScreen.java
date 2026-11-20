package io.ticticboom.mods.mm.client.fx.studio;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.NativeImage;
import io.ticticboom.mods.mm.client.fx.FxManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EffectLayersScreen extends Screen {
    private static final String[] TYPES = FxSchema.LAYERS.keySet().toArray(new String[0]);

    private final EffectStudio.Session session;
    private final Screen parent;
    private final ResourceLocation id;
    private final JsonObject json;
    private final List<ResourceLocation> textures;
    private final Map<String, JsonObject> presets = EffectStudio.presets();
    private final List<String> presetNames = new ArrayList<>(presets.keySet());
    private final Map<ResourceLocation, Integer> textureKinds = new HashMap<>();
    private int selected = -1;
    private int type;
    private int preset;
    private int scroll;
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
        var resources = Minecraft.getInstance().getResourceManager();
        this.textures = resources.listResources("textures/fx", path -> path.getPath().endsWith(".png")).keySet()
                .stream().sorted().toList();
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

    @Override
    protected void init() {
        int y = 28;
        addRenderableWidget(Button.builder(Component.translatable("gui.mm.effect_studio.settings"), b -> select(-1))
                .bounds(10, y, 140, 16).build());
        y += 18;
        for (int i = 0; i < layers().size(); i++) {
            int index = i;
            String name = layers().get(i).isJsonObject() ? type(layers().get(i).getAsJsonObject()) : "?";
            addRenderableWidget(Button.builder(Component.literal((i == selected ? "> " : "") + (i + 1) + ". " + name), b -> select(index))
                    .bounds(10, y, 140, 16).build());
            y += 18;
        }
        y += 4;
        addRenderableWidget(Button.builder(Component.literal(TYPES[type]), b -> {
            type = (type + 1) % TYPES.length;
            b.setMessage(Component.literal(TYPES[type]));
        }).bounds(10, y, 80, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.mm.effect_studio.add"), b -> {
            var layer = new JsonObject();
            layer.addProperty("type", TYPES[type]);
            if (!textures.isEmpty()) {
                layer.addProperty("texture", textures.get(0).toString());
            }
            layers().add(layer);
            select(layers().size() - 1);
            apply();
        }).bounds(92, y, 58, 16).build());
        if (!presetNames.isEmpty()) {
            y += 18;
            addRenderableWidget(Button.builder(Component.literal(presetNames.get(preset)), b -> {
                preset = (preset + 1) % presetNames.size();
                b.setMessage(Component.literal(presetNames.get(preset)));
            }).bounds(10, y, 80, 16).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.mm.effect_studio.add"), b -> {
                for (JsonElement layer : presets.get(presetNames.get(preset)).getAsJsonArray("layers")) {
                    layers().add(layer.deepCopy());
                }
                select(layers().size() - 1);
                apply();
            }).bounds(92, y, 58, 16).build());
        }
        if (selected >= 0) {
            y += 22;
            addRenderableWidget(Button.builder(Component.literal("^"), b -> move(-1)).bounds(10, y, 20, 16).build());
            addRenderableWidget(Button.builder(Component.literal("v"), b -> move(1)).bounds(32, y, 20, 16).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.mm.effect_studio.copy"), b -> {
                layers().add(target().deepCopy());
                select(layers().size() - 1);
                apply();
            }).bounds(54, y, 44, 16).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.mm.effect_studio.remove"), b -> {
                layers().remove(selected);
                select(-1);
                apply();
            }).bounds(100, y, 50, 16).build());
        }

        String[] keys = selected < 0 ? FxSchema.EFFECT
                : FxSchema.LAYERS.getOrDefault(type(target()), new String[0]);
        int rows = Math.max(1, (height - 70) / 20);
        scroll = Math.max(0, Math.min(scroll, keys.length / 2 - rows));
        int ry = 28;
        for (int k = scroll * 2; k < keys.length && ry < 28 + rows * 20; k += 2) {
            String key = keys[k];
            var box = new EditBox(font, 280, ry, 150, 16, Component.literal(key));
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
                }).bounds(432, ry, 16, 16).build());
            }
            ry += 20;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.mm.effect_studio.back"), b ->
                minecraft.setScreen(parent)).bounds(10, height - 26, 80, 20).build());
    }

    private void select(int index) {
        selected = index;
        scroll = 0;
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= 160) {
            scroll -= (int) Math.signum(scrollY);
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);
        gfx.drawString(font, title, 10, 10, 0xFFFFFF);
        String[] keys = selected < 0 ? FxSchema.EFFECT
                : FxSchema.LAYERS.getOrDefault(type(target()), new String[0]);
        int rows = Math.max(1, (height - 70) / 20);
        int ry = 32;
        for (int k = scroll * 2; k < keys.length && ry < 32 + rows * 20; k += 2) {
            gfx.drawString(font, keys[k], 170, ry, 0xC0C0C0);
            ry += 20;
        }
        String warning = selected < 0 ? null : textureWarning(target());
        if (error != null) {
            gfx.drawString(font, error, 100, height - 20, 0xFF5555);
        } else if (warning != null) {
            gfx.drawString(font, Component.translatable(warning), 100, height - 20, 0xFFAA00);
        }
    }

    @Nullable
    private String textureWarning(JsonObject layer) {
        String type = type(layer);
        if (!layer.has("texture")) {
            return null;
        }
        ResourceLocation texture = ResourceLocation.tryParse(layer.get("texture").getAsString());
        int kind = texture == null ? 0 : textureKinds.computeIfAbsent(texture, EffectLayersScreen::textureKind);
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

    private static int textureKind(ResourceLocation texture) {
        var resources = Minecraft.getInstance().getResourceManager();
        try (InputStream in = resources.open(texture); NativeImage image = NativeImage.read(in)) {
            int corner = image.getPixelRGBA(0, 0);
            if ((corner >>> 24) == 0) {
                return 1;
            }
            return (corner & 0xFFFFFF) == 0 ? 2 : 3;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

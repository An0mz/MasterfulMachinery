package io.ticticboom.mods.mm.client.fx.studio;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ticticboom.mods.mm.client.fx.FxDefinition;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.structure.attachment.visuals.EffectVisual;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@EventBusSubscriber(modid = Ref.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class EffectStudio {
    @Nullable
    static Session session;

    private EffectStudio() {
    }

    static final class Session {
        final BlockPos controller;
        final ResourceLocation structure;
        final List<JsonObject> effects;
        int selected = -1;
        boolean idle;
        boolean open;
        boolean dirty = true;
        @Nullable
        List<EffectVisual> base;
        @Nullable
        List<EffectVisual> combined;
        final Map<ResourceLocation, JsonObject> edited = new LinkedHashMap<>();
        final Map<ResourceLocation, JsonObject> originals = new HashMap<>();
        final Map<ResourceLocation, double[]> sliders = new HashMap<>();
        final Map<ResourceLocation, String> colors = new HashMap<>();

        Session(BlockPos controller, ResourceLocation structure, List<JsonObject> effects) {
            this.controller = controller;
            this.structure = structure;
            this.effects = effects;
        }
    }

    @Nullable
    public static List<EffectVisual> effects(MachineControllerBlockEntity be) {
        List<EffectVisual> base = be.effects();
        Session s = session;
        if (!active(be)) {
            return base;
        }
        if (s.dirty || s.base != base) {
            var list = new ArrayList<EffectVisual>();
            for (JsonObject json : s.effects) {
                try {
                    list.add(EffectVisual.parse(json));
                } catch (Exception ignored) {
                }
            }
            s.base = base;
            s.dirty = false;
            s.combined = list.isEmpty() ? null : List.copyOf(list);
        }
        return s.combined;
    }

    public static boolean active(MachineControllerBlockEntity be) {
        Session s = session;
        return s != null && s.controller.equals(be.getBlockPos()) && be.isFormed() && be.getStructure() != null
                && be.getFormedRotation() != null && s.structure.equals(be.getStructure().id());
    }

    public static boolean working(MachineControllerBlockEntity be, boolean actual) {
        return active(be) && session.open ? !session.idle : actual;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        session = null;
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide() || !event.getItemStack().is(MMRegisters.EFFECT_WAND.get())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() == InteractionHand.MAIN_HAND && event.getLevel().getBlockEntity(event.getPos()) instanceof MachineControllerBlockEntity be) {
            open(be);
        }
    }

    private static void open(MachineControllerBlockEntity be) {
        var mc = Minecraft.getInstance();
        if (!mc.player.hasPermissions(2)) {
            mc.player.displayClientMessage(Component.translatable("gui.mm.effect_studio.no_permission"), true);
            return;
        }
        if (be.getStructure() == null || be.getFormedRotation() == null) {
            mc.player.displayClientMessage(Component.translatable("gui.mm.effect_studio.not_formed"), true);
            return;
        }
        ResourceLocation id = be.getStructure().id();
        if (session == null || !session.controller.equals(be.getBlockPos()) || !session.structure.equals(id)) {
            session = new Session(be.getBlockPos(), id, bound(be));
        }
        mc.setScreen(new EffectStudioScreen(session));
    }

    @Nullable
    static MachineControllerBlockEntity machine(Session s) {
        var level = Minecraft.getInstance().level;
        return level != null && level.getBlockEntity(s.controller) instanceof MachineControllerBlockEntity be && active(be) ? be : null;
    }

    static Map<String, JsonObject> presets() {
        var result = new TreeMap<String, JsonObject>();
        var resources = Minecraft.getInstance().getResourceManager();
        for (var entry : resources.listResources("mm_fx_presets", path -> path.getPath().endsWith(".json")).entrySet()) {
            String path = entry.getKey().getPath();
            String name = path.substring("mm_fx_presets/".length(), path.length() - 5);
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                FxDefinition.parse(ResourceLocation.fromNamespaceAndPath(entry.getKey().getNamespace(), name), json);
                json.getAsJsonArray("layers").forEach(JsonElement::getAsJsonObject);
                result.put(name, json);
            } catch (Exception e) {
                Ref.LOG.warn("Ignoring effect preset {}: {}", entry.getKey(), e.toString());
            }
        }
        return result;
    }

    private static List<JsonObject> bound(MachineControllerBlockEntity be) {
        var result = new ArrayList<JsonObject>();
        if (be.effects() != null) {
            for (EffectVisual visual : be.effects()) {
                JsonObject json = visual.toJson();
                json.remove("type");
                result.add(json);
            }
        }
        return result;
    }

    static boolean save(Session s) {
        var json = new JsonObject();
        json.addProperty("structure", s.structure.toString());
        json.addProperty("replace", true);
        var list = new JsonArray();
        s.effects.forEach(list::add);
        json.add("effects", list);
        var gson = new GsonBuilder().setPrettyPrinting().create();
        Path path = FMLPaths.GAMEDIR.get().resolve("kubejs").resolve("data").resolve(s.structure.getNamespace())
                .resolve("mm").resolve("structure_effects").resolve(s.structure.getPath() + ".json");
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, gson.toJson(json), StandardCharsets.UTF_8);
            for (var entry : s.edited.entrySet()) {
                Path fx = FMLPaths.GAMEDIR.get().resolve("kubejs").resolve("assets").resolve(entry.getKey().getNamespace())
                        .resolve("mm_fx").resolve(entry.getKey().getPath() + ".json");
                Files.createDirectories(fx.getParent());
                Files.writeString(fx, gson.toJson(entry.getValue()), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            Ref.LOG.error("Effect Studio: could not write {}", path, e);
            return false;
        }
        Minecraft.getInstance().player.connection.sendCommand("reload");
        return true;
    }
}

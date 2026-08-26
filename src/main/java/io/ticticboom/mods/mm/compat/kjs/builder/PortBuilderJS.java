package io.ticticboom.mods.mm.compat.kjs.builder;

import dev.latvian.mods.rhino.util.HideFromJS;
import io.ticticboom.mods.mm.model.IdList;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.MMPortRegistry;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Getter
public class PortBuilderJS {

    private final String id;
    private ResourceLocation type;
    private String name;
    private final List<ResourceLocation> controllers = new ArrayList<>();
    private Consumer<PortConfigBuilderJS> builder;
    private final Map<String, String> textures = new LinkedHashMap<>();

    @HideFromJS
    public PortBuilderJS(String id) {
        this.id = id;
    }

    public PortBuilderJS overlay(String texture) {
        return putTexture("overlay", texture);
    }

    public PortBuilderJS inputOverlay(String texture) {
        return putTexture("inputOverlay", texture);
    }

    public PortBuilderJS outputOverlay(String texture) {
        return putTexture("outputOverlay", texture);
    }

    public PortBuilderJS texture(String texture) {
        return putTexture("texture", texture);
    }

    public PortBuilderJS inputTexture(String texture) {
        return putTexture("inputTexture", texture);
    }

    public PortBuilderJS outputTexture(String texture) {
        return putTexture("outputTexture", texture);
    }

    @HideFromJS
    private PortBuilderJS putTexture(String key, String texture) {
        var rl = ResourceLocation.tryParse(texture);
        if (rl == null) throw new IllegalArgumentException("Invalid resource location: " + texture);
        textures.put(key, rl.toString());
        return this;
    }

    public PortBuilderJS name(String name) {
        this.name = name;
        return this;
    }

    @SuppressWarnings("unused")
    public PortBuilderJS controllerId(String controllerId) {
        var rl = ResourceLocation.tryParse(controllerId);
        if (rl == null) throw new IllegalArgumentException("Invalid resource location: " + controllerId);
        controllers.add(rl);
        return this;
    }

    public PortBuilderJS config(String type, Consumer<PortConfigBuilderJS> builder) {
        var rl = ResourceLocation.tryParse(type);
        if (rl == null) throw new IllegalArgumentException("Invalid resource location: " + type);
        this.type = rl;
        this.builder = builder;
        return this;
    }

    @HideFromJS
    public List<PortModel> build() {
        var portType = MMPortRegistry.requirePortType(type);
        var storageFactory = portType.createStorageFactory(builder);
        IdList controllerIds = new IdList(controllers);
        var inputPort = PortModel.create(id, name, controllerIds, type, storageFactory, true);
        var outputPort = PortModel.create(id, name, controllerIds, type, storageFactory, false);
        textures.forEach((key, value) -> {
            inputPort.jsonConfig().addProperty(key, value);
            outputPort.jsonConfig().addProperty(key, value);
        });
        return List.of(inputPort, outputPort);
    }
}

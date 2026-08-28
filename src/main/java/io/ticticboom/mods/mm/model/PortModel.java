package io.ticticboom.mods.mm.model;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.port.MMPortRegistry;
import io.ticticboom.mods.mm.util.ParserUtils;
import io.ticticboom.mods.mm.util.PortUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public record PortModel(
        String id,
        String name,
        Component displayName,
        IdList controllerIds,
        ResourceLocation type,
        IPortStorageFactory config,
        JsonObject jsonConfig,
        boolean input) {

    public ResourceLocation overlayTexture() {
        var specific = ParserUtils.parseOptionalId(jsonConfig, input ? "inputOverlay" : "outputOverlay");
        return specific != null ? specific : ParserUtils.parseOptionalId(jsonConfig, "overlay");
    }

    public ResourceLocation baseTexture() {
        var specific = ParserUtils.parseOptionalId(jsonConfig, input ? "inputTexture" : "outputTexture");
        return specific != null ? specific : ParserUtils.parseOptionalId(jsonConfig, "texture");
    }

    public ResourceLocation customModel() {
        var specific = ParserUtils.parseOptionalId(jsonConfig, input ? "inputModel" : "outputModel");
        return specific != null ? specific : ParserUtils.parseOptionalId(jsonConfig, "model");
    }

    public static PortModel parse(JsonObject json, boolean input) {
        var id = PortUtils.id(json.get("id").getAsString(), input);
        // "name" accepts a plain string or a { "translation": "key" } object.
        var name = PortUtils.name(ParserUtils.parseComponentKey(json.get("name")), input);
        var displayName = PortUtils.name(ParserUtils.parseComponent(json.get("name")), input);
        var controllerIds = IdList.parse(json.get("controllerIds"));
        var type = ParserUtils.parseId(json, "type");
        var portType = MMPortRegistry.requirePortType(type);
        var storageFactory = portType.getParser().parseStorage(json.get("config").getAsJsonObject());
        return new PortModel(id, name, displayName, controllerIds, type, storageFactory, json, input);
    }

    public static PortModel create(String id, String name, IdList controllerIds, ResourceLocation type, IPortStorageFactory config, boolean input) {
        var fid = PortUtils.id(id, input);
        var fname = PortUtils.name(name, input);
        var json = paramsToJson(fid, fname, controllerIds, type, config, input);
        return new PortModel(fid, fname, Component.literal(fname), controllerIds, type, config, json, input);
    }

    public static JsonObject paramsToJson(String id, String name, IdList controllerIds, ResourceLocation type, IPortStorageFactory config, boolean input) {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("name", name);
        json.add("controllerIds", controllerIds.serialize());
        json.addProperty("type", type.toString());
        try {
            json.add("config", config.serialize());
        } catch (Exception e) {
            json.addProperty("config", config.toString());
        }
        return json;
    }

}

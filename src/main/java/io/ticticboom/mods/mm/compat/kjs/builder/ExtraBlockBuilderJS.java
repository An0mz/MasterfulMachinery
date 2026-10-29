package io.ticticboom.mods.mm.compat.kjs.builder;

import com.google.gson.JsonElement;
import io.ticticboom.mods.mm.compat.kjs.KubeJsValues;
import io.ticticboom.mods.mm.extra.ExtraBlockModel;
import io.ticticboom.mods.mm.util.ParserUtils;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;

@Getter
public class ExtraBlockBuilderJS {
    private final String id;
    private ResourceLocation type;
    private String name;
    private JsonElement nameSpec;
    private String connectGroup;

    public ExtraBlockBuilderJS(String id) {
        this.id = id;
    }

    public ExtraBlockBuilderJS name(Object name) {
        this.nameSpec = KubeJsValues.toJson(name);
        this.name = ParserUtils.parseComponentKey(nameSpec);
        return this;
    }

    public ExtraBlockBuilderJS type(ResourceLocation type) {
        this.type = type;
        return this;
    }

    public ExtraBlockBuilderJS connectGroup(String connectGroup) {
        this.connectGroup = connectGroup;
        return this;
    }

    public ExtraBlockModel build() {
        return new ExtraBlockModel(id, name, ParserUtils.parseNameSupplier(nameSpec), type, connectGroup);
    }
}

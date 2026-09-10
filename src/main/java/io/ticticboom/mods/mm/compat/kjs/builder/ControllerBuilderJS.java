package io.ticticboom.mods.mm.compat.kjs.builder;

import com.google.gson.JsonElement;
import dev.latvian.mods.rhino.util.HideFromJS;
import io.ticticboom.mods.mm.compat.kjs.KubeJsValues;
import io.ticticboom.mods.mm.model.ControllerModel;
import io.ticticboom.mods.mm.model.RecipeSelectionMode;
import io.ticticboom.mods.mm.util.ParserUtils;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class ControllerBuilderJS {
    private final String id;
    private String name;
    private JsonElement nameSpec;
    private ResourceLocation type;
    private boolean parallelProcessingDefault = false;
    private int maxParallelRecipes = -1;
    private RecipeSelectionMode recipeSelectionMode = RecipeSelectionMode.DEFAULT;
    private final Map<String, String> textures = new LinkedHashMap<>();

    @HideFromJS
    public ControllerBuilderJS(String id) {
        this.id = id;
    }

    public ControllerBuilderJS type(String id) {
        var rl = ResourceLocation.tryParse(id);
        if (rl == null) throw new IllegalArgumentException("Invalid resource location: " + id);
        this.type = rl;
        return this;
    }

    public ControllerBuilderJS overlay(String texture) {
        return putTexture("overlay", texture);
    }

    public ControllerBuilderJS texture(String texture) {
        return putTexture("texture", texture);
    }

    public ControllerBuilderJS model(String model) {
        return putTexture("model", model);
    }

    @HideFromJS
    private ControllerBuilderJS putTexture(String key, String texture) {
        var rl = ResourceLocation.tryParse(texture);
        if (rl == null) throw new IllegalArgumentException("Invalid resource location: " + texture);
        textures.put(key, rl.toString());
        return this;
    }

    public ControllerBuilderJS name(Object name) {
        this.nameSpec = KubeJsValues.toJson(name);
        this.name = ParserUtils.parseComponentKey(nameSpec);
        return this;
    }

    @SuppressWarnings("unused")
    public ControllerBuilderJS parallelProcessingDefault(boolean parallelProcessingDefault) {
        this.parallelProcessingDefault = parallelProcessingDefault;
        return this;
    }

    @SuppressWarnings("unused")
    public ControllerBuilderJS maxParallelRecipes(int maxParallelRecipes) {
        // allow negative to mean unspecified (-1); clamp to [0,100] otherwise
        if (maxParallelRecipes < 0) this.maxParallelRecipes = -1;
        else this.maxParallelRecipes = Math.min(maxParallelRecipes, 100);
        return this;
    }

    @SuppressWarnings("unused")
    public ControllerBuilderJS recipeSelectionMode(String recipeSelectionMode) {
        this.recipeSelectionMode = RecipeSelectionMode.parse(recipeSelectionMode);
        return this;
    }

    @HideFromJS
    public ControllerModel build() {
        var model = ControllerModel.createStyled(id, type, nameSpec, parallelProcessingDefault, maxParallelRecipes, recipeSelectionMode);
        textures.forEach(model.config()::addProperty);
        return model;
    }
}

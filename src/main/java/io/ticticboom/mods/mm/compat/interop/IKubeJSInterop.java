package io.ticticboom.mods.mm.compat.interop;

import java.util.function.Function;
import net.minecraft.nbt.CompoundTag;
import io.ticticboom.mods.mm.builder.structure.BuildableStructure;
import net.minecraft.resources.ResourceLocation;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.extra.ExtraBlockModel;
import io.ticticboom.mods.mm.model.ControllerModel;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.structure.StructureModel;

import java.util.List;

public interface IKubeJSInterop {
    List<StructureModel> postCreateStructures();
    List<RecipeModel> postCreateRecipes();
    List<ControllerModel> postRegisterControllers();
    List<PortModel> postRegisterPorts();
    List<ExtraBlockModel> postRegisterExtraBlocks();

    List<BuildableStructure> postBuilderStructures(List<BuildableStructure> loaded, Function<ResourceLocation, CompoundTag> reader);

    boolean postRecipeStarted(MachineControllerBlockEntity controller, ResourceLocation recipeId);

    void postRecipeFinished(MachineControllerBlockEntity controller, ResourceLocation recipeId);
}

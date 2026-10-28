package io.ticticboom.mods.mm.compat.jei;

import com.google.common.collect.ImmutableList;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.tool.MultiblockToolScreen;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerScreen;
import io.ticticboom.mods.mm.controller.single.register.SingleMachineLayout;
import io.ticticboom.mods.mm.controller.single.register.SingleMachineScreen;
import io.ticticboom.mods.mm.compat.jei.category.MMRecipeCategory;
import io.ticticboom.mods.mm.compat.jei.category.MMStructureCategory;
import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import io.ticticboom.mods.mm.compat.jei.ingredient.create.CreateRotationIngredientHelper;
import io.ticticboom.mods.mm.compat.jei.ingredient.create.CreateRotationIngredientRenderer;
import io.ticticboom.mods.mm.compat.jei.ingredient.energy.EnergyIngredientHelper;
import io.ticticboom.mods.mm.compat.jei.ingredient.entity.EntityIngredientHelper;
import io.ticticboom.mods.mm.compat.jei.ingredient.entity.EntityIngredientRenderer;
import io.ticticboom.mods.mm.compat.jei.ingredient.energy.EnergyIngredientRenderer;
import io.ticticboom.mods.mm.compat.jei.ingredient.heat.HeatIngredientHelper;
import io.ticticboom.mods.mm.compat.jei.ingredient.heat.HeatIngredientRenderer;
import io.ticticboom.mods.mm.compat.jei.ingredient.mana.BotaniaManaIngredientHelper;
import io.ticticboom.mods.mm.compat.jei.ingredient.mana.BotaniaManaIngredientRenderer;
import io.ticticboom.mods.mm.compat.jei.ingredient.matter.MatterIngredientHelper;
import io.ticticboom.mods.mm.compat.jei.ingredient.matter.MatterIngredientRenderer;
import io.ticticboom.mods.mm.compat.jei.ingredient.pncr.PneumaticAirIngredientHelper;
import io.ticticboom.mods.mm.compat.jei.ingredient.pncr.PneumaticAirIngredientRender;
import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.recipe.MachineRecipeManager;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.structure.StructureModel;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.runtime.IClickableIngredient;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import io.ticticboom.mods.mm.compat.jei.ingredient.radiation.RadiationIngredientHelper;
import io.ticticboom.mods.mm.compat.jei.ingredient.radiation.RadiationIngredientRenderer;
import io.ticticboom.mods.mm.compat.jei.ingredient.source.ArsSourceIngredientHelper;
import io.ticticboom.mods.mm.compat.jei.ingredient.source.ArsSourceIngredientRenderer;
import io.ticticboom.mods.mm.compat.jei.ingredient.starlight.StarlightIngredientHelper;
import io.ticticboom.mods.mm.compat.jei.ingredient.starlight.StarlightIngredientRenderer;
import io.ticticboom.mods.mm.compat.jei.ingredient.emc.EmcIngredientHelper;
import io.ticticboom.mods.mm.compat.jei.ingredient.emc.EmcIngredientRenderer;
import io.ticticboom.mods.mm.compat.jei.ingredient.ether.EtherIngredientHelper;
import io.ticticboom.mods.mm.compat.jei.ingredient.ether.EtherIngredientRenderer;
import io.ticticboom.mods.mm.util.ItemNbtUtil;

@SuppressWarnings("unused")
@JeiPlugin
public class MMJeiPlugin implements IModPlugin {

    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime jeiRuntime) {
        JeiRecipeLookup.setRuntime(jeiRuntime);
    }

    @Override
    public void onRuntimeUnavailable() {
        JeiRecipeLookup.setRuntime(null);
    }

    public static final ResourceLocation UID = Ref.id("jei_plugin");

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return UID;
    }

    public static final List<MMRecipeCategory> recipeCategories = new ArrayList<>();

    @Override
    public void registerCategories(@NotNull IRecipeCategoryRegistration registration) {
        recipeCategories.clear();
        if (MMConfig.JEI_RECIPE_SPLIT) {
            for (StructureModel parentStructure : StructureManager.STRUCTURES.values()) {
                registerProcessRecipe(registration, parentStructure);
            }
        } else {
            registerProcessRecipe(registration, null);
        }
        registration.addRecipeCategories(new MMStructureCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    private void registerProcessRecipe(IRecipeCategoryRegistration registration, StructureModel parent) {
        List<RecipeModel> recipes;
        if (parent != null) {
            recipes = MachineRecipeManager.RECIPES.values().stream().filter(x -> x.runsIn(parent.id())).collect(Collectors.toList());
        } else {
            recipes = new ArrayList<>(MachineRecipeManager.RECIPES.values());
        }
        int maxInputRows = recipes.stream().mapToInt(r -> MMRecipeCategory.rowsFor(r.inputs().inputs().size())).max().orElse(1);
        int maxOutputRows = recipes.stream().mapToInt(r -> MMRecipeCategory.rowsFor(
                r.outputs().outputs().stream().mapToInt(o -> o.displayedOutputs().size()).sum())).max().orElse(1);
        int maxRows = Math.max(maxInputRows, maxOutputRows);
        int height = Math.max(40, Math.min(maxRows, MMRecipeCategory.VISIBLE_ROWS) * 20);
        MMRecipeCategory category = new MMRecipeCategory(registration.getJeiHelpers(), parent, height);
        registration.addRecipeCategories(category);
        recipeCategories.add(category);
    }

    // Java
    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        if (MMConfig.JEI_RECIPE_SPLIT) {
            for (var entry : recipeCategories) {
                var recipes = MachineRecipeManager.RECIPES.values().stream()
                        .filter(x -> x.runsIn(entry.getStructureModel().id()))
                        .sorted(java.util.Comparator.comparing(r -> r.id().toString()))
                        .toList();
                registration.addRecipes(entry.getRecipeType(), recipes);
            }
        } else {
            var sorted = MachineRecipeManager.RECIPES.values().stream()
                    .sorted(java.util.Comparator.comparing(r -> r.id().toString()))
                    .collect(Collectors.toList());
            registration.addRecipes(MMRecipeCategory.RECIPE_TYPE, sorted);
        }

        var sortedStructures = StructureManager.buildableStructures().stream()
                .sorted(java.util.Comparator.comparing(s -> s.id().toString()))
                .collect(Collectors.toList());
        registration.addRecipes(MMStructureCategory.RECIPE_TYPE, sortedStructures);
    }


    @Override
    public void registerIngredients(IModIngredientRegistration registration) {
        registration.register(MMJeiIngredients.ENERGY, ImmutableList.of(), new EnergyIngredientHelper(), new EnergyIngredientRenderer());
        registration.register(MMJeiIngredients.PNEUMATIC_AIR, ImmutableList.of(), new PneumaticAirIngredientHelper(), new PneumaticAirIngredientRender());
        registration.register(MMJeiIngredients.BOTANIA_MANA, ImmutableList.of(), new BotaniaManaIngredientHelper(), new BotaniaManaIngredientRenderer());
        registration.register(MMJeiIngredients.CREATE_ROTATION, ImmutableList.of(), new CreateRotationIngredientHelper(), new CreateRotationIngredientRenderer());
        registration.register(MMJeiIngredients.MEKANISM_HEAT, ImmutableList.of(), new HeatIngredientHelper(), new HeatIngredientRenderer());
        registration.register(MMJeiIngredients.REPLICATION_MATTER, ImmutableList.of(), new MatterIngredientHelper(), new MatterIngredientRenderer());
        registration.register(MMJeiIngredients.NUCLEAR_RADIATION, ImmutableList.of(), new RadiationIngredientHelper(), new RadiationIngredientRenderer());
        registration.register(MMJeiIngredients.ENTITY, ImmutableList.of(), new EntityIngredientHelper(), new EntityIngredientRenderer());
        registration.register(MMJeiIngredients.ARS_SOURCE, ImmutableList.of(), new ArsSourceIngredientHelper(), new ArsSourceIngredientRenderer());
        registration.register(MMJeiIngredients.PROJECTE_EMC, ImmutableList.of(), new EmcIngredientHelper(), new EmcIngredientRenderer());
        registration.register(MMJeiIngredients.ASTRAL_STARLIGHT, ImmutableList.of(), new StarlightIngredientHelper(), new StarlightIngredientRenderer());
        registration.register(MMJeiIngredients.ETHERIA_ETHER, ImmutableList.of(), new EtherIngredientHelper(), new EtherIngredientRenderer());
    }

    @Override
    public void registerRecipeCatalysts(@NotNull IRecipeCatalystRegistration registration) {
        for (var entry : recipeCategories) {
            StructureModel structure = entry.getStructureModel();
            if (structure == null) {
                continue;
            }
            List<ResourceLocation> controllerIds = structure.controllerIds().getIds();
            if (controllerIds.isEmpty()) {
                Ref.LOG.error("Skipping JEI catalyst for structure {}: it lists no controllerIds.", structure.id());
                continue;
            }
            ResourceLocation location = controllerIds.get(0);
            ItemStack stack = BuiltInRegistries.ITEM.get(location).getDefaultInstance();
            if (stack.isEmpty()) {
                Ref.LOG.error("Skipping JEI catalyst for structure {}: no controller block '{}' exists. "
                        + "Either it is misspelled, or the controller it names was not loaded.", structure.id(), location);
                continue;
            }
            registration.addRecipeCatalyst(stack, entry.getRecipeType());
        }
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(MMRegisters.BLUEPRINT.get(),
                (stack, ctx) -> String.valueOf(ItemNbtUtil.getTag(stack)));
    }

    @Override
    public void registerGuiHandlers(@NotNull IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(MultiblockToolScreen.class, new IGuiContainerHandler<>() {
            @Override
            public @NotNull List<Rect2i> getGuiExtraAreas(@NotNull MultiblockToolScreen screen) {
                return screen.getTabAreas();
            }

            @Override
            public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(
                    IClickableIngredientFactory factory, MultiblockToolScreen screen, double mouseX, double mouseY) {
                ItemStack stack = screen.getJeiMaterialAt(mouseX, mouseY);
                Rect2i area = screen.getJeiMaterialAreaAt(mouseX, mouseY);
                if (stack == null || stack.isEmpty() || area == null) return Optional.empty();
                return factory.createBuilder(stack).buildWithArea(area);
            }
        });
        registration.addGuiContainerHandler(MachineControllerScreen.class, new IGuiContainerHandler<>() {
            @Override
            public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(
                    IClickableIngredientFactory factory, MachineControllerScreen screen, double mouseX, double mouseY) {
                var hovered = screen.getJeiIngredientAt(mouseX, mouseY);
                if (hovered == null) return Optional.empty();
                var content = hovered.content();
                return switch (content.kind()) {
                    case ITEM -> factory.createBuilder(content.item().copyWithCount(1)).buildWithArea(hovered.area());
                    case FLUID -> factory.createBuilder(NeoForgeTypes.FLUID_STACK, content.fluid().copy()).buildWithArea(hovered.area());
                    default -> Optional.empty();
                };
            }
        });
        registration.addGuiContainerHandler(SingleMachineScreen.class, new IGuiContainerHandler<>() {
            @Override
            public @NotNull Collection<IGuiClickableArea> getGuiClickableAreas(@NotNull SingleMachineScreen screen, double mouseX, double mouseY) {
                var type = recipeTypeFor(Ref.id(screen.getMenu().getModel().id()));
                if (type == null) return List.of();
                var layout = screen.getMenu().getLayout();
                return List.of(IGuiClickableArea.createBasic(layout.barX(), layout.barY() - 3,
                        SingleMachineLayout.BAR_W, SingleMachineLayout.BAR_H + 15, type));
            }
        });
    }

    private static RecipeType<RecipeModel> recipeTypeFor(ResourceLocation structureId) {
        if (!MMConfig.JEI_RECIPE_SPLIT) {
            return MMRecipeCategory.RECIPE_TYPE;
        }
        for (var category : recipeCategories) {
            if (category.getStructureModel() != null && category.getStructureModel().id().equals(structureId)) {
                return category.getRecipeType();
            }
        }
        return null;
    }
}

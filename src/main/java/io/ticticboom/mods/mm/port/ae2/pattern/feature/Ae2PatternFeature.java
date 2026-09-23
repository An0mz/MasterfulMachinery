package io.ticticboom.mods.mm.port.ae2.pattern.feature;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import io.ticticboom.mods.mm.compat.ae2.MatterKeys;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.port.ae2.pattern.register.Ae2PatternPortBlockEntity;
import io.ticticboom.mods.mm.port.fluid.FluidPortIngredient;
import io.ticticboom.mods.mm.port.fluid.FluidPortStorage;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.port.item.SingleItemPortIngredient;
import io.ticticboom.mods.mm.port.replication.matter.MatterTypes;
import io.ticticboom.mods.mm.port.replication.matter.ReplicationMatterPortIngredient;
import io.ticticboom.mods.mm.port.replication.matter.ReplicationMatterPortStorage;
import io.ticticboom.mods.mm.recipe.MachineRecipeManager;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.recipe.input.consume.ConsumeRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.output.simple.SimpleRecipeOutputEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Ae2PatternFeature implements ICraftingProvider {

    private static final int OUTPUT_INTERVAL = 5;
    private static final long PUSH_GRACE = 40;
    private static final boolean BRIDGE_LOADED = ModList.get().isLoaded("rep_ae2_bridge");

    private final Ae2PatternPortBlockEntity port;
    private final IManagedGridNode node;
    private final Map<AEItemKey, ResourceLocation> recipesByPattern = new HashMap<>();

    private List<IPatternDetails> patterns = List.of();
    private ResourceLocation patternStructure = null;
    private int patternRecipeVersion = -1;
    private ResourceLocation pushedRecipe = null;
    private long pushedAt = 0;

    public Ae2PatternFeature(Ae2PatternPortBlockEntity port) {
        this.port = port;
        this.node = GridHelper.createManagedNode(this, NodeListener.INSTANCE)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setInWorldNode(true)
                .setIdlePowerUsage(1)
                .setVisualRepresentation(port.getBlockState().getBlock())
                .addService(ICraftingProvider.class, this);
    }

    public IManagedGridNode getNode() {
        return node;
    }

    public void onLoad() {
        if (port.getLevel() instanceof ServerLevel) {
            GridHelper.onFirstTick(port, be -> node.create(be.getLevel(), be.getBlockPos()));
        }
    }

    public void onRemoved() {
        node.destroy();
    }

    public void saveNode(CompoundTag tag) {
        node.saveToNBT(tag);
    }

    public void loadNode(CompoundTag tag) {
        node.loadFromNBT(tag);
    }

    public void tick() {
        if (!(port.getLevel() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        var controller = port.getPatternStorage().getController(now);
        refreshPatterns(level, controller);
        if (now % OUTPUT_INTERVAL != 0 || controller == null) {
            return;
        }
        pushOutputs(level, controller);
        if (pushedRecipe != null && now - pushedAt > PUSH_GRACE && !controller.isRecipeRunning(pushedRecipe)) {
            pushedRecipe = null;
        }
    }

    private void refreshPatterns(ServerLevel level, MachineControllerBlockEntity controller) {
        var structure = controller == null ? null : controller.getStructure();
        var structureId = structure == null ? null : structure.id();
        if (Objects.equals(structureId, patternStructure) && patternRecipeVersion == MachineRecipeManager.RECIPE_VERSION) {
            return;
        }
        patternStructure = structureId;
        patternRecipeVersion = MachineRecipeManager.RECIPE_VERSION;
        recipesByPattern.clear();
        patterns = structureId == null ? List.of() : buildPatterns(level, structureId);
        ICraftingProvider.requestUpdate(node);
    }

    private List<IPatternDetails> buildPatterns(ServerLevel level, ResourceLocation structureId) {
        var result = new ArrayList<IPatternDetails>();
        for (RecipeModel recipe : MachineRecipeManager.getRecipesByStrucutreId(structureId)) {
            var inputs = patternInputs(recipe);
            var outputs = patternOutputs(recipe);
            if (inputs.isEmpty() || outputs.isEmpty()) {
                continue;
            }
            var encoded = PatternDetailsHelper.encodeProcessingPattern(inputs, outputs);
            var details = PatternDetailsHelper.decodePattern(encoded, level);
            if (details == null) {
                continue;
            }
            result.add(details);
            recipesByPattern.put(details.getDefinition(), recipe.id());
        }
        return List.copyOf(result);
    }

    private List<GenericStack> patternInputs(RecipeModel recipe) {
        var result = new ArrayList<GenericStack>();
        for (var entry : recipe.inputs().inputs()) {
            if (!(entry instanceof ConsumeRecipeIngredientEntry consume)) {
                continue;
            }
            var stack = asStack(consume.getIngredient());
            if (stack != null) {
                result.add(stack);
            }
        }
        return result;
    }

    private List<GenericStack> patternOutputs(RecipeModel recipe) {
        var result = new ArrayList<GenericStack>();
        for (var entry : recipe.outputs().outputs()) {
            if (!(entry instanceof SimpleRecipeOutputEntry simple)) {
                continue;
            }
            var stack = asStack(simple.getIngredient());
            if (stack != null) {
                result.add(stack);
            }
        }
        return result;
    }

    private GenericStack asStack(Object ingredient) {
        if (ingredient instanceof SingleItemPortIngredient item) {
            return new GenericStack(AEItemKey.of(item.outputStack()), item.getCount());
        }
        if (ingredient instanceof FluidPortIngredient fluid) {
            var found = BuiltInRegistries.FLUID.get(fluid.getFluidId());
            return found == null ? null : new GenericStack(AEFluidKey.of(found), fluid.getAmountRange().max());
        }
        if (BRIDGE_LOADED && ingredient instanceof ReplicationMatterPortIngredient matter) {
            var type = MatterTypes.get(matter.getMatterId());
            var key = type == null ? null : MatterKeys.of(type);
            return key == null ? null : new GenericStack(key, matter.getAmountRange().max());
        }
        return null;
    }

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        return patterns;
    }

    @Override
    public int getPatternPriority() {
        return port.getPatternStorage().getPatternPriority();
    }

    @Override
    public boolean isBusy() {
        return pushedRecipe != null;
    }

    @Override
    public boolean pushPattern(IPatternDetails details, KeyCounter[] inputHolder) {
        if (isBusy() || !(port.getLevel() instanceof ServerLevel level)) {
            return false;
        }
        long now = level.getGameTime();
        var controller = port.getPatternStorage().getController(now);
        if (controller == null) {
            return false;
        }
        var recipeId = recipesByPattern.get(details.getDefinition());
        var storages = controller.getPortStorages();
        if (recipeId == null || storages == null) {
            return false;
        }
        if (!insertInputs(storages, inputHolder, true)) {
            return false;
        }
        insertInputs(storages, inputHolder, false);
        controller.requestRecipe(this, recipeId, now, 1);
        pushedRecipe = recipeId;
        pushedAt = now;
        return true;
    }

    private boolean insertInputs(RecipeStorages storages, KeyCounter[] inputHolder, boolean simulate) {
        for (KeyCounter counter : inputHolder) {
            for (var entry : counter) {
                if (!insertKey(storages, entry.getKey(), entry.getLongValue(), simulate)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean insertKey(RecipeStorages storages, AEKey key, long amount, boolean simulate) {
        if (amount <= 0) {
            return true;
        }
        if (BRIDGE_LOADED) {
            var type = MatterKeys.typeOf(key);
            if (type != null) {
                int remaining = (int) amount;
                for (ReplicationMatterPortStorage storage : storages.getInputStorages(ReplicationMatterPortStorage.class)) {
                    remaining -= storage.internalInsert(type, remaining, simulate);
                    if (remaining <= 0) {
                        return true;
                    }
                }
                return false;
            }
        }
        if (key instanceof AEItemKey item) {
            int remaining = (int) amount;
            for (ItemPortStorage storage : storages.getInputStorages(ItemPortStorage.class)) {
                var stack = item.toStack(remaining);
                remaining = simulate ? storage.canInsert(stack, remaining) : storage.insert(stack, remaining);
                if (remaining <= 0) {
                    return true;
                }
            }
            return false;
        }
        if (key instanceof AEFluidKey fluid) {
            int remaining = (int) amount;
            var action = simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE;
            for (FluidPortStorage storage : storages.getInputStorages(FluidPortStorage.class)) {
                remaining -= storage.getWrappedHandler().fill(new FluidStack(fluid.getFluid(), remaining), action);
                if (remaining <= 0) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private void pushOutputs(ServerLevel level, MachineControllerBlockEntity controller) {
        var grid = node.getGrid();
        var storages = controller.getPortStorages();
        if (grid == null || storages == null || !node.isActive()) {
            return;
        }
        var me = grid.getStorageService().getInventory();
        var source = IActionSource.ofMachine(port);
        for (ItemPortStorage storage : storages.getOutputStorages(ItemPortStorage.class)) {
            var handler = storage.getHandler();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                long inserted = me.insert(AEItemKey.of(stack), handler.getActualCount(slot), Actionable.MODULATE, source);
                if (inserted > 0) {
                    handler.extractItem(slot, (int) inserted, false);
                }
            }
        }
        for (FluidPortStorage storage : storages.getOutputStorages(FluidPortStorage.class)) {
            var handler = storage.getWrappedHandler();
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                var stack = handler.getFluidInTank(tank);
                if (stack.isEmpty()) {
                    continue;
                }
                long inserted = me.insert(AEFluidKey.of(stack), stack.getAmount(), Actionable.MODULATE, source);
                if (inserted > 0) {
                    handler.drain(new FluidStack(stack.getFluid(), (int) inserted), IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }

    private static class NodeListener implements IGridNodeListener<Ae2PatternFeature> {
        private static final NodeListener INSTANCE = new NodeListener();

        @Override
        public void onSaveChanges(Ae2PatternFeature owner, IGridNode node) {
            owner.port.setChanged();
        }
    }
}

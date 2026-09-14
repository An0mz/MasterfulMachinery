package io.ticticboom.mods.mm.port.replication.link.feature;

import com.buuz135.replication.api.pattern.MatterPattern;
import com.buuz135.replication.api.task.IReplicationTask;
import com.buuz135.replication.calculation.ReplicationCalculation;
import com.buuz135.replication.network.DefaultMatterNetworkElement;
import com.buuz135.replication.network.MatterNetwork;
import com.hrznstudio.titanium.block_network.NetworkManager;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.port.replication.link.ReplicationLinkRecipes;
import io.ticticboom.mods.mm.port.replication.link.register.ReplicationLinkPortBlockEntity;
import io.ticticboom.mods.mm.recipe.MachineRecipeManager;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ReplicationLinkFeature {

    private static final int TICK_INTERVAL = 5;

    private final ReplicationLinkPortBlockEntity port;
    private boolean joined = false;
    private boolean unloaded = false;
    private List<MatterPattern> patterns = List.of();
    private ResourceLocation patternStructure = null;
    private int patternRecipeVersion = -1;
    private String taskId = null;

    public ReplicationLinkFeature(ReplicationLinkPortBlockEntity port) {
        this.port = port;
    }

    public List<MatterPattern> getPatterns() {
        return patterns;
    }

    public void onLoad() {
        if (!(port.getLevel() instanceof ServerLevel level)) {
            return;
        }
        var manager = NetworkManager.get(level);
        if (manager.getElement(port.getBlockPos()) == null) {
            manager.addElement(new DefaultMatterNetworkElement(level, port.getBlockPos()));
        }
        joined = true;
    }

    public void onChunkUnloaded() {
        unloaded = true;
    }

    public void onRemoved() {
        if (unloaded || !joined) {
            return;
        }
        if (!(port.getLevel() instanceof ServerLevel level)) {
            return;
        }
        var manager = NetworkManager.get(level);
        var element = manager.getElement(port.getBlockPos());
        manager.removeElement(port.getBlockPos());
        if (element != null && element.getNetwork() instanceof MatterNetwork matterNetwork) {
            matterNetwork.removeElement(element);
        }
        joined = false;
    }

    public void tick() {
        if (!(port.getLevel() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        if (now % TICK_INTERVAL != 0) {
            return;
        }
        var controller = port.getLinkStorage().getController(now);
        var network = network(level);
        refreshPatterns(network, controller);
        if (controller == null || network == null) {
            taskId = null;
            return;
        }
        var task = currentTask(level, network, controller);
        if (task == null) {
            return;
        }
        var recipe = ReplicationLinkRecipes.recipeFor(controller.getStructure(), task.getReplicatingStack());
        if (recipe == null) {
            taskId = null;
            return;
        }
        deliver(level, network, controller, task);
        if (taskId == null) {
            return;
        }
        int remaining = task.getTotalAmount() - task.getCurrentAmount();
        int waiting = countWaiting(controller, task.getReplicatingStack());
        int inFlight = controller.isRecipeRunning(recipe.id()) ? 1 : 0;
        controller.requestRecipe(recipe.id(), now, remaining - waiting - inFlight);
    }

    private int countWaiting(MachineControllerBlockEntity controller, ItemStack wanted) {
        var storages = controller.getPortStorages();
        if (storages == null) {
            return 0;
        }
        int found = 0;
        for (ItemPortStorage storage : storages.getOutputStorages(ItemPortStorage.class)) {
            var handler = storage.getHandler();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                if (ItemStack.isSameItemSameComponents(handler.getStackInSlot(slot), wanted)) {
                    found += handler.getActualCount(slot);
                }
            }
        }
        return found;
    }

    private void refreshPatterns(MatterNetwork network, MachineControllerBlockEntity controller) {
        var structure = controller == null ? null : controller.getStructure();
        var structureId = structure == null ? null : structure.id();
        if (Objects.equals(structureId, patternStructure) && patternRecipeVersion == MachineRecipeManager.RECIPE_VERSION) {
            return;
        }
        patternStructure = structureId;
        patternRecipeVersion = MachineRecipeManager.RECIPE_VERSION;
        patterns = structureId == null ? List.of() : ReplicationLinkRecipes.outputs(structureId).stream()
                .map(stack -> new MatterPattern(stack, 1f))
                .toList();
        if (network != null) {
            network.onChipValuesChanged(port, port.getBlockPos());
        }
    }

    private IReplicationTask currentTask(ServerLevel level, MatterNetwork network, MachineControllerBlockEntity controller) {
        var pending = network.getTaskManager().getPendingTasks();
        if (taskId != null) {
            var task = pending.get(taskId);
            if (task != null) {
                claim(level, network, task);
                return task;
            }
            taskId = null;
        }
        for (Map.Entry<String, IReplicationTask> entry : pending.entrySet()) {
            var task = entry.getValue();
            if (task.getCurrentAmount() >= task.getTotalAmount()) {
                continue;
            }
            if (ReplicationLinkRecipes.recipeFor(controller.getStructure(), task.getReplicatingStack()) == null) {
                continue;
            }
            boolean replicable = ReplicationCalculation.getMatterCompound(task.getReplicatingStack()) != null;
            if (replicable && !isOnTask(task) && !task.canAcceptReplicator(port.getBlockPos(), network.getReplicators().size() + 1)) {
                continue;
            }
            taskId = entry.getKey();
            claim(level, network, task);
            return task;
        }
        return null;
    }

    private boolean isOnTask(IReplicationTask task) {
        return task.getReplicatorsOnTask().contains(port.getBlockPos().asLong());
    }

    private void claim(ServerLevel level, MatterNetwork network, IReplicationTask task) {
        if (!isOnTask(task)) {
            task.acceptReplicator(port.getBlockPos());
            network.onTaskValueChanged(task, level);
        }
    }

    private void deliver(ServerLevel level, MatterNetwork network, MachineControllerBlockEntity controller, IReplicationTask task) {
        var storages = controller.getPortStorages();
        if (storages == null) {
            return;
        }
        var target = level.getCapability(Capabilities.ItemHandler.BLOCK, task.getSource(), Direction.UP);
        if (target == null) {
            return;
        }
        var wanted = task.getReplicatingStack();
        int needed = task.getTotalAmount() - task.getCurrentAmount();
        int delivered = 0;
        for (ItemPortStorage storage : storages.getOutputStorages(ItemPortStorage.class)) {
            var handler = storage.getHandler();
            for (int slot = 0; slot < handler.getSlots() && delivered < needed; slot++) {
                if (!ItemStack.isSameItemSameComponents(handler.getStackInSlot(slot), wanted)) {
                    continue;
                }
                var offered = handler.extractItem(slot, needed - delivered, true);
                if (offered.isEmpty()) {
                    continue;
                }
                int fits = offered.getCount() - ItemHandlerHelper.insertItemStacked(target, offered, true).getCount();
                if (fits <= 0) {
                    break;
                }
                var taken = handler.extractItem(slot, fits, false);
                var leftover = ItemHandlerHelper.insertItemStacked(target, taken, false);
                if (!leftover.isEmpty()) {
                    storage.insert(leftover, leftover.getCount());
                }
                delivered += taken.getCount() - leftover.getCount();
            }
        }
        if (delivered <= 0) {
            return;
        }
        for (int i = 0; i < delivered; i++) {
            task.finalizeReplication(level, port.getBlockPos(), network);
        }
        if (network.getTaskManager().getPendingTasks().containsKey(taskId)) {
            task.acceptReplicator(port.getBlockPos());
        } else {
            taskId = null;
        }
        network.onTaskValueChanged(task, level);
    }

    private MatterNetwork network(ServerLevel level) {
        var element = NetworkManager.get(level).getElement(port.getBlockPos());
        if (element == null) {
            return null;
        }
        return element.getNetwork() instanceof MatterNetwork matterNetwork ? matterNetwork : null;
    }
}

package io.ticticboom.mods.mm.port.replication.matter.feature;

import com.buuz135.replication.api.IMatterType;
import com.buuz135.replication.api.matter_fluid.IMatterTank;
import com.buuz135.replication.api.matter_fluid.MatterStack;
import com.buuz135.replication.api.network.IMatterTanksSupplier;
import com.buuz135.replication.network.DefaultMatterNetworkElement;
import com.buuz135.replication.network.MatterNetwork;
import com.hrznstudio.titanium.block_network.NetworkManager;
import io.ticticboom.mods.mm.port.replication.matter.ReplicationMatterPortStorage;
import io.ticticboom.mods.mm.port.replication.matter.ReplicationMatterPortTank;
import io.ticticboom.mods.mm.port.replication.matter.register.ReplicationMatterPortBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class ReplicationMatterNetworkFeature {

    private static final int PULL_INTERVAL = 5;

    private final ReplicationMatterPortBlockEntity portBlockEntity;
    private boolean joined = false;
    private boolean unloaded = false;

    public ReplicationMatterNetworkFeature(ReplicationMatterPortBlockEntity portBlockEntity) {
        this.portBlockEntity = portBlockEntity;
    }

    public void onLoad() {
        if (!(portBlockEntity.getLevel() instanceof ServerLevel level)) {
            return;
        }
        var manager = NetworkManager.get(level);
        if (manager.getElement(portBlockEntity.getBlockPos()) == null) {
            manager.addElement(new DefaultMatterNetworkElement(level, portBlockEntity.getBlockPos()));
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
        if (!(portBlockEntity.getLevel() instanceof ServerLevel level)) {
            return;
        }
        var manager = NetworkManager.get(level);
        var element = manager.getElement(portBlockEntity.getBlockPos());
        manager.removeElement(portBlockEntity.getBlockPos());
        if (element != null && element.getNetwork() instanceof MatterNetwork matterNetwork) {
            matterNetwork.removeElement(element);
        }
        joined = false;
    }

    /**
     * Output ports are suppliers, and the network empties those into its tanks on its own. Input
     * ports are consumers, which the network never fills, so they go and take what they are short
     * of the way a replicator does.
     */
    public void tick() {
        if (!portBlockEntity.isInput()) {
            return;
        }
        if (!(portBlockEntity.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (level.getGameTime() % PULL_INTERVAL != 0) {
            return;
        }
        if (!(portBlockEntity.getStorage() instanceof ReplicationMatterPortStorage storage)) {
            return;
        }
        var network = network(level);
        if (network == null) {
            return;
        }
        for (ReplicationMatterPortTank tank : storage.getHandler().tanks()) {
            var type = tank.getRequestedType();
            if (type == null) {
                continue;
            }
            double wanted = tank.getCapacity() - tank.getMatterAmount();
            if (wanted <= 0) {
                continue;
            }
            pull(level, network, tank, type, wanted);
        }
    }

    private void pull(ServerLevel level, MatterNetwork network, ReplicationMatterPortTank tank, IMatterType type, double wanted) {
        for (var element : network.getMatterStacksHolders()) {
            if (wanted <= 0) {
                return;
            }
            if (element.getLevel() != level || !level.isLoaded(element.getPos())) {
                continue;
            }
            if (element.getPos().equals(portBlockEntity.getBlockPos())) {
                continue;
            }
            if (!(level.getBlockEntity(element.getPos()) instanceof IMatterTanksSupplier supplier)) {
                continue;
            }
            for (IMatterTank source : supplier.getTanks()) {
                if (wanted <= 0) {
                    return;
                }
                if (source.getMatter().isEmpty() || source.getMatter().getMatterType() != type) {
                    continue;
                }
                var available = source.drain(new MatterStack(type, wanted), IFluidHandler.FluidAction.SIMULATE);
                if (available.isEmpty()) {
                    continue;
                }
                double accepted = tank.fill(available, IFluidHandler.FluidAction.SIMULATE);
                if (accepted <= 0) {
                    continue;
                }
                var taken = source.drain(new MatterStack(type, accepted), IFluidHandler.FluidAction.EXECUTE);
                if (taken.isEmpty()) {
                    continue;
                }
                wanted -= tank.fill(taken, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    private MatterNetwork network(ServerLevel level) {
        var element = NetworkManager.get(level).getElement(portBlockEntity.getBlockPos());
        if (element == null) {
            return null;
        }
        return element.getNetwork() instanceof MatterNetwork matterNetwork ? matterNetwork : null;
    }
}

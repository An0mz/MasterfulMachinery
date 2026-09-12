package io.ticticboom.mods.mm.port.energy.feature;

import io.ticticboom.mods.mm.cap.MMCapabilities;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.common.AbstractPortAutoPushFeature;
import io.ticticboom.mods.mm.port.energy.register.EnergyPortBlockEntity;
import io.ticticboom.mods.mm.port.energy.EnergyPortRouting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.HashMap;
import java.util.Map;

public class EnergyPortAutoPushFeature extends AbstractPortAutoPushFeature<EnergyHandlerCoupling> {

    public EnergyPortAutoPushFeature(EnergyPortBlockEntity portBlockEntity, PortModel model) {
        super(portBlockEntity, model);
    }

    public void tick() {
        var level = portBlockEntity.getLevel();
        if (level == null) {
            return;
        }

        if (level.isClientSide) {
            return;
        }

        // Build candidates
        Map<BlockPos, io.ticticboom.mods.mm.port.energy.EnergyPortStorage> candidates = new HashMap<>();
        for (BlockPos pos : autoPushNeighbors.keySet()) {
            BlockEntity be = portBlockEntity.getLevel().getBlockEntity(pos);
            if (be instanceof IPortBlockEntity pbe) {
                var storage = pbe.getStorage();
                if (storage instanceof io.ticticboom.mods.mm.port.energy.EnergyPortStorage eps) {
                    candidates.put(pos, eps);
                }
            }
        }

        if (!(portBlockEntity instanceof IPortBlockEntity sourcePbe)) return;
        var sourceStorage = sourcePbe.getStorage();
        if (!(sourceStorage instanceof io.ticticboom.mods.mm.port.energy.EnergyPortStorage source)) return;

        long available = source.getStoredEnergy();
        if (available > 0) {
            EnergyPortRouting.distributeFillThrough(available, source, candidates);
        }
        pushToExternalNeighbours(candidates);
    }

    private void pushToExternalNeighbours(Map<BlockPos, io.ticticboom.mods.mm.port.energy.EnergyPortStorage> handledByRouting) {
        for (var entry : autoPushNeighbors.entrySet()) {
            if (handledByRouting.containsKey(entry.getKey())) {
                continue;
            }
            entry.getValue().attemptTransfer();
        }
    }

    @Override
    protected void tryAddNeighborHandler(BlockPos neighborPos, Direction neighborFace) {
        BlockEntity neighborBe = portBlockEntity.getLevel().getBlockEntity(neighborPos);
        if (neighborBe == null) {
            return;
        }

        var valid = canAddAsNeighbor(neighborPos, neighborBe);
        if (!valid) {
            return;
        }

        // A BlockCapabilityCache re-resolves when the neighbour is replaced or removed, which is
        // what LazyOptional invalidation used to provide.
        if (!(neighborBe.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (serverLevel.getCapability(MMCapabilities.ENERGY, neighborPos, neighborFace) == null) {
            return;
        }
        var neighborCap = BlockCapabilityCache.create(MMCapabilities.ENERGY, serverLevel, neighborPos, neighborFace);

        if (autoPushNeighbors.containsKey(neighborPos)) {
            EnergyHandlerCoupling pairing = autoPushNeighbors.get(neighborPos);
            pairing.setToHandler(neighborCap);
        } else {
            // The block entity no longer answers capability queries itself, so ask its storage.
            var capability = this.portBlockEntity instanceof IPortBlockEntity port
                    ? port.getStorage().getCapability(MMCapabilities.ENERGY)
                    : null;
            autoPushNeighbors.put(neighborPos, new EnergyHandlerCoupling(capability, neighborCap));
        }
    }

    private boolean canAddAsNeighbor(BlockPos pos, BlockEntity be) {
        if (pos == null) return false; // reference pos to avoid unused-parameter warning
        if (be instanceof IPortBlockEntity pbe) {
            return pbe.isInput();
        }
        return true;
    }
}

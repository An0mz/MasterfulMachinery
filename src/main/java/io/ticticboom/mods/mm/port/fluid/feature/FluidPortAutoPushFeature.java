package io.ticticboom.mods.mm.port.fluid.feature;

import io.ticticboom.mods.mm.cap.MMCapabilities;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.common.AbstractPortAutoPushFeature;
import io.ticticboom.mods.mm.port.fluid.FluidPortRouting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.HashMap;
import java.util.Map;

public class FluidPortAutoPushFeature extends AbstractPortAutoPushFeature<FluidHandlerCoupling> {

    public FluidPortAutoPushFeature(BlockEntity portBlockEntity, PortModel model) {
        super(portBlockEntity, model);
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
        if (serverLevel.getCapability(MMCapabilities.FLUID, neighborPos, neighborFace) == null) {
            return;
        }
        var neighborCap = BlockCapabilityCache.create(MMCapabilities.FLUID, serverLevel, neighborPos, neighborFace);

        if (autoPushNeighbors.containsKey(neighborPos)) {
            FluidHandlerCoupling pairing = autoPushNeighbors.get(neighborPos);
            pairing.setToHandler(neighborCap);
        } else {
            // The block entity no longer answers capability queries itself, so ask its storage.
            var capability = this.portBlockEntity instanceof IPortBlockEntity port
                    ? port.getStorage().getCapability(MMCapabilities.FLUID)
                    : null;
            autoPushNeighbors.put(neighborPos, new FluidHandlerCoupling(capability, neighborCap));
        }
    }

    private boolean canAddAsNeighbor(BlockPos pos, BlockEntity be) {
        // reference pos to avoid unused-parameter warnings in static analysis
        if (pos == null) return false;
        if (be instanceof IPortBlockEntity pbe) {
            return pbe.isInput();
        }
        return true;
    }

    @Override
    public void tick() {
        var level = portBlockEntity.getLevel();
        if (level == null) return;
        if (level.isClientSide) return;

        Map<BlockPos, io.ticticboom.mods.mm.port.fluid.FluidPortStorage> candidates = new HashMap<>();
        for (BlockPos pos : autoPushNeighbors.keySet()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof IPortBlockEntity pbe) {
                var storage = pbe.getStorage();
                if (storage instanceof io.ticticboom.mods.mm.port.fluid.FluidPortStorage fps) {
                    candidates.put(pos, fps);
                }
            }
        }

        if (!(portBlockEntity instanceof IPortBlockEntity sourcePbe)) return;
        var sourceStorage = sourcePbe.getStorage();
        if (!(sourceStorage instanceof io.ticticboom.mods.mm.port.fluid.FluidPortStorage source)) return;

        // For each tank in the source, attempt to push fluid
        for (int tank = 0; tank < source.getHandler().getTanks(); tank++) {
            var f = source.getHandler().getFluidInTank(tank);
            if (f.isEmpty()) continue;
            int amount = f.getAmount();
            FluidPortRouting.distributeFillThrough(f, amount, source, candidates);
        }
    }
}

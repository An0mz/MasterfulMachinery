package io.ticticboom.mods.mm.port.mekanism.chemical.feature;

import io.ticticboom.mods.mm.cap.MekCapabilities;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorage;
import io.ticticboom.mods.mm.port.mekanism.chemical.register.MekanismChemicalPortBlockEntity;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.IChemicalHandler;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;

import java.util.EnumMap;
import java.util.Map;

public class MekanismChemicalPortPushFeature {

    private final MekanismChemicalPortBlockEntity portBlockEntity;
    private final Map<Direction, BlockCapabilityCache<IChemicalHandler, Direction>> neighbours = new EnumMap<>(Direction.class);
    private boolean resolved = false;

    public MekanismChemicalPortPushFeature(MekanismChemicalPortBlockEntity portBlockEntity) {
        this.portBlockEntity = portBlockEntity;
    }

    public void onLoad() {
        neighborsChanged();
    }

    public void neighborsChanged() {
        neighbours.clear();
        resolved = false;
    }

    public void tick() {
        if (!(portBlockEntity.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!resolved) {
            resolve(level);
        }
        if (neighbours.isEmpty() || !(portBlockEntity.getStorage() instanceof MekanismChemicalPortStorage storage)) {
            return;
        }
        var tank = storage.chemicalTank;
        for (var cache : neighbours.values()) {
            if (tank.isEmpty()) {
                return;
            }
            var adjacent = cache.getCapability();
            if (adjacent == null) {
                continue;
            }
            var offered = tank.extract(tank.getStored(), Action.SIMULATE, AutomationType.INTERNAL);
            if (offered.isEmpty()) {
                continue;
            }
            var leftover = adjacent.insertChemical(offered, Action.EXECUTE);
            long moved = offered.getAmount() - (leftover.isEmpty() ? 0 : leftover.getAmount());
            if (moved > 0) {
                tank.extract(moved, Action.EXECUTE, AutomationType.INTERNAL);
            }
        }
    }

    private void resolve(ServerLevel level) {
        resolved = true;
        neighbours.clear();
        var pos = portBlockEntity.getBlockPos();
        for (Direction direction : Direction.values()) {
            var neighbourPos = pos.relative(direction);
            if (level.getBlockEntity(neighbourPos) instanceof IPortBlockEntity port && !port.isInput()) {
                continue;
            }
            if (level.getCapability(MekCapabilities.CHEMICAL, neighbourPos, direction.getOpposite()) == null) {
                continue;
            }
            neighbours.put(direction, BlockCapabilityCache.create(MekCapabilities.CHEMICAL, level, neighbourPos, direction.getOpposite()));
        }
    }
}

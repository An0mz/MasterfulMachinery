package io.ticticboom.mods.mm.gateway;

import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.setup.MMRegisters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class InputGatewayBlockEntity extends BlockEntity {
    private static final int REFRESH_TICKS = 20;
    private static final int RELINK_EVERY = 10;
    private static final int SEARCH_RADIUS = 16;

    private final IItemHandler itemHandler = new GatewayItemHandler(this::inputs);
    private final IFluidHandler fluidHandler = new GatewayFluidHandler(this::inputs);
    private final IEnergyStorage energyHandler = new GatewayEnergyHandler(this::inputs);
    private Object chemicalHandler;

    private List<IPortStorage> cachedInputs = List.of();
    private long refreshAt = Long.MIN_VALUE;
    private int refreshCount = 0;
    @Nullable
    private BlockPos controllerPos;

    public InputGatewayBlockEntity(BlockPos pos, BlockState state) {
        super(MMRegisters.INPUT_GATEWAY_BE.get(), pos, state);
    }

    public IItemHandler itemHandler() {
        return itemHandler;
    }

    public IFluidHandler fluidHandler() {
        return fluidHandler;
    }

    public IEnergyStorage energyHandler() {
        return energyHandler;
    }

    public Object chemicalHandler() {
        if (chemicalHandler == null) {
            chemicalHandler = new GatewayChemicalHandler(this::inputs);
        }
        return chemicalHandler;
    }

    List<IPortStorage> inputs() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return List.of();
        }
        long now = serverLevel.getGameTime();
        if (now < refreshAt) {
            return cachedInputs;
        }
        refreshAt = now + REFRESH_TICKS;
        if (++refreshCount % RELINK_EVERY == 0) {
            controllerPos = null;
        }
        MachineControllerBlockEntity controller = linkedController(serverLevel);
        if (controller == null) {
            controller = searchController(serverLevel);
            controllerPos = controller == null ? null : controller.getBlockPos();
        }
        var storages = controller == null ? null : controller.getPortStorages();
        cachedInputs = storages == null ? List.of() : storages.inputStorages();
        return cachedInputs;
    }

    @Nullable
    private MachineControllerBlockEntity linkedController(ServerLevel serverLevel) {
        if (controllerPos != null && serverLevel.isLoaded(controllerPos)
                && serverLevel.getBlockEntity(controllerPos) instanceof MachineControllerBlockEntity controller
                && controller.getPortStorages() != null) {
            return controller;
        }
        return null;
    }

    @Nullable
    private MachineControllerBlockEntity searchController(ServerLevel serverLevel) {
        BlockPos self = getBlockPos();
        for (Direction dir : Direction.values()) {
            BlockPos portPos = self.relative(dir);
            if (serverLevel.isLoaded(portPos) && serverLevel.getBlockEntity(portPos) instanceof AbstractPortBlockEntity port
                    && port.isInput() && port.getControllerPos() != null
                    && serverLevel.getBlockEntity(port.getControllerPos()) instanceof MachineControllerBlockEntity controller
                    && controller.getPortStorages() != null) {
                return controller;
            }
        }
        for (int cx = (self.getX() - SEARCH_RADIUS) >> 4; cx <= (self.getX() + SEARCH_RADIUS) >> 4; cx++) {
            for (int cz = (self.getZ() - SEARCH_RADIUS) >> 4; cz <= (self.getZ() + SEARCH_RADIUS) >> 4; cz++) {
                var chunk = serverLevel.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (var be : chunk.getBlockEntities().values()) {
                    if (be instanceof MachineControllerBlockEntity controller && be.getBlockPos().closerThan(self, SEARCH_RADIUS + 1)
                            && controller.getPortStorages() != null && controller.getFormedRotation() != null
                            && controller.getStructure().layout().contains(controller.getBlockPos(), controller.getFormedRotation(), self)) {
                        return controller;
                    }
                }
            }
        }
        return null;
    }
}

package io.ticticboom.mods.mm.cap;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.items.IItemHandler;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class MMCapabilities {
    public static final Capability<IItemHandler> ITEM = CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<IFluidHandler> FLUID = CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<IEnergyStorage> ENERGY = CapabilityManager.get(new CapabilityToken<>() {});
}

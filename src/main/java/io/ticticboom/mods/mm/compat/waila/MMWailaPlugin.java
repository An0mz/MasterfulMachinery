package io.ticticboom.mods.mm.compat.waila;

import net.minecraft.world.level.block.Block;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlock;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.item.register.ItemPortBlock;
import io.ticticboom.mods.mm.port.fluid.register.FluidPortBlock;
import io.ticticboom.mods.mm.port.energy.register.EnergyPortBlock;
import io.ticticboom.mods.mm.port.kinetic.register.CreateKineticPortBlock;
import io.ticticboom.mods.mm.port.pneumaticcraft.air.register.PneumaticAirPortBlock;
import io.ticticboom.mods.mm.port.botania.mana.register.BotaniaManaPortBlock;
import io.ticticboom.mods.mm.port.replication.matter.register.ReplicationMatterPortBlock;
import net.neoforged.fml.ModList;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import io.ticticboom.mods.mm.port.ars.source.register.ArsSourcePortBlock;
import io.ticticboom.mods.mm.port.entity.register.EntityPortBlock;
import io.ticticboom.mods.mm.port.mekanism.chemical.register.MekanismChemicalPortBlock;
import io.ticticboom.mods.mm.port.mekanism.heat.register.MekanismHeatPortBlock;
import io.ticticboom.mods.mm.port.nuclear.radiation.register.NuclearRadiationPortBlock;


@WailaPlugin
public class MMWailaPlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(ControllerDataProvider.INSTANCE, MachineControllerBlockEntity.class);
        registration.registerBlockDataProvider(PortPriorityDataProvider.INSTANCE, AbstractPortBlockEntity.class);
        registration.registerBlockDataProvider(PortContentsDataProvider.INSTANCE, AbstractPortBlockEntity.class);
        registration.registerBlockDataProvider(PortMachineDataProvider.INSTANCE, AbstractPortBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(ControllerDataProvider.INSTANCE, MachineControllerBlock.class);
        registration.registerBlockComponent(PortMachineDataProvider.INSTANCE, Block.class);

        // register port priority tooltip for known port block classes
        registration.registerBlockComponent(PortPriorityDataProvider.INSTANCE, ItemPortBlock.class);
        registration.registerBlockComponent(PortPriorityDataProvider.INSTANCE, FluidPortBlock.class);
        registration.registerBlockComponent(PortPriorityDataProvider.INSTANCE, EnergyPortBlock.class);
        registration.registerBlockComponent(PortPriorityDataProvider.INSTANCE, CreateKineticPortBlock.class);
        registration.registerBlockComponent(PortPriorityDataProvider.INSTANCE, PneumaticAirPortBlock.class);
        registration.registerBlockComponent(PortPriorityDataProvider.INSTANCE, BotaniaManaPortBlock.class);

        registration.registerBlockComponent(PortContentsDataProvider.INSTANCE, EntityPortBlock.class);
        if (ModList.get().isLoaded("mekanism")) {
            registration.registerBlockComponent(PortContentsDataProvider.INSTANCE, MekanismChemicalPortBlock.class);
            registration.registerBlockComponent(PortContentsDataProvider.INSTANCE, MekanismHeatPortBlock.class);
        }
        // Unlike the other port blocks this one implements a Titanium interface, so naming the
        // class at all resolves it. Behind the check it stays unloaded when Replication is absent.
        if (ModList.get().isLoaded("replication")) {
            registration.registerBlockComponent(PortPriorityDataProvider.INSTANCE, ReplicationMatterPortBlock.class);
            registration.registerBlockComponent(PortContentsDataProvider.INSTANCE, ReplicationMatterPortBlock.class);
        }
        if (ModList.get().isLoaded("ars_nouveau")) {
            registration.registerBlockComponent(PortContentsDataProvider.INSTANCE, ArsSourcePortBlock.class);
        }
        if (ModList.get().isLoaded("nuclear_radiation")) {
            registration.registerBlockComponent(PortContentsDataProvider.INSTANCE, NuclearRadiationPortBlock.class);
        }
    }
}

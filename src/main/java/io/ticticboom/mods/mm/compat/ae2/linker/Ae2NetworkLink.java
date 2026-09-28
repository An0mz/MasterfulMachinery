package io.ticticboom.mods.mm.compat.ae2.linker;

import appeng.api.features.GridLinkables;
import appeng.api.networking.security.IActionSource;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.networklink.LinkData;
import io.ticticboom.mods.mm.networklink.NetworkLinkProtection;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.setup.MMRegisters;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class Ae2NetworkLink {

    private Ae2NetworkLink() {
    }

    public static DeferredHolder<Item, Item> init(IEventBus modBus) {
        DeferredHolder<Item, Item> linker = MMRegisters.ITEMS.register("network_linker", () -> new LinkerItem(new Item.Properties().stacksTo(1)));
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> GridLinkables.register(linker.get(), new LinkerGridLinkable())));
        NeoForge.EVENT_BUS.register(new NetworkLinkProtection());
        return linker;
    }

    public static void exportOutputs(ServerLevel level, MachineControllerBlockEntity controller, LinkData link) {
        var storages = controller.getPortStorages();
        if (storages == null || storages.outputStorages().isEmpty()) {
            return;
        }
        var network = NetworkAccess.storage(level.getServer(), link.network());
        if (network == null) {
            return;
        }
        for (IPortStorage output : storages.outputStorages()) {
            PortDrainer.drain(output, network, IActionSource.empty());
        }
    }

    public static void sendPortContents(ServerLevel level, BlockPos pos, IPortBlockEntity port, LinkData link) {
        try {
            var network = NetworkAccess.storage(level.getServer(), link.network());
            if (network != null) {
                PortDrainer.drain(port.getStorage(), network, IActionSource.empty());
            }
        } catch (RuntimeException e) {
            Ref.LOG.error("Failed to send port contents at {} to the AE2 network", pos, e);
        }
    }
}

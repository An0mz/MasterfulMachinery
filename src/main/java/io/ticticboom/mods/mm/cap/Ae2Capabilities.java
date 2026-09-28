package io.ticticboom.mods.mm.cap;

import appeng.api.AECapabilities;
import appeng.api.networking.IInWorldGridNodeHost;
import io.ticticboom.mods.mm.compat.ae2.Ae2ItemPortStorage;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.port.item.register.ItemPortBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class Ae2Capabilities {

    public static void registerNodeHost(RegisterCapabilitiesEvent event, BlockEntityType<?> beType) {
        @SuppressWarnings("unchecked")
        var typed = (BlockEntityType<BlockEntity>) beType;
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, typed,
                (be, context) -> be instanceof IInWorldGridNodeHost host ? host : null);
    }

    public static void registerItemPortStorage(RegisterCapabilitiesEvent event, BlockEntityType<?> beType) {
        @SuppressWarnings("unchecked")
        var typed = (BlockEntityType<BlockEntity>) beType;
        event.registerBlockEntity(AECapabilities.ME_STORAGE, typed,
                (be, context) -> be instanceof ItemPortBlockEntity port && port.getStorage() instanceof ItemPortStorage storage
                        ? new Ae2ItemPortStorage(storage.getHandler()) : null);
    }
}

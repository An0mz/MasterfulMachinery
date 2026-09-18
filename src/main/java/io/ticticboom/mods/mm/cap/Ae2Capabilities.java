package io.ticticboom.mods.mm.cap;

import appeng.api.AECapabilities;
import appeng.api.networking.IInWorldGridNodeHost;
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
}

package io.ticticboom.mods.mm.cap;

import io.ticticboom.mods.mm.port.botania.mana.register.BotaniaManaPortBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class BotaniaCapabilities {

    @SuppressWarnings("unchecked")
    public static void registerManaReceiver(RegisterCapabilitiesEvent event, BlockEntityType<?> beType) {
        event.registerBlockEntity(velrondevs.botania.api.BotaniaCapabilities.MANA_RECEIVER, (BlockEntityType<BlockEntity>) beType,
                (be, side) -> be instanceof BotaniaManaPortBlockEntity pool ? pool : null);
    }
}

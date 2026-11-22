package io.ticticboom.mods.mm.client.compat.botania;

import io.ticticboom.mods.mm.Ref;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = Ref.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class BotaniaClientCapabilityEvents {

    private BotaniaClientCapabilityEvents() {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        if (ModList.get().isLoaded("botania")) {
            BotaniaManaPortWandHud.register(event);
        }
    }
}

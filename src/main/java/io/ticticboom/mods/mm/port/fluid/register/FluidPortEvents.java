package io.ticticboom.mods.mm.port.fluid.register;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class FluidPortEvents {

    @SubscribeEvent
    public static void onForeground(ContainerScreenEvent.Render.Foreground event) {
        if (event.getContainerScreen() instanceof FluidPortScreen fps) {
            fps.renderFluids(event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
        }
    }
}

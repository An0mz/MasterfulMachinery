package io.ticticboom.mods.mm.client.geo;

import com.mojang.blaze3d.vertex.PoseStack;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.ControllerState;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

public interface GeoBridge {

    void render(MachineControllerBlockEntity be, GeoModelConfig config, ControllerState state, PoseStack pose,
                MultiBufferSource buffers, int light, float partialTick);

    @Nullable
    static GeoBridge get() {
        return Holder.INSTANCE;
    }

    final class Holder {
        private static final GeoBridge INSTANCE = create();

        private Holder() {
        }

        @Nullable
        private static GeoBridge create() {
            if (!ModList.get().isLoaded("geckolib")) {
                return null;
            }
            try {
                return new io.ticticboom.mods.mm.compat.geckolib.GeckoLibCompat();
            } catch (LinkageError e) {
                Ref.LOG.warn("GeckoLib is installed but MM can't use it, machine models are off", e);
                return null;
            }
        }
    }
}

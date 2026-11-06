package io.ticticboom.mods.mm.port.thaumaturge.essentia;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import org.jetbrains.annotations.Nullable;

final class EssentiaClientRegistries {

    private EssentiaClientRegistries() {
    }

    @Nullable
    static HolderLookup.Provider current() {
        var level = Minecraft.getInstance().level;
        return level == null ? null : level.registryAccess();
    }
}

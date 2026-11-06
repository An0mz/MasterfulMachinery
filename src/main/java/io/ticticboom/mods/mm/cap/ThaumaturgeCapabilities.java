package io.ticticboom.mods.mm.cap;

import com.leclowndu93150.thaumaturge.api.aspect.AspectCapabilities;
import com.leclowndu93150.thaumaturge.api.aspect.IAspectContainer;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaCapabilities;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaConduit;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaStorage;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;

public class ThaumaturgeCapabilities {
    public static final BlockCapability<IEssentiaConduit, Direction> ESSENTIA_CONDUIT = EssentiaCapabilities.CONDUIT;
    public static final BlockCapability<IEssentiaStorage, Direction> ESSENTIA_STORAGE = EssentiaCapabilities.STORAGE;
    public static final BlockCapability<IAspectContainer, Direction> ESSENTIA_CONTAINER = AspectCapabilities.CONTAINER;
}

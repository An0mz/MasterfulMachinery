package io.ticticboom.mods.mm.cap;

import mekanism.api.chemical.IChemicalHandler;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;

/**
 * Mekanism 1.21.1 merged the gas, slurry, pigment and infusion capabilities into a single chemical
 * capability, so the four separate constants collapse to one. CHEMICAL is kept as the name; the
 * old per-kind fields are gone because there is nothing left to distinguish.
 */
public class MekCapabilities {
    public static final BlockCapability<IChemicalHandler, Direction> CHEMICAL = Capabilities.CHEMICAL.block();
}

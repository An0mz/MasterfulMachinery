package io.ticticboom.mods.mm.cap;

import hellfirepvp.astralsorcery.common.lumen.ILumenHandler;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;

public class AstralCapabilities {
    public static final BlockCapability<ILumenHandler, Direction> LUMEN_HANDLER = ILumenHandler.BLOCK;
}

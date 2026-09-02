package io.ticticboom.mods.mm.cap;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import vazkii.botania.api.block.WandHUD;
import vazkii.botania.api.mana.ManaReceiver;
import vazkii.botania.api.neoforge.BotaniaNeoForgeCapabilities;

/**
 * Botania no longer exposes NeoForge capabilities directly. It keeps its own cross-loader lookup
 * objects and hands back the matching BlockCapability for whichever loader is running, so these
 * are resolved through that bridge rather than declared.
 * <p>
 * Kept in its own class so the Botania types are only loaded when the mod is installed;
 * MMCapabilities touches it from behind a ModList check.
 */
public class BotaniaCapabilities {
    public static final BlockCapability<ManaReceiver, Direction> MANA_RECEIVER =
            BotaniaNeoForgeCapabilities.getBlockApiLookupById(ManaReceiver.LOOKUP);

    public static final BlockCapability<WandHUD, Void> WAND_HUD =
            BotaniaNeoForgeCapabilities.getBlockApiLookupById(WandHUD.BLOCK_LOOKUP);
}

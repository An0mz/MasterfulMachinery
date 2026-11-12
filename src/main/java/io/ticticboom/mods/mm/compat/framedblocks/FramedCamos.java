package io.ticticboom.mods.mm.compat.framedblocks;

import io.ticticboom.mods.mm.builder.FramedCamo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

public final class FramedCamos {
    private static Boolean loaded;

    private FramedCamos() {
    }

    public static boolean loaded() {
        if (loaded == null) {
            loaded = ModList.get().isLoaded("framedblocks");
        }
        return loaded;
    }

    public static boolean bare(Level level, BlockPos pos, FramedCamo camo) {
        return loaded() && FramedBlocksCamoApplier.bare(level, pos, camo.secondary());
    }

    public static boolean apply(Level level, BlockPos pos, Player player, ItemStack stack, FramedCamo camo) {
        return loaded() && FramedBlocksCamoApplier.apply(level, pos, player, stack, camo.secondary());
    }
}

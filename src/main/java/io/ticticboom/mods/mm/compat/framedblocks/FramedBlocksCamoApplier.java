package io.ticticboom.mods.mm.compat.framedblocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import xfacthd.framedblocks.api.block.blockentity.FramedBlockEntity;
import xfacthd.framedblocks.api.block.blockentity.IFramedDoubleBlockEntity;
import xfacthd.framedblocks.api.camo.CamoContainer;
import xfacthd.framedblocks.api.camo.CamoContainerFactory;
import xfacthd.framedblocks.api.camo.CamoContainerHelper;

final class FramedBlocksCamoApplier {

    private FramedBlocksCamoApplier() {
    }

    static boolean bare(Level level, BlockPos pos, boolean secondary) {
        if (!(level.getBlockEntity(pos) instanceof FramedBlockEntity be)) {
            return false;
        }
        CamoContainer<?, ?> current;
        if (secondary) {
            current = be instanceof IFramedDoubleBlockEntity doubled ? doubled.getCamoTwo() : null;
        } else {
            current = be.getCamo();
        }
        return current != null && current.isEmpty();
    }

    static boolean apply(Level level, BlockPos pos, Player player, ItemStack stack, boolean secondary) {
        if (!(level.getBlockEntity(pos) instanceof FramedBlockEntity be)) {
            return false;
        }
        CamoContainerFactory<?> factory = CamoContainerHelper.findCamoFactory(stack);
        if (factory == null) {
            return false;
        }
        CamoContainer<?, ?> camo = factory.applyCamo(level, pos, player, stack);
        if (camo == null) {
            return false;
        }
        be.setCamo(camo, secondary);
        return true;
    }
}

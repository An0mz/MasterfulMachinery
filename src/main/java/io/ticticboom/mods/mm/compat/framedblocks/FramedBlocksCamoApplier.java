package io.ticticboom.mods.mm.compat.framedblocks;

import io.ticticboom.mods.mm.builder.FramedCamo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import xfacthd.framedblocks.api.camo.block.SimpleBlockCamoContainer;
import xfacthd.framedblocks.api.camo.block.SimpleBlockCamoContainerFactory;

import java.util.List;
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

    static void preview(BlockEntity be, List<FramedCamo> camos) {
        if (!(be instanceof FramedBlockEntity framed)) {
            return;
        }
        CompoundTag tag = new CompoundTag();
        for (FramedCamo camo : camos) {
            if (!(CamoContainerHelper.findCamoFactory(new ItemStack(camo.block())) instanceof SimpleBlockCamoContainerFactory factory)) {
                continue;
            }
            var container = new SimpleBlockCamoContainer(camo.block().defaultBlockState(), factory);
            String key = camo.secondary() ? IFramedDoubleBlockEntity.CAMO_TWO_NBT_KEY : FramedBlockEntity.CAMO_NBT_KEY;
            tag.put(key, CamoContainerHelper.writeToDisk(container));
        }
        if (!tag.isEmpty()) {
            framed.loadAdditional(tag, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        }
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

package io.ticticboom.mods.mm.compat.ae2.linker;

import appeng.api.features.IGridLinkableHandler;
import io.ticticboom.mods.mm.networklink.LinkData;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.item.ItemStack;

public class LinkerGridLinkable implements IGridLinkableHandler {

    @Override
    public boolean canLink(ItemStack stack) {
        return stack.getItem() instanceof LinkerItem;
    }

    @Override
    public void link(ItemStack stack, GlobalPos pos) {
        LinkerItem.setNetwork(stack, new LinkData.NetworkPos(pos.dimension(), pos.pos(), null));
    }

    @Override
    public void unlink(ItemStack stack) {
        LinkerItem.setNetwork(stack, null);
    }
}

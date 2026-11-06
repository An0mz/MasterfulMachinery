package io.ticticboom.mods.mm.port.thaumaturge.essentia.register;

import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortItem;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class ThaumaturgeEssentiaPortBlockItem extends BlockItem implements IPortItem {

    private final PortModel model;

    public ThaumaturgeEssentiaPortBlockItem(PortModel model, RegistryGroupHolder groupHolder) {
        super(groupHolder.getBlock().get(), new Properties());
        this.model = model;
    }

    @Override
    public Component getTypeName() {
        return Component.translatable("port.mm.thaumaturge_essentia.name").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD);
    }

    @Override
    public PortModel getModel() {
        return model;
    }

    @Override
    public Component getName(ItemStack stack) {
        var name = getModel().displayName();
        return name != null ? name : super.getName(stack);
    }
}

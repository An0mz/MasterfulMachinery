package io.ticticboom.mods.mm.port.etheria.ether.register;

import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortItem;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class EtheriaEtherPortBlockItem extends BlockItem implements IPortItem {

    private final PortModel model;

    public EtheriaEtherPortBlockItem(PortModel model, RegistryGroupHolder groupHolder) {
        super(groupHolder.getBlock().get(), new Properties());
        this.model = model;
    }

    @Override
    public Component getTypeName() {
        return Component.translatable("port.mm.etheria_ether.name").withStyle(ChatFormatting.BOLD, ChatFormatting.LIGHT_PURPLE);
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

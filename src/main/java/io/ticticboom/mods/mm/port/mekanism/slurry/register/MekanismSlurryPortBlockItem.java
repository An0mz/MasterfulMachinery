package io.ticticboom.mods.mm.port.mekanism.slurry.register;

import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.mekanism.chemical.register.MekanismChemicalPortBlockItem;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class MekanismSlurryPortBlockItem extends MekanismChemicalPortBlockItem {
    public MekanismSlurryPortBlockItem(PortModel model, RegistryGroupHolder groupHolder) {
        super(model, groupHolder);
    }

    @Override
    public Component getTypeName() {
        return Component.translatable("port.mm.mekanism_slurry.name").withStyle(ChatFormatting.BOLD, ChatFormatting.GREEN);
    }
}

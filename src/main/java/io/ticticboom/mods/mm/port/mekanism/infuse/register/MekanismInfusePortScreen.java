package io.ticticboom.mods.mm.port.mekanism.infuse.register;

import io.ticticboom.mods.mm.port.mekanism.chemical.register.MekanismChemicalPortMenu;
import io.ticticboom.mods.mm.port.mekanism.chemical.register.MekanismChemicalPortScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MekanismInfusePortScreen extends MekanismChemicalPortScreen<MekanismInfusePortMenu> {

    public MekanismInfusePortScreen(MekanismInfusePortMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }
}

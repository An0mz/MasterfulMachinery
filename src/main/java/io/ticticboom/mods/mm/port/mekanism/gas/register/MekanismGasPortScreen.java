package io.ticticboom.mods.mm.port.mekanism.gas.register;

import io.ticticboom.mods.mm.port.mekanism.chemical.register.MekanismChemicalPortScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MekanismGasPortScreen extends MekanismChemicalPortScreen<MekanismGasPortMenu> {

    public MekanismGasPortScreen(MekanismGasPortMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }
}

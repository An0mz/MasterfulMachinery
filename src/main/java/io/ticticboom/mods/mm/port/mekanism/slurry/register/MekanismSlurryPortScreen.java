package io.ticticboom.mods.mm.port.mekanism.slurry.register;

import io.ticticboom.mods.mm.port.mekanism.chemical.register.MekanismChemicalPortScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MekanismSlurryPortScreen extends MekanismChemicalPortScreen<MekanismSlurryPortMenu> {

    public MekanismSlurryPortScreen(MekanismSlurryPortMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }
}

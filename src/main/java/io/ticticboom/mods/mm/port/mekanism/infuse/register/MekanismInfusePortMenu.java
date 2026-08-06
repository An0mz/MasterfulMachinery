package io.ticticboom.mods.mm.port.mekanism.infuse.register;

import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.mekanism.chemical.register.MekanismChemicalPortBlockEntity;
import io.ticticboom.mods.mm.port.mekanism.chemical.register.MekanismChemicalPortMenu;
import io.ticticboom.mods.mm.port.mekanism.chemical.register.MekanismChemicalPortScreen;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class MekanismInfusePortMenu extends MekanismChemicalPortMenu {

    public MekanismInfusePortMenu(PortModel model, RegistryGroupHolder groupHolder, int windowId, Inventory inv, MekanismChemicalPortBlockEntity be) {
        super(model, groupHolder, windowId, be, inv);
    }

    public MekanismInfusePortMenu(PortModel model, RegistryGroupHolder groupHolder, int i, Inventory o, FriendlyByteBuf u) {
        this(model, groupHolder, i, o, (MekanismInfusePortBlockEntity) o.player.level().getBlockEntity(u.readBlockPos()));
    }
}

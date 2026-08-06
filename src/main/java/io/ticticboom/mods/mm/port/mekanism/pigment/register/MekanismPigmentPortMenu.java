package io.ticticboom.mods.mm.port.mekanism.pigment.register;

import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.mekanism.chemical.register.MekanismChemicalPortBlockEntity;
import io.ticticboom.mods.mm.port.mekanism.chemical.register.MekanismChemicalPortMenu;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class MekanismPigmentPortMenu extends MekanismChemicalPortMenu {
    public MekanismPigmentPortMenu(PortModel model, RegistryGroupHolder groupHolder, int windowId, Inventory inv, MekanismChemicalPortBlockEntity be) {
        super(model, groupHolder, windowId, be, inv);
    }

    public MekanismPigmentPortMenu(PortModel model, RegistryGroupHolder groupHolder, int i, Inventory o, FriendlyByteBuf u) {
        this(model, groupHolder, i, o, (MekanismPigmentPortBlockEntity) o.player.level().getBlockEntity(u.readBlockPos()));
    }
}

package io.ticticboom.mods.mm.port.astral.lumen.register;

import io.ticticboom.mods.mm.menu.MMContainerMenu;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortMenu;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import io.ticticboom.mods.mm.util.MenuUtils;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class AstralLumenPortMenu extends MMContainerMenu implements IPortMenu {
    @Getter
    private final PortModel model;
    private final AstralLumenPortBlockEntity be;

    public AstralLumenPortMenu(PortModel model, RegistryGroupHolder groupHolder, int windowId, Inventory inv, AstralLumenPortBlockEntity be) {
        super(groupHolder.getMenu().get(), groupHolder.getBlock().get(), windowId, MenuUtils.createAccessFromBlockEntity(be), 0);
        this.model = model;
        this.be = be;
        be.getStorage().setupContainer(this, inv, model);
    }

    public AstralLumenPortMenu(PortModel model, RegistryGroupHolder groupHolder, int windowId, Inventory inv, FriendlyByteBuf buf) {
        this(model, groupHolder, windowId, inv, (AstralLumenPortBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    @Override
    public <T> T getBlockEntity() {
        return (T) be;
    }
}

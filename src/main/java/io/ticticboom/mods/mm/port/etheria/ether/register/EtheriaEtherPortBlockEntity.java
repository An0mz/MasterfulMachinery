package io.ticticboom.mods.mm.port.etheria.ether.register;

import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.etheria.ether.EtheriaEtherPortStorage;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EtheriaEtherPortBlockEntity extends AbstractPortBlockEntity {

    private final PortModel model;
    private final RegistryGroupHolder groupHolder;
    private final EtheriaEtherPortStorage storage;

    public EtheriaEtherPortBlockEntity(PortModel model, RegistryGroupHolder groupHolder, BlockPos pos, BlockState state) {
        super(groupHolder.getBe().get(), pos, state);
        this.model = model;
        this.groupHolder = groupHolder;
        this.storage = (EtheriaEtherPortStorage) model.config().createPortStorage(this::setChanged);
        this.storage.setInput(model.input());
    }

    @Override
    public IPortStorage getStorage() {
        return storage;
    }

    public EtheriaEtherPortStorage getEtherStorage() {
        return storage;
    }

    @Override
    public boolean isInput() {
        return model.input();
    }

    @Override
    public PortModel getModel() {
        return model;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("port.mm.etheria_ether.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory inv, @NotNull Player player) {
        return new EtheriaEtherPortMenu(model, groupHolder, windowId, inv, this);
    }

    @Override
    public void tick() {
        if (level == null || lastTick == level.getGameTime()) {
            return;
        }
        super.tick();
        storage.tickVessel();
    }
}

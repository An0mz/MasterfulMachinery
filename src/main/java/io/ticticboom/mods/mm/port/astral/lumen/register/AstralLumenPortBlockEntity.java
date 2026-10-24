package io.ticticboom.mods.mm.port.astral.lumen.register;

import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.astral.lumen.AstralLumenPortStorage;
import io.ticticboom.mods.mm.port.astral.lumen.feature.AstralLumenNetworkFeature;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AstralLumenPortBlockEntity extends AbstractPortBlockEntity {

    private final PortModel model;
    private final RegistryGroupHolder groupHolder;
    private final AstralLumenPortStorage storage;
    private final AstralLumenNetworkFeature feature;

    public AstralLumenPortBlockEntity(PortModel model, RegistryGroupHolder groupHolder, BlockPos pos, BlockState state) {
        super(groupHolder.getBe().get(), pos, state);
        this.model = model;
        this.groupHolder = groupHolder;
        this.storage = (AstralLumenPortStorage) model.config().createPortStorage(this::setChanged);
        this.storage.setInput(model.input());
        this.feature = new AstralLumenNetworkFeature(storage, model.input());
    }

    @Override
    public IPortStorage getStorage() {
        return storage;
    }

    public AstralLumenPortStorage getLumenStorage() {
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
        return Component.translatable("port.mm.astral_lumen.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory inv, @NotNull Player player) {
        return new AstralLumenPortMenu(model, groupHolder, windowId, inv, this);
    }

    @Override
    public void tick() {
        if (level == null || lastTick == level.getGameTime()) {
            return;
        }
        super.tick();
        if (level instanceof ServerLevel serverLevel) {
            feature.tick(serverLevel, getBlockPos());
        }
    }
}

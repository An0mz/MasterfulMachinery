package io.ticticboom.mods.mm.port.astral.starlight.register;

import hellfirepvp.astralsorcery.common.lib.RegistriesAS;
import hellfirepvp.astralsorcery.common.starlight.transmission.StarlightTransmissionPacket;
import hellfirepvp.astralsorcery.common.tile.network.ForwardingStarlightReceiverNode;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.astral.starlight.AstralStarlightPortStorage;
import io.ticticboom.mods.mm.port.astral.starlight.feature.AstralStarlightReceiverFeature;
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

public class AstralStarlightPortBlockEntity extends AbstractPortBlockEntity implements ForwardingStarlightReceiverNode.ReceiverTile {

    private final PortModel model;
    private final RegistryGroupHolder groupHolder;
    private final AstralStarlightPortStorage storage;
    private final AstralStarlightReceiverFeature receiver = new AstralStarlightReceiverFeature();

    public AstralStarlightPortBlockEntity(PortModel model, RegistryGroupHolder groupHolder, BlockPos pos, BlockState state) {
        super(groupHolder.getBe().get(), pos, state);
        this.model = model;
        this.groupHolder = groupHolder;
        this.storage = (AstralStarlightPortStorage) model.config().createPortStorage(this::setChanged);
    }

    @Override
    public IPortStorage getStorage() {
        return storage;
    }

    public AstralStarlightPortStorage getStarlightStorage() {
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
        return Component.translatable("port.mm.astral_starlight.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory inv, @NotNull Player player) {
        return new AstralStarlightPortMenu(model, groupHolder, windowId, inv, this);
    }

    @Override
    public void tick() {
        if (level == null || lastTick == level.getGameTime()) {
            return;
        }
        super.tick();
        if (model.input() && level instanceof ServerLevel serverLevel) {
            receiver.tick(serverLevel, getBlockPos());
        }
    }

    @Override
    public void receiveStarlight(ServerLevel sLevel, StarlightTransmissionPacket packet) {
        if (!model.input() || packet.amount() <= 0) {
            return;
        }
        var key = RegistriesAS.REGISTRY_CONSTELLATIONS.getKey(packet.constellation());
        if (key != null) {
            storage.receive(key, packet.amount(), false);
        }
    }
}

package io.ticticboom.mods.mm.port.thaumaturge.essentia.register;

import com.leclowndu93150.thaumaturge.api.essentia.EssentiaAccess;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaTransfer;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.thaumaturge.essentia.ThaumaturgeEssentiaPortStorage;
import io.ticticboom.mods.mm.port.thaumaturge.essentia.ThaumaturgeEssentiaPortStorageModel;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ThaumaturgeEssentiaPortBlockEntity extends AbstractPortBlockEntity {
    private static final int PULL_INTERVAL = 5;

    private final PortModel model;
    private final RegistryGroupHolder groupHolder;
    private final ThaumaturgeEssentiaPortStorage storage;

    public ThaumaturgeEssentiaPortBlockEntity(PortModel model, RegistryGroupHolder groupHolder, BlockPos pos, BlockState state) {
        super(groupHolder.getBe().get(), pos, state);
        this.model = model;
        this.groupHolder = groupHolder;
        this.storage = (ThaumaturgeEssentiaPortStorage) model.config().createPortStorage(this::setChanged);
        this.storage.attach(this::getLevel, model.input());
    }

    @Override
    public IPortStorage getStorage() {
        return storage;
    }

    public ThaumaturgeEssentiaPortStorage getEssentiaStorage() {
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
        return Component.translatable("port.mm.thaumaturge_essentia.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory inv, @NotNull Player player) {
        return new ThaumaturgeEssentiaPortMenu(model, groupHolder, windowId, inv, this);
    }

    @Override
    public void tick() {
        if (level == null || lastTick == level.getGameTime()) {
            return;
        }
        super.tick();
        if (model.input() && level instanceof ServerLevel server
                && Math.floorMod(server.getGameTime() + getBlockPos().asLong(), PULL_INTERVAL) == 0) {
            pull(server);
        }
    }

    private void pull(ServerLevel server) {
        var own = storage.getConduit();
        int rate = Math.max(1, ((ThaumaturgeEssentiaPortStorageModel) storage.getStorageModel()).transferRate());
        for (Direction side : Direction.values()) {
            var wanted = own.pull(side);
            if (!wanted.isActive()) {
                return;
            }
            var sourcePos = getBlockPos().relative(side);
            if (!server.hasChunkAt(sourcePos)) {
                continue;
            }
            var face = side.getOpposite();
            var source = EssentiaAccess.conduit(server, sourcePos, face);
            if (source == null || !source.releasesOn(face) || source.heldAmount(face) <= 0) {
                continue;
            }
            if (wanted.strength() <= source.pull(face).strength() || wanted.strength() < source.pullThreshold()) {
                continue;
            }
            var aspect = wanted.aspect() != null ? wanted.aspect() : source.heldAspect(face);
            if (aspect == null || !wanted.accepts(aspect)) {
                continue;
            }
            EssentiaTransfer.transfer(source, face, own, side, aspect, rate, false);
        }
    }
}

package io.ticticboom.mods.mm.port.mekanism.chemical.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorage;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorageModel;
import io.ticticboom.mods.mm.port.mekanism.chemical.feature.MekanismChemicalPortPushFeature;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;

import java.util.Optional;
import io.ticticboom.mods.mm.util.BlockUtils;
import io.ticticboom.mods.mm.util.MenuUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MekanismChemicalPortBlockEntity extends AbstractPortBlockEntity {

    private final PortModel model;
    private final RegistryGroupHolder groupHolder;
    private final boolean isInput;
    private final MekanismChemicalPortStorage storage;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<MekanismChemicalPortPushFeature> push;

    public MekanismChemicalPortBlockEntity(PortModel model, RegistryGroupHolder groupHolder, boolean isInput, BlockPos pos, BlockState state) {
        super(groupHolder.getBe().get(), pos, state);
        this.model = model;
        this.groupHolder = groupHolder;
        this.isInput = isInput;
        this.storage = (MekanismChemicalPortStorage) model.config().createPortStorage(this::setChanged);
        var shouldPush = !isInput && ((MekanismChemicalPortStorageModel) storage.getStorageModel()).autoPush().get();
        this.push = shouldPush ? Optional.of(new MekanismChemicalPortPushFeature(this)) : Optional.empty();
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("port.mm.mekanism_chemical.title");
    }

    @Nullable
    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int windowId, net.minecraft.world.entity.player.Inventory inventory, net.minecraft.world.entity.player.Player player) {
        return new MekanismChemicalPortMenu(model, groupHolder, windowId, this, inventory);
    }

    public void tick() {
        assert level != null;
        if (lastTick == level.getGameTime()) {
            return;
        }
        lastTick = level.getGameTime();
        push.ifPresent(MekanismChemicalPortPushFeature::tick);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        push.ifPresent(MekanismChemicalPortPushFeature::onLoad);
    }

    public void neighborsChanged() {
        push.ifPresent(MekanismChemicalPortPushFeature::neighborsChanged);
    }

    @Override
    public PortModel getModel() {
        return model;
    }

    @Override
    public IPortStorage getStorage() {
        return storage;
    }

    @Override
    public boolean isInput() {
        return isInput;
    }
}

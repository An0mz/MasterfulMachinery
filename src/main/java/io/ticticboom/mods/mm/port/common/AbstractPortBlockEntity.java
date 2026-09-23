package io.ticticboom.mods.mm.port.common;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.IPortPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractPortBlockEntity extends BlockEntity implements IPortBlockEntity, IPortPart {

    static final int SYNC_INTERVAL = 10;

    protected long lastTick = 0;
    private long changeCount = 0;
    private long lastSync = Long.MIN_VALUE / 2;
    private boolean syncPending = false;

    public AbstractPortBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put(Ref.NBT_STORAGE_KEY, getStorage().save(new CompoundTag(), registries));
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        getStorage().load(tag.getCompound(Ref.NBT_STORAGE_KEY), registries);
        super.loadAdditional(tag, registries);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public long changeCount() {
        return changeCount;
    }

    @Override
    public void setChanged() {
        if (level == null || level.isClientSide()) {
            return;
        }
        super.setChanged();
        changeCount++;
        if (syncPending) {
            return;
        }
        if (level.getGameTime() - lastSync >= SYNC_INTERVAL) {
            sendSync();
        } else {
            syncPending = true;
            PortSyncQueue.add(this);
        }
    }

    boolean flushSync() {
        if (level == null || isRemoved()) {
            return true;
        }
        if (level.getGameTime() - lastSync < SYNC_INTERVAL) {
            return false;
        }
        syncPending = false;
        sendSync();
        return true;
    }

    private void sendSync() {
        lastSync = level.getGameTime();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
}

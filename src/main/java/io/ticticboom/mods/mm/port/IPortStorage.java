package io.ticticboom.mods.mm.port;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.util.BlockUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface IPortStorage {
    /**
     * NeoForge replaced LazyOptional with plain nullable returns, and Capability with
     * BlockCapability. Absent capability is now null rather than LazyOptional.empty().
     */
    <T> @Nullable T getCapability(BlockCapability<T, ?> capability);

    <T> boolean hasCapability(BlockCapability<T, ?> capability);

    CompoundTag save(CompoundTag tag);

    void load(CompoundTag tag);

    IPortStorageModel getStorageModel();

    UUID getStorageUid();

    JsonObject debugDump();

    default void setupContainer(AbstractContainerMenu container, Inventory inv, PortModel model) {
        BlockUtils.setupPlayerInventory(container, inv, 0, 0);
    }

    // Priority for outputs. Server authoritative. Range expected to be clamped to [0,10].
    // Implementations should persist this value in their save/load methods under the key "Priority" if applicable.
    default int getPriority() {
        return 0;
    }

    default void setPriority(int priority) {
        // default no-op for storage implementations that don't track priority
    }
}

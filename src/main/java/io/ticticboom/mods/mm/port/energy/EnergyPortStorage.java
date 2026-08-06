package io.ticticboom.mods.mm.port.energy;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.cap.MMCapabilities;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.UUID;

public class EnergyPortStorage implements IPortStorage {


    private final EnergyPortStorageModel model;
    @Getter
    private final EnergyPortHandler handler;
    private final UUID uid = UUID.randomUUID();

    // Priority for outputs. Default 0. Range 0..10.
    private int priority = 0;

    public EnergyPortStorage(EnergyPortStorageModel model, INotifyChangeFunction changed) {
        this.model = model;
        handler = new EnergyPortHandler(model.capacity(), model.maxReceive(), model.maxExtract(), changed);
    }

    @Override
    public <T> @Nullable T getCapability(BlockCapability<T, ?> capability) {
        @SuppressWarnings("unchecked") var cast = (T) handler;
        return hasCapability(capability) ? cast : null;
    }

    @Override
    public <T> boolean hasCapability(BlockCapability<T, ?> capability) {
        return MMCapabilities.ENERGY == capability;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("handler", handler.serializeNBT());
        tag.putInt("Priority", this.priority);
        return tag;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        handler.deserializeNBT(tag.get("handler"));
        if (tag.contains("Priority")) {
            this.priority = Math.max(0, Math.min(10, tag.getInt("Priority")));
        } else {
            this.priority = 0;
        }
    }

    @Override
    public IPortStorageModel getStorageModel() {
        return model;
    }

    @Override
    public UUID getStorageUid() {
        return uid;
    }

    @Override
    public JsonObject debugDump() {
        JsonObject dump = new JsonObject();
        dump.addProperty("uid", uid.toString());
        dump.addProperty("stored", handler.getEnergyStored());
        dump.addProperty("maxReceive", model.maxReceive());
        dump.addProperty("maxExtract", model.maxExtract());
        dump.addProperty("capacity", model.capacity());
        dump.addProperty("priority", this.priority);
        return dump;
    }

    public int internalExtract(int amount, boolean simulate) {
        return handler.unboundedExtractEnergy(amount, simulate);
    }

    public int internalInsert(int amount, boolean simulate) {
        return handler.unboundedReceiveEnergy(amount, simulate);
    }

    public int getStoredEnergy()  {
        return handler.getEnergyStored();
    }

    // Priority accessors
    public int getPriority() { return this.priority; }
    public void setPriority(int priority) { this.priority = Math.max(0, Math.min(10, priority)); }
}

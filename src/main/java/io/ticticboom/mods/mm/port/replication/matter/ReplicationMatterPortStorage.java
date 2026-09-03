package io.ticticboom.mods.mm.port.replication.matter;

import com.buuz135.replication.api.IMatterType;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.cap.ReplicationCapabilities;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ReplicationMatterPortStorage implements IPortStorage {

    private final ReplicationMatterPortStorageModel model;
    @Getter
    private final ReplicationMatterPortHandler handler;
    private final UUID uid = UUID.randomUUID();

    private int priority;

    public ReplicationMatterPortStorage(ReplicationMatterPortStorageModel model, INotifyChangeFunction changed) {
        this.model = model;
        this.handler = new ReplicationMatterPortHandler(model, changed);
        this.priority = model.priority();
    }

    @Override
    public <T> @Nullable T getCapability(BlockCapability<T, ?> capability) {
        @SuppressWarnings("unchecked") var cast = (T) handler;
        return hasCapability(capability) ? cast : null;
    }

    @Override
    public <T> boolean hasCapability(BlockCapability<T, ?> capability) {
        return ReplicationCapabilities.MATTER_HANDLER == capability;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var tanks = new ListTag();
        for (var tank : handler.tanks()) {
            tanks.add(tank.writeToNBT(new CompoundTag()));
        }
        tag.put("Tanks", tanks);
        tag.putInt("Priority", priority);
        return tag;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        var tanks = tag.getList("Tanks", Tag.TAG_COMPOUND);
        var handlerTanks = handler.tanks();
        for (int i = 0; i < handlerTanks.size(); i++) {
            handlerTanks.get(i).readFromNBT(i < tanks.size() ? tanks.getCompound(i) : new CompoundTag());
        }
        priority = tag.contains("Priority")
                ? ReplicationMatterPortStorageModel.clampPriority(tag.getInt("Priority"))
                : model.priority();
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
        var dump = new JsonObject();
        dump.addProperty("uid", uid.toString());
        dump.addProperty("capacity", model.capacity());
        dump.addProperty("priority", priority);
        var tanks = new JsonArray();
        for (var tank : handler.tanks()) {
            var entry = new JsonObject();
            var id = MatterTypes.idOf(tank.getMatter().getMatterType());
            entry.addProperty("matter", id == null ? "empty" : id.toString());
            entry.addProperty("amount", tank.getMatterAmount());
            entry.addProperty("filter", tank.getFilter() == null ? null : tank.getFilter().toString());
            tanks.add(entry);
        }
        dump.add("tanks", tanks);
        return dump;
    }

    public int internalExtract(IMatterType type, int amount, boolean simulate) {
        var action = simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE;
        return (int) Math.floor(handler.drain(type, amount, action).getAmount());
    }

    public int internalInsert(IMatterType type, int amount, boolean simulate) {
        var action = simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE;
        return (int) Math.floor(handler.fill(type, amount, action));
    }

    public int getStoredMatter(IMatterType type) {
        int stored = 0;
        for (var tank : handler.tanks()) {
            if (!tank.getMatter().isEmpty() && tank.getMatter().getMatterType() == type) {
                stored += (int) Math.floor(tank.getMatterAmount());
            }
        }
        return stored;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = ReplicationMatterPortStorageModel.clampPriority(priority);
    }
}

package io.ticticboom.mods.mm.port.astral.starlight;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.compat.astralsorcery.AstralSorceryNames;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.PortContent;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AstralStarlightPortStorage implements IPortStorage {

    public static final int FILL_COLOR = 0xFF5A82E6;

    private final AstralStarlightPortStorageModel model;
    private final INotifyChangeFunction changed;
    private final UUID uid = UUID.randomUUID();
    private final Map<ResourceLocation, Double> stored = new LinkedHashMap<>();

    @Getter
    private final int capacity;

    public AstralStarlightPortStorage(AstralStarlightPortStorageModel model, INotifyChangeFunction changed) {
        this.model = model;
        this.changed = changed;
        this.capacity = model.capacity();
    }

    public double getTotal() {
        double total = 0;
        for (double amount : stored.values()) {
            total += amount;
        }
        return total;
    }

    public double getStored(@Nullable ResourceLocation constellation) {
        if (constellation == null) {
            return getTotal();
        }
        return stored.getOrDefault(constellation, 0d);
    }

    public Map<ResourceLocation, Double> getStoredByConstellation() {
        return stored;
    }

    public double receive(ResourceLocation constellation, double amount, boolean simulate) {
        var accepted = Math.min(capacity - getTotal(), Math.abs(amount));
        if (accepted <= 0) {
            return 0;
        }
        if (!simulate) {
            stored.merge(constellation, accepted, Double::sum);
            changed.call();
        }
        return accepted;
    }

    public double extract(@Nullable ResourceLocation constellation, double amount, boolean simulate) {
        if (constellation != null) {
            return extractFrom(constellation, amount, simulate);
        }
        var order = new ArrayList<>(stored.keySet());
        order.sort(Comparator.comparingDouble((ResourceLocation key) -> stored.get(key)).reversed());
        double drained = 0;
        for (ResourceLocation key : order) {
            if (drained >= amount) {
                break;
            }
            drained += extractFrom(key, amount - drained, simulate);
        }
        return drained;
    }

    private double extractFrom(ResourceLocation constellation, double amount, boolean simulate) {
        double current = stored.getOrDefault(constellation, 0d);
        double drained = Math.min(current, Math.abs(amount));
        if (!simulate && drained > 0) {
            if (current - drained <= 1e-6) {
                stored.remove(constellation);
            } else {
                stored.put(constellation, current - drained);
            }
            changed.call();
        }
        return drained;
    }

    @Override
    public <T> @Nullable T getCapability(BlockCapability<T, ?> capability) {
        return null;
    }

    @Override
    public <T> boolean hasCapability(BlockCapability<T, ?> capability) {
        return false;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var list = new ListTag();
        stored.forEach((constellation, amount) -> {
            var entry = new CompoundTag();
            entry.putString("constellation", constellation.toString());
            entry.putDouble("amount", amount);
            list.add(entry);
        });
        tag.put("starlight", list);
        return tag;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        stored.clear();
        var list = tag.getList("starlight", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            var entry = list.getCompound(i);
            var constellation = ResourceLocation.tryParse(entry.getString("constellation"));
            if (constellation != null && entry.getDouble("amount") > 0) {
                stored.put(constellation, entry.getDouble("amount"));
            }
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
    public List<PortContent> contents() {
        return List.of(PortContent.gauge(Component.translatable("port.mm.astral_starlight.name"), FILL_COLOR, (long) getTotal(), capacity, "Starlight"));
    }

    @Override
    public List<Component> describeContents() {
        var lines = new ArrayList<Component>();
        lines.add(Component.translatable("gui.mm.port.astral_starlight.storage", (long) getTotal(), capacity));
        stored.forEach((constellation, amount) -> lines.add(Component.translatable("gui.mm.port.astral_starlight.constellation",
                AstralSorceryNames.constellation(constellation), (long) Math.floor(amount))));
        return lines;
    }

    @Override
    public double fillRatio() {
        return capacity <= 0 ? -1 : Math.min(1, getTotal() / capacity);
    }

    @Override
    public int fillColor() {
        return FILL_COLOR;
    }

    @Override
    public JsonObject debugDump() {
        var json = new JsonObject();
        json.addProperty("uid", uid.toString());
        json.addProperty("capacity", capacity);
        var byConstellation = new JsonObject();
        stored.forEach((constellation, amount) -> byConstellation.addProperty(constellation.toString(), amount));
        json.add("stored", byConstellation);
        return json;
    }
}

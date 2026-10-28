package io.ticticboom.mods.mm.port.etheria.ether;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.cap.MMCapabilities;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.PortContent;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import it.mralxart.etheria.items.base.IEtherEntryItem;
import it.mralxart.etheria.utils.EtherUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class EtheriaEtherPortStorage implements IPortStorage {

    public static final int FILL_COLOR = 0xFF9B6BE8;

    public static final int VESSEL_SLOT_X = 79;
    public static final int VESSEL_SLOT_Y = 28;

    private final EtheriaEtherPortStorageModel model;
    private final INotifyChangeFunction changed;
    private final UUID uid = UUID.randomUUID();

    @Getter
    private final long capacity;
    @Getter
    private long stored = 0;
    @Setter
    private boolean input = false;
    @Getter
    private final ItemStackHandler vessel;
    private final VesselAccess vesselAccess = new VesselAccess();

    public EtheriaEtherPortStorage(EtheriaEtherPortStorageModel model, INotifyChangeFunction changed) {
        this.model = model;
        this.changed = changed;
        this.capacity = Math.max(0, model.capacity());
        this.vessel = new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return isVessel(stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            protected void onContentsChanged(int slot) {
                changed.call();
            }
        };
    }

    public static boolean isVessel(ItemStack stack) {
        return stack.getItem() instanceof IEtherEntryItem;
    }

    private long vesselLimit(long amount) {
        return model.vesselRate() > 0 ? Math.min(amount, model.vesselRate()) : amount;
    }

    public void tickVessel() {
        var stack = vessel.getStackInSlot(0);
        if (stack.isEmpty() || !isVessel(stack)) {
            return;
        }
        int held = EtherUtils.getEther(stack);
        if (input) {
            long moved = receive(vesselLimit(held), false);
            if (moved > 0) {
                EtherUtils.setEther(stack, held - (int) moved);
                changed.call();
            }
        } else {
            long room = Math.max(0, EtherUtils.getMaxEther(stack) - held);
            long moved = extract(vesselLimit(room), false);
            if (moved > 0) {
                EtherUtils.setEther(stack, held + (int) moved);
                changed.call();
            }
        }
    }

    private boolean vesselDone(ItemStack stack) {
        int held = EtherUtils.getEther(stack);
        return input ? held <= 0 : held >= EtherUtils.getMaxEther(stack);
    }

    @Override
    public void setupContainer(AbstractContainerMenu container, Inventory inv, PortModel portModel) {
        container.addSlot(new SlotItemHandler(vessel, 0, VESSEL_SLOT_X + 1, VESSEL_SLOT_Y + 1));
        IPortStorage.super.setupContainer(container, inv, portModel);
    }

    public long receive(long ether, boolean simulate) {
        var accepted = Math.min(capacity - stored, Math.max(0, ether));
        if (!simulate && accepted > 0) {
            stored += accepted;
            changed.call();
        }
        return accepted;
    }

    public long extract(long ether, boolean simulate) {
        var drained = Math.min(stored, Math.max(0, ether));
        if (!simulate && drained > 0) {
            stored -= drained;
            changed.call();
        }
        return drained;
    }

    @Override
    public <T> @Nullable T getCapability(BlockCapability<T, ?> capability) {
        @SuppressWarnings("unchecked") var cast = (T) vesselAccess;
        return hasCapability(capability) ? cast : null;
    }

    @Override
    public <T> boolean hasCapability(BlockCapability<T, ?> capability) {
        return capability == MMCapabilities.ITEM;
    }

    private class VesselAccess implements IItemHandler {

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return vessel.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return vessel.insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            var stack = vessel.getStackInSlot(slot);
            if (stack.isEmpty() || !vesselDone(stack)) {
                return ItemStack.EMPTY;
            }
            return vessel.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return vessel.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return vessel.isItemValid(slot, stack);
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("stored", stored);
        tag.put("vessel", vessel.serializeNBT(registries));
        return tag;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        stored = Math.min(capacity, tag.getLong("stored"));
        if (tag.contains("vessel")) {
            vessel.deserializeNBT(registries, tag.getCompound("vessel"));
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
        return List.of(PortContent.gauge(Component.translatable("port.mm.etheria_ether.name"), 0x9B6BE8, stored, capacity, "Ether"));
    }

    @Override
    public List<Component> describeContents() {
        return List.of(Component.translatable("gui.mm.port.etheria_ether.storage", stored, capacity));
    }

    @Override
    public double fillRatio() {
        return capacity <= 0 ? -1 : Math.min(1, (double) stored / capacity);
    }

    @Override
    public int fillColor() {
        return FILL_COLOR;
    }

    @Override
    public JsonObject debugDump() {
        var json = new JsonObject();
        json.addProperty("uid", uid.toString());
        json.addProperty("stored", stored);
        json.addProperty("capacity", capacity);
        json.addProperty("vesselRate", model.vesselRate());
        return json;
    }
}

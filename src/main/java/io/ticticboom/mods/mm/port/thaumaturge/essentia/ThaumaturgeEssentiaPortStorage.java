package io.ticticboom.mods.mm.port.thaumaturge.essentia;

import com.google.gson.JsonObject;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.IAspectContainer;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaPull;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaConduit;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaStorage;
import com.leclowndu93150.thaumaturge.api.transaction.SnapshotJournal;
import com.leclowndu93150.thaumaturge.api.transaction.Transaction;
import com.leclowndu93150.thaumaturge.api.transaction.TransactionContext;
import io.ticticboom.mods.mm.cap.ThaumaturgeCapabilities;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.PortContent;
import io.ticticboom.mods.mm.port.common.ILockablePortStorage;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class ThaumaturgeEssentiaPortStorage implements IPortStorage, ILockablePortStorage {

    private final ThaumaturgeEssentiaPortStorageModel model;
    private final INotifyChangeFunction changed;
    private final UUID uid = UUID.randomUUID();
    private final Conduit conduit = new Conduit();
    private final View view = new View();
    private final Container container = new Container();
    private final Journal journal = new Journal();
    private Supplier<Level> level = () -> null;
    private boolean input = false;

    @Nullable
    private ResourceKey<IAspect> aspect;
    @Nullable
    private ResourceKey<IAspect> lastAspect;
    @Nullable
    private ResourceKey<IAspect> lockedAspect;
    private int amount = 0;
    private boolean locked = false;
    private int priority = 0;
    private long revision = 0;

    public ThaumaturgeEssentiaPortStorage(ThaumaturgeEssentiaPortStorageModel model, INotifyChangeFunction changed) {
        this.model = model;
        this.changed = changed;
    }

    public void attach(Supplier<Level> level, boolean input) {
        this.level = level;
        this.input = input;
    }

    @Nullable
    public ResourceKey<IAspect> getAspect() {
        return aspect;
    }

    public int getAmount() {
        return amount;
    }

    public int getCapacity() {
        return model.capacity();
    }

    @Nullable
    public ResourceKey<IAspect> getFilter() {
        return lockedAspect != null ? lockedAspect : aspect;
    }

    public IEssentiaConduit getConduit() {
        return conduit;
    }

    public int amountOf(ResourceKey<IAspect> wanted) {
        return wanted.equals(aspect) ? amount : 0;
    }

    public int spaceFor(ResourceKey<IAspect> incoming) {
        if (lockedAspect != null && !lockedAspect.equals(incoming)) {
            return 0;
        }
        if (amount > 0 && !incoming.equals(aspect)) {
            return 0;
        }
        return Math.max(0, model.capacity() - amount);
    }

    public int insert(ResourceKey<IAspect> incoming, int requested, boolean simulate) {
        int accepted = Math.min(requested, spaceFor(incoming));
        if (accepted <= 0) {
            return 0;
        }
        if (!simulate) {
            put(incoming, accepted);
            changed.call();
        }
        return accepted;
    }

    public int extract(ResourceKey<IAspect> wanted, int requested, boolean simulate) {
        int taken = Math.min(requested, amountOf(wanted));
        if (taken <= 0) {
            return 0;
        }
        if (!simulate) {
            take(taken);
            changed.call();
        }
        return taken;
    }

    private int insert(ResourceKey<IAspect> incoming, int requested, TransactionContext transaction) {
        int accepted = Math.min(requested, spaceFor(incoming));
        if (accepted <= 0) {
            return 0;
        }
        journal.updateSnapshots(transaction);
        put(incoming, accepted);
        return accepted;
    }

    private int extract(ResourceKey<IAspect> wanted, int requested, TransactionContext transaction) {
        int taken = Math.min(requested, amountOf(wanted));
        if (taken <= 0) {
            return 0;
        }
        journal.updateSnapshots(transaction);
        take(taken);
        return taken;
    }

    private void put(ResourceKey<IAspect> incoming, int accepted) {
        aspect = incoming;
        lastAspect = incoming;
        amount += accepted;
        revision++;
        if (locked && lockedAspect == null) {
            lockedAspect = incoming;
        }
    }

    private void take(int taken) {
        amount -= taken;
        if (amount <= 0) {
            amount = 0;
            aspect = null;
        }
        revision++;
    }

    private AspectList held() {
        if (aspect == null || amount <= 0) {
            return AspectList.EMPTY;
        }
        var holder = EssentiaAspects.holder(level.get(), aspect);
        return holder == null ? AspectList.EMPTY : AspectList.of(new AspectInstance(holder, amount));
    }

    @Override
    public boolean isLocked() {
        return locked;
    }

    @Override
    public void setLocked(boolean locked) {
        this.locked = locked;
        lockedAspect = locked ? aspect : null;
    }

    @Override
    public void dump() {
        amount = 0;
        aspect = null;
        revision++;
        changed.call();
    }

    @Override
    public <T> @Nullable T getCapability(BlockCapability<T, ?> capability) {
        Object result = null;
        if (capability == ThaumaturgeCapabilities.ESSENTIA_CONDUIT) {
            result = conduit;
        } else if (capability == ThaumaturgeCapabilities.ESSENTIA_STORAGE) {
            result = view;
        } else if (capability == ThaumaturgeCapabilities.ESSENTIA_CONTAINER) {
            result = container;
        }
        @SuppressWarnings("unchecked") var cast = (T) result;
        return cast;
    }

    @Override
    public <T> boolean hasCapability(BlockCapability<T, ?> capability) {
        return capability == ThaumaturgeCapabilities.ESSENTIA_CONDUIT
                || capability == ThaumaturgeCapabilities.ESSENTIA_STORAGE
                || capability == ThaumaturgeCapabilities.ESSENTIA_CONTAINER;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (aspect != null && amount > 0) {
            tag.putString("Aspect", aspect.location().toString());
            tag.putInt("Amount", amount);
        }
        tag.putInt("Priority", priority);
        if (locked) {
            tag.putBoolean("Locked", true);
            if (lockedAspect != null) {
                tag.putString("LockedAspect", lockedAspect.location().toString());
            }
        }
        return tag;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        aspect = readKey(tag, "Aspect");
        amount = aspect == null ? 0 : Math.max(0, tag.getInt("Amount"));
        if (aspect != null) {
            lastAspect = aspect;
        }
        priority = Math.max(0, Math.min(10, tag.getInt("Priority")));
        locked = tag.getBoolean("Locked");
        lockedAspect = locked ? readKey(tag, "LockedAspect") : null;
        revision++;
    }

    @Nullable
    private static ResourceKey<IAspect> readKey(CompoundTag tag, String name) {
        if (!tag.contains(name)) {
            return null;
        }
        var id = ResourceLocation.tryParse(tag.getString(name));
        return id == null ? null : EssentiaAspects.key(id);
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = Math.max(0, Math.min(10, priority));
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
        var name = aspect == null ? null : EssentiaAspects.name(level.get(), aspect);
        return List.of(PortContent.gauge(name, EssentiaAspects.color(level.get(), aspect), amount, model.capacity(), ""));
    }

    @Override
    public List<Component> describeContents() {
        if (aspect == null || amount <= 0) {
            return List.of(Component.translatable("jade.mm.port.empty"));
        }
        return List.of(Component.translatable("jade.mm.port.amount", EssentiaAspects.name(level.get(), aspect), amount, model.capacity()));
    }

    @Override
    public double fillRatio() {
        return model.capacity() <= 0 ? -1 : Math.min(1, (double) amount / model.capacity());
    }

    @Override
    public int fillColor() {
        var shown = aspect != null ? aspect : lockedAspect != null ? lockedAspect : lastAspect;
        return shown == null ? 0 : EssentiaAspects.color(level.get(), shown);
    }

    @Override
    public JsonObject debugDump() {
        var json = new JsonObject();
        json.addProperty("uid", uid.toString());
        json.addProperty("aspect", aspect == null ? "none" : aspect.location().toString());
        json.addProperty("amount", amount);
        json.addProperty("capacity", model.capacity());
        json.addProperty("suction", model.suction());
        json.addProperty("locked", locked);
        json.addProperty("priority", priority);
        return json;
    }

    private record Snapshot(
            @Nullable ResourceKey<IAspect> aspect,
            @Nullable ResourceKey<IAspect> lastAspect,
            @Nullable ResourceKey<IAspect> lockedAspect,
            int amount,
            long revision
    ) {
    }

    private final class Journal extends SnapshotJournal<Snapshot> {

        @Override
        protected Snapshot createSnapshot() {
            return new Snapshot(aspect, lastAspect, lockedAspect, amount, revision);
        }

        @Override
        protected void revertToSnapshot(Snapshot snapshot) {
            aspect = snapshot.aspect();
            lastAspect = snapshot.lastAspect();
            lockedAspect = snapshot.lockedAspect();
            amount = snapshot.amount();
            revision = snapshot.revision();
        }

        @Override
        protected void onRootCommit(Snapshot original) {
            changed.call();
        }
    }

    private final class Conduit implements IEssentiaConduit {

        @Override
        public int insert(Holder<IAspect> incoming, int requested, Direction side, boolean simulate) {
            var key = EssentiaAspects.key(incoming);
            return !input || key == null ? 0 : ThaumaturgeEssentiaPortStorage.this.insert(key, requested, simulate);
        }

        @Override
        public int extract(Holder<IAspect> wanted, int requested, Direction side, boolean simulate) {
            var key = EssentiaAspects.key(wanted);
            return input || key == null ? 0 : ThaumaturgeEssentiaPortStorage.this.extract(key, requested, simulate);
        }

        @Override
        public @Nullable Holder<IAspect> heldAspect(@Nullable Direction side) {
            return amount > 0 ? EssentiaAspects.holder(level.get(), aspect) : null;
        }

        @Override
        public int heldAmount(@Nullable Direction side) {
            return amount;
        }

        @Override
        public EssentiaPull pull(@Nullable Direction side) {
            if (!input || amount >= model.capacity()) {
                return EssentiaPull.NONE;
            }
            return EssentiaPull.of(EssentiaAspects.holder(level.get(), getFilter()), model.suction());
        }

        @Override
        public int pullThreshold() {
            return 0;
        }

        @Override
        public boolean releasesOn(Direction side) {
            return !input;
        }

        @Override
        public boolean receivesOn(Direction side) {
            return input;
        }

        @Override
        public boolean connectsOn(Direction side) {
            return true;
        }

        @Override
        public int roomFor(Holder<IAspect> incoming, Direction side) {
            var key = EssentiaAspects.key(incoming);
            return !input || key == null ? 0 : spaceFor(key);
        }
    }

    private final class View implements IEssentiaStorage {

        @Override
        public AspectList contents() {
            return held();
        }

        @Override
        public int insert(Holder<IAspect> incoming, int requested, boolean simulate) {
            try (Transaction transaction = Transaction.openRoot()) {
                int accepted = insert(incoming, requested, transaction);
                if (!simulate) {
                    transaction.commit();
                }
                return accepted;
            }
        }

        @Override
        public int insert(Holder<IAspect> incoming, int requested, TransactionContext transaction) {
            var key = EssentiaAspects.key(incoming);
            if (!input || key == null || requested <= 0) {
                return 0;
            }
            return ThaumaturgeEssentiaPortStorage.this.insert(key, requested, transaction);
        }

        @Override
        public int extract(Holder<IAspect> wanted, int requested, boolean simulate) {
            try (Transaction transaction = Transaction.openRoot()) {
                int taken = extract(wanted, requested, transaction);
                if (!simulate) {
                    transaction.commit();
                }
                return taken;
            }
        }

        @Override
        public int extract(Holder<IAspect> wanted, int requested, TransactionContext transaction) {
            var key = EssentiaAspects.key(wanted);
            if (input || key == null || requested <= 0) {
                return 0;
            }
            return ThaumaturgeEssentiaPortStorage.this.extract(key, requested, transaction);
        }

        @Override
        public long contentRevision() {
            return revision;
        }
    }

    private final class Container implements IAspectContainer {

        @Override
        public AspectList getAspects() {
            return held();
        }

        @Override
        public void setAspects(AspectList aspects) {
        }

        @Override
        public boolean accepts(Holder<IAspect> candidate) {
            return false;
        }

        @Override
        public int fill(Holder<IAspect> candidate, int requested) {
            return requested;
        }

        @Override
        public boolean drain(Holder<IAspect> candidate, int requested) {
            return false;
        }
    }
}

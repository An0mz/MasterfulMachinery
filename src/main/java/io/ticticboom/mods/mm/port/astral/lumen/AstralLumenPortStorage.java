package io.ticticboom.mods.mm.port.astral.lumen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import hellfirepvp.astralsorcery.common.lib.RegistriesAS;
import hellfirepvp.astralsorcery.common.lumen.ILumenHandler;
import hellfirepvp.astralsorcery.common.lumen.Lumen;
import hellfirepvp.astralsorcery.common.lumen.LumenLike;
import hellfirepvp.astralsorcery.common.lumen.LumenStack;
import io.ticticboom.mods.mm.compat.astralsorcery.AstralSorceryNames;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.port.IControllerAwareStorage;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.IRecipeDemandListener;
import io.ticticboom.mods.mm.port.PortContent;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import io.ticticboom.mods.mm.recipe.MachineRecipeManager;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.input.consume.ConsumeRecipeIngredientEntry;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AstralLumenPortStorage implements IPortStorage, IRecipeDemandListener, IControllerAwareStorage {

    public static final int FILL_COLOR = 0xFFB27CE8;
    private static final long DEMAND_TIMEOUT = 100;

    private final AstralLumenPortStorageModel model;
    private final INotifyChangeFunction changed;
    private final UUID uid = UUID.randomUUID();
    private final Map<ResourceLocation, Integer> stored = new LinkedHashMap<>();
    private final LumenHandler handler = new LumenHandler();

    @Getter
    private final int capacity;
    @Setter
    private boolean input = false;
    @Nullable
    private RecipeModel demandRecipe;
    private long demandTick = Long.MIN_VALUE / 2;
    private WeakReference<MachineControllerBlockEntity> controller = new WeakReference<>(null);
    private long controllerSeen = Long.MIN_VALUE / 2;

    public AstralLumenPortStorage(AstralLumenPortStorageModel model, INotifyChangeFunction changed) {
        this.model = model;
        this.changed = changed;
        this.capacity = model.capacity();
    }

    public int getTotal() {
        int total = 0;
        for (int amount : stored.values()) {
            total += amount;
        }
        return total;
    }

    public int getStored(ResourceLocation lumen) {
        return stored.getOrDefault(lumen, 0);
    }

    public Map<ResourceLocation, Integer> getStoredByLumen() {
        return stored;
    }

    public ILumenHandler getHandler() {
        return handler;
    }

    public boolean accepts(ResourceLocation lumen) {
        return model.filter().isEmpty() || model.filter().contains(lumen);
    }

    public int receive(ResourceLocation lumen, int amount, boolean simulate) {
        if (!accepts(lumen)) {
            return 0;
        }
        int accepted = Math.max(0, Math.min(capacity - getTotal(), Math.abs(amount)));
        if (!simulate && accepted > 0) {
            stored.merge(lumen, accepted, Integer::sum);
            changed.call();
        }
        return accepted;
    }

    public int extract(ResourceLocation lumen, int amount, boolean simulate) {
        int current = stored.getOrDefault(lumen, 0);
        int drained = Math.min(current, Math.abs(amount));
        if (!simulate && drained > 0) {
            if (current == drained) {
                stored.remove(lumen);
            } else {
                stored.put(lumen, current - drained);
            }
            changed.call();
        }
        return drained;
    }

    @Override
    public void setRecipeDemand(long gameTime, @Nullable RecipeModel recipe) {
        demandRecipe = recipe;
        demandTick = gameTime;
    }

    @Override
    public void attachController(MachineControllerBlockEntity controller, long gameTime) {
        if (this.controller.get() != controller) {
            this.controller = new WeakReference<>(controller);
        }
        controllerSeen = gameTime;
    }

    public List<ResourceLocation> wantedLumen(long gameTime) {
        var wanted = new ArrayList<ResourceLocation>();
        if (demandRecipe != null && gameTime - demandTick <= DEMAND_TIMEOUT) {
            collectLumen(demandRecipe, wanted);
            return wanted;
        }
        if (!model.filter().isEmpty()) {
            wanted.addAll(model.filter());
            return wanted;
        }
        var found = controller.get();
        if (found != null && !found.isRemoved() && gameTime - controllerSeen <= DEMAND_TIMEOUT && found.getStructure() != null) {
            for (var recipe : MachineRecipeManager.getRecipesByStrucutreId(found.getStructure().id())) {
                collectLumen(recipe, wanted);
            }
        }
        return wanted;
    }

    private void collectLumen(RecipeModel recipe, List<ResourceLocation> wanted) {
        for (var entry : recipe.inputs().inputs()) {
            if (entry instanceof ConsumeRecipeIngredientEntry consume
                    && consume.getIngredient() instanceof AstralLumenPortIngredient lumen
                    && accepts(lumen.getLumen()) && !wanted.contains(lumen.getLumen())) {
                wanted.add(lumen.getLumen());
            }
        }
    }

    @Override
    public <T> @Nullable T getCapability(BlockCapability<T, ?> capability) {
        @SuppressWarnings("unchecked") var cast = (T) handler;
        return hasCapability(capability) ? cast : null;
    }

    @Override
    public <T> boolean hasCapability(BlockCapability<T, ?> capability) {
        return capability == ILumenHandler.BLOCK;
    }

    private class LumenHandler implements ILumenHandler {

        @Override
        public List<LumenStack> getContainedLumen() {
            var stacks = new ArrayList<LumenStack>();
            stored.forEach((id, amount) -> RegistriesAS.REGISTRY_LUMEN.getOptional(id)
                    .ifPresent(lumen -> stacks.add(LumenStack.of(lumen, amount))));
            return stacks;
        }

        @Override
        public Optional<LumenStack> getContainedLumen(LumenLike type) {
            var lumen = type.asLumen();
            var id = RegistriesAS.REGISTRY_LUMEN.getKey(lumen);
            int amount = id == null ? 0 : stored.getOrDefault(id, 0);
            return amount > 0 ? Optional.of(LumenStack.of(lumen, amount)) : Optional.empty();
        }

        @Override
        public int getCapacity(LumenLike type) {
            return capacity;
        }

        @Override
        public int fill(LumenStack stack, Action action) {
            if (!input || stack.isEmpty()) {
                return 0;
            }
            var id = RegistriesAS.REGISTRY_LUMEN.getKey(stack.getLumen());
            return id == null ? 0 : receive(id, stack.getAmount(), action.isSimulate());
        }

        @Override
        public LumenStack drain(LumenLike type, int amount, Action action) {
            if (input || amount <= 0) {
                return LumenStack.EMPTY;
            }
            Lumen lumen = type.asLumen();
            var id = RegistriesAS.REGISTRY_LUMEN.getKey(lumen);
            if (id == null) {
                return LumenStack.EMPTY;
            }
            int drained = extract(id, amount, action.isSimulate());
            return drained > 0 ? LumenStack.of(lumen, drained) : LumenStack.EMPTY;
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var lumen = new CompoundTag();
        stored.forEach((id, amount) -> lumen.putInt(id.toString(), amount));
        tag.put("lumen", lumen);
        return tag;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        stored.clear();
        var lumen = tag.getCompound("lumen");
        for (String key : lumen.getAllKeys()) {
            var id = ResourceLocation.tryParse(key);
            if (id != null && lumen.getInt(key) > 0) {
                stored.put(id, lumen.getInt(key));
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
        return List.of(PortContent.gauge(Component.translatable("port.mm.astral_lumen.name"), FILL_COLOR, getTotal(), capacity, "Lumen"));
    }

    @Override
    public List<Component> describeContents() {
        var lines = new ArrayList<Component>();
        lines.add(Component.translatable("gui.mm.port.astral_lumen.storage", getTotal(), capacity));
        stored.forEach((id, amount) -> lines.add(Component.translatable("gui.mm.port.astral_lumen.entry",
                AstralSorceryNames.lumen(id), amount)));
        return lines;
    }

    @Override
    public double fillRatio() {
        return capacity <= 0 ? -1 : Math.min(1, (double) getTotal() / capacity);
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
        var filter = new JsonArray();
        model.filter().forEach(id -> filter.add(id.toString()));
        json.add("filter", filter);
        var byLumen = new JsonObject();
        stored.forEach((id, amount) -> byLumen.addProperty(id.toString(), amount));
        json.add("stored", byLumen);
        return json;
    }
}

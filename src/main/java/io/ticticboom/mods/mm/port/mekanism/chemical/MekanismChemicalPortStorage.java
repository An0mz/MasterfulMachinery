package io.ticticboom.mods.mm.port.mekanism.chemical;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import io.ticticboom.mods.mm.port.mekanism.NotifyChangeContentsListener;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Mekanism 1.21.1 merged the gas, slurry, pigment and infusion registries into a single Chemical
 * type with no discriminator, so this class is not generic over a chemical/stack pair and MM
 * exposes one mm:mekanism/chemical port rather than one per former kind.
 */
public class MekanismChemicalPortStorage implements IPortStorage {

    public IChemicalTank chemicalTank;
    private final MekanismChemicalPortStorageModel model;
    private final UUID uid = UUID.randomUUID();

    public MekanismChemicalPortStorage(MekanismChemicalPortStorageModel model, INotifyChangeFunction changed) {
        this.model = model;
        this.chemicalTank = createTank(model.amount(), changed);
    }

    /** ChemicalTankBuilder.GAS/SLURRY/... collapsed into the single BasicChemicalTank factory. */
    protected IChemicalTank createTank(long capacity, INotifyChangeFunction changed) {
        return BasicChemicalTank.createAllValid(capacity, new NotifyChangeContentsListener(changed));
    }

    protected JsonObject debugStack(ChemicalStack stack) {
        var json = new JsonObject();
        var key = MekanismAPI.CHEMICAL_REGISTRY.getKey(stack.getChemical());
        json.addProperty("chemical", key == null ? "null" : key.toString());
        json.addProperty("amount", stack.getAmount());
        return json;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T getCapability(BlockCapability<T, ?> capability) {
        // BasicChemicalTank implements IChemicalHandler as well as IChemicalTank, so the tank
        // itself satisfies Mekanism's chemical capability.
        return hasCapability(capability) ? (T) chemicalTank : null;
    }

    @Override
    public <T> boolean hasCapability(BlockCapability<T, ?> capability) {
        return capability == Capabilities.CHEMICAL.block();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("handler", chemicalTank.serializeNBT(registries));
        return tag;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        chemicalTank.deserializeNBT(registries, tag.getCompound("handler"));
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
        JsonObject json = new JsonObject();
        json.addProperty("uid", uid.toString());
        json.addProperty("amount", model.amount());
        json.add("stack", debugStack(chemicalTank.getStack()));
        return json;
    }

    public ChemicalStack extract(long amount, Action action) {
        return chemicalTank.extract(amount, action, AutomationType.INTERNAL);
    }

    public ChemicalStack insert(ChemicalStack stack, Action action) {
        var leftInStack = chemicalTank.insert(stack, action, AutomationType.INTERNAL);
        long remainingToInsert = stack.getAmount() - leftInStack.getAmount();
        stack.setAmount(remainingToInsert);
        return stack;
    }
}

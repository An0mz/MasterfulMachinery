package io.ticticboom.mods.mm.port.botania.mana.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.botania.mana.BotaniaManaPortStorage;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Botania has no 1.21.1 release, so this block entity is stubbed: the mana storage, menu wiring
 * and serialisation are intact, but every hook into Botania is removed and the port does nothing
 * useful at runtime. MMPortRegistry only registers the mana port when Botania is loaded, so on
 * 1.21.1 this class is currently unreachable.
 *
 * <p>To restore Botania support, re-enable the dependency in build.gradle and put back:
 * <ul>
 *   <li>{@code implements ManaPool} and the {@code @Override}s on the mana methods below, which
 *       are kept verbatim precisely so they can be re-annotated rather than rewritten.</li>
 *   <li>{@code getColor}/{@code setColor}, {@code getManaReceiverLevel}, {@code getManaReceiverPos}
 *       and {@code canReceiveManaFromBursts}.</li>
 *   <li>The {@code WandHud} inner class, which rendered the wand HUD.</li>
 *   <li>{@code tick()} and {@code setRemoved()}, which added and removed this pool from the mana
 *       network via {@code ManaNetworkHandler} and {@code BotaniaAPI}.</li>
 *   <li>Capability exposure for {@code WandHUD} and {@code ManaReceiver}. Note these must now be
 *       registered through {@code RegisterCapabilitiesEvent} rather than a getCapability override,
 *       and {@code io.ticticboom.mods.mm.cap.BotaniaCapabilities} was deleted because it was built
 *       entirely on Forge's removed Capability API.</li>
 * </ul>
 *
 * <p>Five of the imports this file used were {@code vazkii.botania.common.*} and
 * {@code client.*} internals rather than the {@code api.*} surface, so expect them to have moved
 * and check against the real jar rather than restoring the old names blindly.
 */
public class BotaniaManaPortBlockEntity extends BlockEntity implements IPortBlockEntity {

    private final PortModel model;
    private final RegistryGroupHolder groupHolder;
    private final BotaniaManaPortStorage storage;

    public BotaniaManaPortBlockEntity(PortModel model, RegistryGroupHolder groupHolder, BlockPos pos, BlockState state) {
        super(groupHolder.getBe().get(), pos, state);
        this.model = model;
        this.groupHolder = groupHolder;
        this.storage = (BotaniaManaPortStorage) model.config().createPortStorage(this::setChanged);
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
        return model.input();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("port.mm.botania_mana.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return null;
    }

    // --- ManaPool implementation, kept unannotated while Botania is unavailable ---

    public boolean isOutputtingPower() {
        return !this.model.input();
    }

    public int getMaxMana() {
        return storage.getCapacity();
    }

    public Optional<DyeColor> getColor() {
        return Optional.of(DyeColor.CYAN);
    }

    public void setColor(Optional<DyeColor> optional) {
    }

    public Level getManaReceiverLevel() {
        return level;
    }

    public BlockPos getManaReceiverPos() {
        return this.getBlockPos();
    }

    public int getCurrentMana() {
        return storage.getStored();
    }

    public boolean isFull() {
        return storage.getStored() >= storage.getCapacity();
    }

    public void receiveMana(int i) {
        if (i < 0) {
            storage.extractMana(i, false);
        } else {
            storage.receiveMana(i, false);
        }
    }

    public boolean canReceiveManaFromBursts() {
        return this.model.input();
    }

    // --- serialisation, unaffected by Botania ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(Ref.NBT_STORAGE_KEY, storage.save(new CompoundTag(), registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        storage.load(tag.getCompound(Ref.NBT_STORAGE_KEY), registries);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level == null || level.isClientSide()) {
            return;
        }
        level.sendBlockUpdated(getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

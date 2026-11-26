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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import velrondevs.botania.api.BotaniaAPI;
import velrondevs.botania.api.mana.ManaBlockType;
import velrondevs.botania.api.mana.ManaNetworkAction;
import velrondevs.botania.api.mana.ManaPool;
import velrondevs.botania.api.mana.spark.ManaSpark;
import velrondevs.botania.api.mana.spark.SparkAttachable;
import velrondevs.botania.common.handler.ManaNetworkHandler;

import java.util.Optional;
import java.util.function.Predicate;

public class BotaniaManaPortBlockEntity extends BlockEntity implements ManaPool, SparkAttachable, IPortBlockEntity {

    private static final Predicate<Entity> IS_SPARK = e -> e instanceof ManaSpark;

    private final PortModel model;
    private final RegistryGroupHolder groupHolder;
    private final BotaniaManaPortStorage storage;

    public BotaniaManaPortBlockEntity(PortModel model, RegistryGroupHolder groupHolder, BlockPos pos, BlockState state) {
        super(groupHolder.getBe().get(), pos, state);
        this.model = model;
        this.groupHolder = groupHolder;
        this.storage = (BotaniaManaPortStorage) model.config().createPortStorage(this::setChanged);
    }

    public void tick() {
        if (!ManaNetworkHandler.instance.isPoolIn(level, this) && !isRemoved()) {
            BotaniaAPI.instance().getManaNetworkInstance().fireManaNetworkEvent(this, ManaBlockType.POOL, ManaNetworkAction.ADD);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide()) {
            BotaniaAPI.instance().getManaNetworkInstance().fireManaNetworkEvent(this, ManaBlockType.POOL, ManaNetworkAction.REMOVE);
        }
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

    @Override
    public boolean isOutputtingPower() {
        return !this.model.input();
    }

    @Override
    public int getMaxMana() {
        return storage.getCapacity();
    }

    @Override
    public Optional<DyeColor> getColor() {
        return Optional.of(DyeColor.CYAN);
    }

    @Override
    public void setColor(Optional<DyeColor> optional) {
    }

    @Override
    public Level getManaReceiverLevel() {
        return level;
    }

    @Override
    public BlockPos getManaReceiverPos() {
        return this.getBlockPos();
    }

    @Override
    public int getCurrentMana() {
        return storage.getStored();
    }

    @Override
    public boolean isFull() {
        return storage.getStored() >= storage.getCapacity();
    }

    @Override
    public void receiveMana(int i) {
        if (i == 0) {
            return;
        }
        if (i < 0) {
            storage.extractMana(i, false);
        } else {
            storage.receiveMana(i, false);
        }
    }

    @Override
    public boolean canReceiveManaFromBursts() {
        return this.model.input();
    }

    @Override
    public boolean canAttachSpark(ItemStack stack) {
        return true;
    }

    @Override
    public ManaSpark getAttachedSpark() {
        if (level == null) {
            return null;
        }
        var above = worldPosition.above();
        var sparks = level.getEntitiesOfClass(Entity.class, new AABB(above), IS_SPARK);
        return sparks.size() == 1 ? (ManaSpark) sparks.get(0) : null;
    }

    @Override
    public int getAvailableSpaceForMana() {
        return model.input() ? Math.max(0, storage.getCapacity() - storage.getStored()) : 0;
    }

    @Override
    public boolean areIncomingTranfersDone() {
        return false;
    }

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

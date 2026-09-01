package io.ticticboom.mods.mm.port.botania.mana.register;

import com.mojang.blaze3d.platform.Window;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.botania.mana.BotaniaManaPortStorage;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import lombok.Getter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.api.BotaniaAPIClient;
import vazkii.botania.api.block.WandHUD;
import vazkii.botania.api.mana.ManaPool;
import vazkii.botania.client.core.helper.RenderHelper;
import vazkii.botania.client.gui.HUDHandler;
import vazkii.botania.common.item.BotaniaItems;
import vazkii.botania.common.item.ManaTabletItem;

public class BotaniaManaPortBlockEntity extends BlockEntity implements ManaPool, IPortBlockEntity {

    private final PortModel model;
    private final RegistryGroupHolder groupHolder;
    private final BotaniaManaPortStorage storage;
    @Getter
    private final WandHud wandHud = new WandHud(this);

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

    @Override
    public boolean isOutputtingPower() {
        return !this.model.input();
    }

    @Override
    public int getMaxMana() {
        return storage.getCapacity();
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

    public static class WandHud implements WandHUD {
        private final BotaniaManaPortBlockEntity pool;

        public WandHud(BotaniaManaPortBlockEntity pool) {
            this.pool = pool;
        }

        @Override
        public void renderHUD(GuiGraphics gui, Window window, Font font, float partialTicks) {
            ItemStack poolStack = new ItemStack(this.pool.getBlockState().getBlock());
            String name = poolStack.getHoverName().getString();
            int centerX = window.getGuiScaledWidth() / 2;
            int centerY = window.getGuiScaledHeight() / 2;
            int width = Math.max(102, font.width(name)) + 4;
            RenderHelper.renderHUDBox(gui, centerX - width / 2, centerY + 8, centerX + width / 2, centerY + 48);
            BotaniaAPIClient.instance().drawSimpleManaHUD(gui, window, font, 38399,
                    this.pool.getCurrentMana(), this.pool.getMaxMana(), name);
            int arrowU = this.pool.isOutputtingPower() ? 22 : 0;
            RenderHelper.drawTexturedModalRect(gui, HUDHandler.manaBar, centerX - 11, centerY + 30, arrowU, 38, 22, 15);
            ItemStack tablet = new ItemStack(BotaniaItems.MANA_TABLET);
            ManaTabletItem.setStackCreative(tablet);
            gui.renderItem(tablet, centerX - 31, centerY + 30);
            gui.renderItem(poolStack, centerX + 15, centerY + 30);
        }
    }
}

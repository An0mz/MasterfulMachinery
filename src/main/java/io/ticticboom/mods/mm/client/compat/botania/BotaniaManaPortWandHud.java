package io.ticticboom.mods.mm.client.compat.botania;

import com.mojang.blaze3d.systems.RenderSystem;
import io.ticticboom.mods.mm.port.MMPortRegistry;
import io.ticticboom.mods.mm.port.botania.mana.register.BotaniaManaPortBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import velrondevs.botania.api.BotaniaAPIClient;
import velrondevs.botania.api.BotaniaClientCapabilities;
import velrondevs.botania.api.block.WandHUD;
import velrondevs.botania.client.core.helper.RenderHelper;
import velrondevs.botania.client.gui.HUDHandler;
import velrondevs.botania.common.item.ManaTabletItem;
import velrondevs.botania.registry.BotaniaItems;

public class BotaniaManaPortWandHud implements WandHUD {
    private final BotaniaManaPortBlockEntity pool;

    public BotaniaManaPortWandHud(BotaniaManaPortBlockEntity pool) {
        this.pool = pool;
    }

    @SuppressWarnings("unchecked")
    public static void register(RegisterCapabilitiesEvent event) {
        for (var holder : MMPortRegistry.PORTS) {
            var beType = holder.getBe().get();
            if (beType == null) {
                continue;
            }
            event.registerBlockEntity(BotaniaClientCapabilities.WAND_HUD, (BlockEntityType<BlockEntity>) beType,
                    (be, ctx) -> be instanceof BotaniaManaPortBlockEntity pool ? new BotaniaManaPortWandHud(pool) : null);
        }
    }

    @Override
    public void renderHUD(GuiGraphics gui, Minecraft mc) {
        ItemStack poolStack = new ItemStack(pool.getBlockState().getBlock());
        String name = poolStack.getHoverName().getString();
        int centerX = mc.getWindow().getGuiScaledWidth() / 2;
        int centerY = mc.getWindow().getGuiScaledHeight() / 2;
        int width = Math.max(102, mc.font.width(name)) + 4;
        RenderHelper.renderHUDBox(gui, centerX - width / 2, centerY + 8, centerX + width / 2, centerY + 48);
        BotaniaAPIClient.instance().drawSimpleManaHUD(gui, 0x0095FF, pool.getCurrentMana(), pool.getMaxMana(), name);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        int arrowU = pool.isOutputtingPower() ? 22 : 0;
        RenderHelper.drawTexturedModalRect(gui, HUDHandler.manaBar, centerX - 11, centerY + 30, arrowU, 38, 22, 15);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        ItemStack tablet = new ItemStack(BotaniaItems.manaTablet);
        ManaTabletItem.setStackCreative(tablet);
        gui.renderItem(tablet, centerX - 31, centerY + 30);
        gui.renderItem(poolStack, centerX + 15, centerY + 30);
        RenderSystem.disableBlend();
    }
}

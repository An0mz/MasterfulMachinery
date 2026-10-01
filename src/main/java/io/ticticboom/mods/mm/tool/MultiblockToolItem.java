package io.ticticboom.mods.mm.tool;

import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.builder.AssemblyJobs;
import io.ticticboom.mods.mm.builder.DismantlePlanner;
import io.ticticboom.mods.mm.networklink.NetworkLink;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class MultiblockToolItem extends Item {
    private static final int BAR_COLOR = 0x3399FF;
    private static final int NOTICE_TICKS = 200;
    private static final Map<UUID, Integer> LAST_NOTICE = new HashMap<>();
    private static Supplier<Component> dismantleKeyName = () -> Component.literal("V");

    public MultiblockToolItem() {
        super(new Item.Properties().stacksTo(1));
    }

    public static void setDismantleKeyName(Supplier<Component> name) {
        dismantleKeyName = name;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide()) {
            openStore((ServerPlayer) player, stack, hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static void openStore(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        player.openMenu(new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                return stack.getHoverName();
            }

            @Override
            public @NotNull AbstractContainerMenu createMenu(int windowId, @NotNull Inventory inv, @NotNull Player p) {
                return new MultiblockToolMenu(windowId, inv, hand);
            }
        }, buf -> buf.writeEnum(hand));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown()
                || ToolBuilds.acceptingController(context.getLevel(), context.getClickedPos(), stack) == null) {
            return InteractionResult.PASS;
        }
        return build(context);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (!player.isShiftKeyDown()) {
            return build(context);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (NetworkLink.bindTool(serverPlayer, context)) {
                return InteractionResult.SUCCESS;
            }
            if (DismantlePlanner.resolve(context.getLevel(), context.getClickedPos()) != null
                    || DismantlePlanner.matchBuilder(context.getLevel(), context.getClickedPos(), context.getItemInHand()) != null) {
                ToolDismantles.shiftClick(serverPlayer, context.getItemInHand(), context.getClickedPos());
            } else {
                openStore(serverPlayer, context.getItemInHand(), context.getHand());
            }
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }

    private static InteractionResult build(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (ToolData.structure(stack) == null && ToolData.builderStructure(stack) == null) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.mm.tool.no_structure"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        Component busy = AssemblyJobs.busyMessage(serverPlayer);
        if (busy != null) {
            player.displayClientMessage(busy, true);
            return InteractionResult.FAIL;
        }
        ToolBuilds.Result result = ToolBuilds.prepare(level, player, stack, context.getClickedPos(), context.getClickedFace());
        if (result.notice() != null && noticeDue(serverPlayer)) {
            player.displayClientMessage(result.notice(), false);
        }
        if (result.prepared() == null) {
            player.displayClientMessage(result.error(), true);
            return InteractionResult.FAIL;
        }
        ToolBuilds.Prepared build = result.prepared();
        if (!AssemblyJobs.start(serverPlayer, build.controllerPos(), build.plan(), build.source(), build.perBlockFe(),
                build.requiresController(), ToolData.instantBuild(stack))) {
            player.displayClientMessage(Component.translatable("message.mm.assemble.busy"), true);
            return InteractionResult.FAIL;
        }
        return InteractionResult.CONSUME;
    }

    private static boolean noticeDue(ServerPlayer player) {
        int now = player.server.getTickCount();
        Integer last = LAST_NOTICE.get(player.getUUID());
        if (last != null && now - last >= 0 && now - last < NOTICE_TICKS) {
            return false;
        }
        LAST_NOTICE.put(player.getUUID(), now);
        return true;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int capacity = MMConfig.TOOL_ENERGY_CAPACITY;
        int energy = ToolEnergy.of(stack).getEnergyStored();
        return Math.round(13.0F * energy / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BAR_COLOR;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation structure = ToolData.structure(stack);
        if (structure == null) {
            structure = ToolData.builderStructure(stack);
        }
        if (structure != null) {
            tooltip.add(Component.translatable("tooltip.mm.structure_builder.structure", structure.toString()).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.mm.structure_builder.no_structure").withStyle(ChatFormatting.DARK_GRAY));
        }
        int capacity = MMConfig.TOOL_ENERGY_CAPACITY;
        int energy = ToolEnergy.of(stack).getEnergyStored();
        tooltip.add(Component.translatable("tooltip.mm.structure_builder.energy", energy, capacity).withStyle(ChatFormatting.GRAY));
        var network = NetworkLink.AVAILABLE ? ToolData.network(stack) : null;
        if (network != null) {
            tooltip.add(Component.translatable("tooltip.mm.structure_builder.network",
                    network.pos().toShortString(), network.dimension().location().getPath()).withStyle(ChatFormatting.AQUA));
        } else if (NetworkLink.AVAILABLE) {
            tooltip.add(Component.translatable("tooltip.mm.structure_builder.no_network").withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.translatable("tooltip.mm.structure_builder.usage", dismantleKeyName.get()).withStyle(ChatFormatting.DARK_GRAY));
    }
}

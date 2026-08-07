package io.ticticboom.mods.mm.setup;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.debug.tool.DebugToolItem;
import io.ticticboom.mods.mm.item.BlueprintItem;
import io.ticticboom.mods.mm.item.MultiblockSaverItem;
import io.ticticboom.mods.mm.item.PrioritySetterItem;
import io.ticticboom.mods.mm.structure.StructureManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("unused")
public class MMRegisters {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, Ref.ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, Ref.ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Ref.ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU, Ref.ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Ref.ID);

    public static final DeferredHolder<Item, BlueprintItem> BLUEPRINT = ITEMS.register("blueprint", BlueprintItem::new);
    public static final DeferredHolder<Item, Item> DEBUG_TOOL = ITEMS.register("debug_tool", DebugToolItem::new);
    public static final DeferredHolder<Item, Item> PRIORITY_SETTER = ITEMS.register("priority_setter", PrioritySetterItem::new);
    public static final DeferredHolder<Item, MultiblockSaverItem> MULTIBLOCK_SAVER = ITEMS.register("multiblock_saver", MultiblockSaverItem::new);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MM_TAB = TABS.register("mm", () -> CreativeModeTab.builder().title(Component.translatable("tab.mm.main"))
            .icon(() -> BLUEPRINT.get().getDefaultInstance())
            .backgroundTexture(Ref.UiTextures.CREATIVE_TAB_BG)
            .withSearchBar(55)
            .displayItems((p, o) -> o.acceptAll(ITEMS.getEntries().stream().map(x -> x.get().getDefaultInstance()).toList()))
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MM_STRUCTURE_TAB = TABS.register("mm_structures", () -> CreativeModeTab.builder().title(Component.translatable("tab.mm.structures"))
            .icon(() -> BLUEPRINT.get().getDefaultInstance())
            .backgroundTexture(Ref.UiTextures.CREATIVE_TAB_BG)
            .withSearchBar(55)
            .displayItems((p, o) -> o.acceptAll(StructureManager.STRUCTURE_BLUEPRINTS.values()))
            .build());

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        TABS.register(bus);
    }
}

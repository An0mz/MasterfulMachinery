package io.ticticboom.mods.mm.port.astral.lumen;

import io.ticticboom.mods.mm.compat.kjs.builder.PortConfigBuilderJS;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.port.PortType;
import io.ticticboom.mods.mm.port.astral.lumen.compat.AstralLumenConfigBuilderJS;
import io.ticticboom.mods.mm.port.astral.lumen.register.AstralLumenPortBlock;
import io.ticticboom.mods.mm.port.astral.lumen.register.AstralLumenPortBlockEntity;
import io.ticticboom.mods.mm.port.astral.lumen.register.AstralLumenPortBlockItem;
import io.ticticboom.mods.mm.port.astral.lumen.register.AstralLumenPortMenu;
import io.ticticboom.mods.mm.port.astral.lumen.register.AstralLumenPortScreen;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Consumer;

public class AstralLumenPortType extends PortType {

    @Override
    public IPortParser getParser() {
        return new AstralLumenPortParser();
    }

    @Override
    public DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> registerBlockEntity(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.BLOCK_ENTITIES.register(model.id(), () -> BlockEntityType.Builder.of((p, s) -> new AstralLumenPortBlockEntity(model, groupHolder, p, s), groupHolder.getBlock().get()).build(null));
    }

    @Override
    public DeferredHolder<Block, Block> registerBlock(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.BLOCKS.register(model.id(), () -> new AstralLumenPortBlock(model, groupHolder));
    }

    @Override
    public DeferredHolder<Item, Item> registerItem(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.ITEMS.register(model.id(), () -> new AstralLumenPortBlockItem(model, groupHolder));
    }

    @Override
    public DeferredHolder<MenuType<?>, MenuType<?>> registerMenu(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.MENUS.register(model.id(), () -> IMenuTypeExtension.create((i, o, u) -> new AstralLumenPortMenu(model, groupHolder, i, o, u)));
    }

    @Override
    public void registerScreen(RegistryGroupHolder groupHolder) {
        MenuScreens.register((MenuType<AstralLumenPortMenu>) groupHolder.getMenu().get(), AstralLumenPortScreen::new);
    }

    @Override
    public IPortStorageFactory createStorageFactory(Consumer<PortConfigBuilderJS> consumer) {
        var builder = new AstralLumenConfigBuilderJS();
        consumer.accept(builder);
        return new AstralLumenPortStorageFactory((AstralLumenPortStorageModel) builder.build());
    }
}

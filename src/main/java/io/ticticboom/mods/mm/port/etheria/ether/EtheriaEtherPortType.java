package io.ticticboom.mods.mm.port.etheria.ether;

import io.ticticboom.mods.mm.compat.kjs.builder.PortConfigBuilderJS;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.port.PortType;
import io.ticticboom.mods.mm.port.etheria.ether.compat.EtheriaEtherConfigBuilderJS;
import io.ticticboom.mods.mm.port.etheria.ether.register.EtheriaEtherPortBlock;
import io.ticticboom.mods.mm.port.etheria.ether.register.EtheriaEtherPortBlockEntity;
import io.ticticboom.mods.mm.port.etheria.ether.register.EtheriaEtherPortBlockItem;
import io.ticticboom.mods.mm.port.etheria.ether.register.EtheriaEtherPortMenu;
import io.ticticboom.mods.mm.port.etheria.ether.register.EtheriaEtherPortScreen;
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

public class EtheriaEtherPortType extends PortType {

    @Override
    public IPortParser getParser() {
        return new EtheriaEtherPortParser();
    }

    @Override
    public DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> registerBlockEntity(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.BLOCK_ENTITIES.register(model.id(), () -> BlockEntityType.Builder.of((p, s) -> new EtheriaEtherPortBlockEntity(model, groupHolder, p, s), groupHolder.getBlock().get()).build(null));
    }

    @Override
    public DeferredHolder<Block, Block> registerBlock(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.BLOCKS.register(model.id(), () -> new EtheriaEtherPortBlock(model, groupHolder));
    }

    @Override
    public DeferredHolder<Item, Item> registerItem(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.ITEMS.register(model.id(), () -> new EtheriaEtherPortBlockItem(model, groupHolder));
    }

    @Override
    public DeferredHolder<MenuType<?>, MenuType<?>> registerMenu(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.MENUS.register(model.id(), () -> IMenuTypeExtension.create((i, o, u) -> new EtheriaEtherPortMenu(model, groupHolder, i, o, u)));
    }

    @Override
    public void registerScreen(RegistryGroupHolder groupHolder) {
        MenuScreens.register((MenuType<EtheriaEtherPortMenu>) groupHolder.getMenu().get(), EtheriaEtherPortScreen::new);
    }

    @Override
    public IPortStorageFactory createStorageFactory(Consumer<PortConfigBuilderJS> consumer) {
        var builder = new EtheriaEtherConfigBuilderJS();
        consumer.accept(builder);
        return new EtheriaEtherPortStorageFactory((EtheriaEtherPortStorageModel) builder.build());
    }
}
